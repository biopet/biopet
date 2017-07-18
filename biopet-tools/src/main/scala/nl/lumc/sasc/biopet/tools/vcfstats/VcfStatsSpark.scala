package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.{File, PrintWriter}
import java.net.URLClassLoader

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats._
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand, VcfUtils}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 14/07/2017.
  */
object VcfStatsSpark extends ToolCommand {

  type Args = VcfStatsArgs

  def main(args: Array[String]): Unit = {

    logger.info("Started")
    val argsParser = new VcfStatsOptParser(commandName)
    val cmdArgs = argsParser.parse(args, VcfStatsArgs()) getOrElse (throw new IllegalArgumentException)

    val reader = new VCFFileReader(cmdArgs.inputFile, true)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder.toList

    reader.close()

    val adInfoTags = {
      (for (infoTag <- cmdArgs.infoTags if !defaultInfoFields.contains(infoTag)) yield {
        require(header.getInfoHeaderLine(infoTag) != null,
                "Info tag '" + infoTag + "' not found in header of vcf file")
        infoTag
      }) ::: (for (line <- header.getInfoHeaderLines if cmdArgs.allInfoTags
                   if !defaultInfoFields.contains(line.getID)
                   if !cmdArgs.infoTags.contains(line.getID)) yield {
        line.getID
      }).toList ::: defaultInfoFields
    }

    val adGenotypeTags = (for (genotypeTag <- cmdArgs.genotypeTags
                               if !defaultGenotypeFields.contains(genotypeTag)) yield {
      require(header.getFormatHeaderLine(genotypeTag) != null,
              "Format tag '" + genotypeTag + "' not found in header of vcf file")
      genotypeTag
    }) ::: (for (line <- header.getFormatHeaderLines if cmdArgs.allGenotypeTags
                 if !defaultGenotypeFields.contains(line.getID)
                 if !cmdArgs.genotypeTags.contains(line.getID)
                 if line.getID != "PL") yield {
      line.getID
    }).toList ::: defaultGenotypeFields

    logger.info("Init spark context")

    val jars = ClassLoader.getSystemClassLoader
      .asInstanceOf[URLClassLoader]
      .getURLs
      .map(_.getFile)
    val conf = new SparkConf()
      .setAppName(commandName)
      .setMaster(cmdArgs.sparkMaster.getOrElse(s"local[${cmdArgs.localThreads}]"))
      .setJars(jars)
    val sc = new SparkContext(conf)
    logger.info("Spark context is up")

    val regions = (cmdArgs.intervals match {
      case Some(i) =>
        BedRecordList.fromFile(i).validateContigs(cmdArgs.referenceFile)
      case _ => BedRecordList.fromReference(cmdArgs.referenceFile)
    }).combineOverlap
      .scatter(cmdArgs.binSize, maxContigsInSingleJob = Some(cmdArgs.maxContigsInSingleJob))
    val contigs = regions.flatMap(_.map(_.chr)).distinct

    val regionStats = sc
      .parallelize(regions, regions.size)
      .map(readBin(_, samples, cmdArgs, adInfoTags, adGenotypeTags))

    val totalStats = regionStats.reduce(_ += _)

    val allWriter = new PrintWriter(new File(cmdArgs.outputDir, "stats.json"))
    val json = ConfigUtils.mapToJson(
      totalStats.getAllStats(contigs, samples, adGenotypeTags, adInfoTags, sampleDistributions))
    allWriter.println(json.nospaces)
    allWriter.close()

    //TODO: write wig files

    writeOverlap(totalStats,
                 _.genotypeOverlap,
                 cmdArgs.outputDir + "/sample_compare/genotype_overlap",
                 samples)
    writeOverlap(totalStats,
                 _.alleleOverlap,
                 cmdArgs.outputDir + "/sample_compare/allele_overlap",
                 samples)

    sc.stop
    logger.info("Done")
  }

  def readBin(bedRecords: List[BedRecord],
              samples: List[String],
              cmdArgs: Args,
              adInfoTags: List[String],
              adGenotypeTags: List[String]): Stats = {
    val reader = new VCFFileReader(cmdArgs.inputFile, true)
    var chunkCounter = 0
    val stats = Stats.emptyStats(samples)

    for (bedRecord <- bedRecords) {
      logger.info("Starting on: " + bedRecord)

      val samInterval = bedRecord.toSamInterval

      val query =
        reader.query(samInterval.getContig, samInterval.getStart, samInterval.getEnd)
      if (!query.hasNext) {
        Stats.mergeNestedStatsMap(stats.generalStats, fillGeneral(adInfoTags))
        for (sample <- samples) yield {
          Stats.mergeNestedStatsMap(stats.samplesStats(sample).genotypeStats,
                                    fillGenotype(adGenotypeTags))
        }
        chunkCounter += 1
      }

      for (record <- query if record.getStart <= samInterval.getEnd) {
        Stats.mergeNestedStatsMap(stats.generalStats, checkGeneral(record, adInfoTags))
        for (sample1 <- samples) yield {
          val genotype = record.getGenotype(sample1)
          Stats.mergeNestedStatsMap(stats.samplesStats(sample1).genotypeStats,
                                    checkGenotype(record, genotype, adGenotypeTags))
          for (sample2 <- samples) {
            val genotype2 = record.getGenotype(sample2)
            if (genotype.getAlleles == genotype2.getAlleles)
              stats.samplesStats(sample1).sampleToSample(sample2).genotypeOverlap += 1
            stats.samplesStats(sample1).sampleToSample(sample2).alleleOverlap += VcfUtils
              .alleleOverlap(genotype.getAlleles.toList, genotype2.getAlleles.toList)
          }
        }
        chunkCounter += 1
      }
    }
    reader.close()

    stats
  }
}
