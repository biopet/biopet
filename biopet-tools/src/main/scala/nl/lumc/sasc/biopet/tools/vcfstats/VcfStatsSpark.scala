package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.{File, PrintWriter}
import java.net.URLClassLoader

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats._
import nl.lumc.sasc.biopet.utils.{ConfigUtils, FastaUtils, ToolCommand, VcfUtils}
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by pjvanthof on 14/07/2017.
  */
object VcfStatsSpark extends ToolCommand {

  type Args = VcfStatsArgs

  /** Parsing commandline arguments */
  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(inputFile = x.getAbsoluteFile)
    } validate { x =>
      if (x.exists) success else failure("Input VCF required")
    } text "Input VCF file (required)"
    opt[File]('R', "referenceFile") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(referenceFile = x)
    } validate { x =>
      if (x.exists) success else failure("Reference file required")
    } text "Fasta reference which was used to call input VCF (required)"
    opt[File]('o', "outputDir") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(outputDir = x)
    } validate { x =>
      if (x == null) failure("Valid output directory required")
      else if (x.exists) success
      else failure(s"Output directory does not exist: $x")
    } text "Path to directory for output (required)"
    opt[File]('i', "intervals") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(intervals = Some(x))
    } text "Path to interval (BED) file (optional)"
    opt[String]("infoTag") unbounded () valueName "<tag>" action { (x, c) =>
      c.copy(infoTags = x :: c.infoTags)
    } text s"Summarize these info tags. Default is (${defaultInfoFields.mkString(", ")})"
    opt[String]("genotypeTag") unbounded () valueName "<tag>" action { (x, c) =>
      c.copy(genotypeTags = x :: c.genotypeTags)
    } text s"Summarize these genotype tags. Default is (${defaultGenotypeFields.mkString(", ")})"
    opt[Unit]("allInfoTags") unbounded () action { (_, c) =>
      c.copy(allInfoTags = true)
    } text "Summarize all info tags. Default false"
    opt[Unit]("allGenotypeTags") unbounded () action { (_, c) =>
      c.copy(allGenotypeTags = true)
    } text "Summarize all genotype tags. Default false"
    opt[Int]("binSize") unbounded () action { (x, c) =>
      c.copy(binSize = x)
    } text "Binsize in estimated base pairs"
    opt[Unit]("writeBinStats") unbounded () action { (_, c) =>
      c.copy(writeBinStats = true)
    } text "Write bin statistics. Default False"
    opt[String]("generalWiggle") unbounded () action { (x, c) =>
      c.copy(generalWiggle = x :: c.generalWiggle, writeBinStats = true)
    } validate { x =>
      if (generalWiggleOptions.contains(x)) success else failure(s"""Nonexistent field $x""")
    } text s"""Create a wiggle track with bin size <binSize> for any of the following statistics:
              |${generalWiggleOptions.mkString(", ")}""".stripMargin
    opt[String]("genotypeWiggle") unbounded () action { (x, c) =>
      c.copy(genotypeWiggle = x :: c.genotypeWiggle, writeBinStats = true)
    } validate { x =>
      if (genotypeWiggleOptions.contains(x)) success else failure(s"""Non-existent field $x""")
    } text s"""Create a wiggle track with bin size <binSize> for any of the following genotype fields:
              |${genotypeWiggleOptions.mkString(", ")}""".stripMargin
    opt[Int]('t', "localThreads") unbounded () action { (x, c) =>
      c.copy(localThreads = x)
    } text s"Number of local threads to use"
    opt[String]("sparkMaster") unbounded () action { (x, c) =>
      c.copy(sparkMaster = Some(x))
    } text s"Spark master to use"
  }

  def main(args: Array[String]): Unit = {

    logger.info("Started")
    val argsParser = new OptParser
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
      .map(_.getFile) ++ List(
      "/home/pjvan_thof/src/biopet/biopet-utils/target/BiopetUtils-0.10.0-SNAPSHOT.jar",
      "/home/pjvan_thof/src/biopet/biopet-tools/target/BiopetTools-0.10.0-SNAPSHOT.jar"
    )
    val conf = new SparkConf()
      .setAppName(this.getClass.getSimpleName)
      .setMaster(cmdArgs.sparkMaster.getOrElse(s"local[${cmdArgs.localThreads}]"))
      .setJars(jars)
    val sc = new SparkContext(conf)
    logger.info("Spark context is up")

    val regions = (cmdArgs.intervals match {
      case Some(i) =>
        BedRecordList.fromFile(i).validateContigs(cmdArgs.referenceFile)
      case _ => BedRecordList.fromReference(cmdArgs.referenceFile)
    }).combineOverlap
      .scatter(cmdArgs.binSize)
      .flatten
    val contigs = regions.map(_.chr).distinct

    val regionStats = sc.parallelize(regions, regions.size).map { record =>
      record.chr -> (readBin(record, samples, cmdArgs, adInfoTags, adGenotypeTags), record)
    }

    val chrStats = regionStats.combineByKey(
      createCombiner = (x: (Stats, BedRecord)) => x._1,
      mergeValue = (x: Stats, b: (Stats, BedRecord)) => x += b._1,
      mergeCombiners = (x: Stats, y: Stats) => x += y,
      partitioner = new HashPartitioner(contigs.size),
      mapSideCombine = true
    )

    val totalStats = chrStats.aggregate(Stats.emptyStats(samples))(_ += _._2, _ += _)

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

  def readBin(bedRecord: BedRecord,
              samples: List[String],
              cmdArgs: Args,
              adInfoTags: List[String],
              adGenotypeTags: List[String]): Stats = {
    val reader = new VCFFileReader(cmdArgs.inputFile, true)
    var chunkCounter = 0
    val stats = Stats.emptyStats(samples)
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
    reader.close()

    stats
  }
}

/** Commandline argument */
case class VcfStatsArgs(inputFile: File = null,
                        outputDir: File = null,
                        referenceFile: File = null,
                        intervals: Option[File] = None,
                        infoTags: List[String] = Nil,
                        genotypeTags: List[String] = Nil,
                        allInfoTags: Boolean = false,
                        allGenotypeTags: Boolean = false,
                        binSize: Int = 10000000,
                        writeBinStats: Boolean = false,
                        generalWiggle: List[String] = Nil,
                        genotypeWiggle: List[String] = Nil,
                        localThreads: Int = 1,
                        sparkMaster: Option[String] = None,
                        contigSampleOverlapPlots: Boolean = false)
