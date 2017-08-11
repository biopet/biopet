package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.File
import java.net.URLClassLoader

import htsjdk.variant.variantcontext.{Genotype, VariantContext}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils._
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 14/07/2017.
  */
object VcfStats extends ToolCommand {

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
      .set("spark.executor.memory", cmdArgs.sparkExecutorMemory.getOrElse(""))
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

    val regionStats = sc
      .parallelize(regions, regions.size)
      .map(readBin(_, samples, cmdArgs, adInfoTags, adGenotypeTags))
      .cache()

    val totalStats = Future(regionStats.aggregate(Stats.emptyStats(samples))(_ += _._1, _ += _))
      .map(
        _.writeAllOutput(cmdArgs.outputDir,
                         samples,
                         adGenotypeTags,
                         adInfoTags,
                         sampleDistributions,
                         None))

    regionStats
      .flatMap(_._2)
      .aggregateByKey(Stats.emptyStats(samples))(_ += _, _ += _)
      .foreach {
        case (k, v) =>
          v.writeAllOutput(new File(cmdArgs.outputDir, "contigs" + File.separator + k),
                           samples,
                           adGenotypeTags,
                           adInfoTags,
                           sampleDistributions,
                           Some(k))
      }
    regionStats.unpersist()

    Await.result(totalStats, Duration.Inf)

    val completeStatsJson = regions
      .flatMap(_.map(_.chr))
      .foldLeft(ConfigUtils.fileToConfigMap(new File(cmdArgs.outputDir, "total.json"))) {
        case (map, contig) =>
          val contigMap = ConfigUtils.fileToConfigMap(
            new File(cmdArgs.outputDir,
                     "contigs" + File.separator + contig + File.separator + s"$contig.json"))
          ConfigUtils.mergeMaps(map, contigMap)
      }

    IoUtils.writeLinesToFile(new File(cmdArgs.outputDir, "stats.json"),
                             ConfigUtils.mapToJson(completeStatsJson).nospaces :: Nil)

    sc.stop
    logger.info("Done")
  }

  def readBin(bedRecords: List[BedRecord],
              samples: List[String],
              cmdArgs: Args,
              adInfoTags: List[String],
              adGenotypeTags: List[String]): (Stats, List[(String, Stats)]) = {
    val reader = new VCFFileReader(cmdArgs.inputFile, true)
    val totalStats = Stats.emptyStats(samples)
    val dict = FastaUtils.getDictFromFasta(cmdArgs.referenceFile)

    val nonCompleteContigs = for (bedRecord <- bedRecords) yield {
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
      }
      totalStats += stats
      if (bedRecord.length == dict.getSequence(bedRecord.chr).getSequenceLength) {
        Future {
          stats.writeAllOutput(
            new File(cmdArgs.outputDir, "contigs" + File.separator + bedRecord.chr),
            samples,
            adGenotypeTags,
            adInfoTags,
            sampleDistributions,
            Some(bedRecord.chr))
          None
        }
      } else Future.successful(Some(bedRecord.chr -> stats))
    }
    reader.close()

    (totalStats, Await.result(Future.sequence(nonCompleteContigs), Duration.Inf).flatten)
  }

  val defaultGenotypeFields =
    List("DP", "GQ", "AD", "AD-ref", "AD-alt", "AD-used", "AD-not_used", "general")

  val defaultInfoFields = List("QUAL", "general", "AC", "AF", "AN", "DP")

  val sampleDistributions = List("Het",
                                 "HetNonRef",
                                 "Hom",
                                 "HomRef",
                                 "HomVar",
                                 "Mixed",
                                 "NoCall",
                                 "NonInformative",
                                 "Available",
                                 "Called",
                                 "Filtered",
                                 "Variant")

  /** Function to check sample/genotype specific stats */
  protected[tools] def checkGenotype(record: VariantContext,
                                     genotype: Genotype,
                                     additionalTags: List[String]): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    buffer += "DP" -> Map((if (genotype.hasDP) genotype.getDP else "not set") -> 1)
    buffer += "GQ" -> Map((if (genotype.hasGQ) genotype.getGQ else "not set") -> 1)

    val usedAlleles =
      (for (allele <- genotype.getAlleles) yield record.getAlleleIndex(allele)).toList

    addToBuffer("general", "Total", found = true)
    addToBuffer("general", "Het", genotype.isHet)
    addToBuffer("general", "HetNonRef", genotype.isHetNonRef)
    addToBuffer("general", "Hom", genotype.isHom)
    addToBuffer("general", "HomRef", genotype.isHomRef)
    addToBuffer("general", "HomVar", genotype.isHomVar)
    addToBuffer("general", "Mixed", genotype.isMixed)
    addToBuffer("general", "NoCall", genotype.isNoCall)
    addToBuffer("general", "NonInformative", genotype.isNonInformative)
    addToBuffer("general", "Available", genotype.isAvailable)
    addToBuffer("general", "Called", genotype.isCalled)
    addToBuffer("general", "Filtered", genotype.isFiltered)
    addToBuffer("general", "Variant", genotype.isHetNonRef || genotype.isHet || genotype.isHomVar)

    if (genotype.hasAD) {
      val ad = genotype.getAD
      for (i <- 0 until ad.size if ad(i) > 0) {
        addToBuffer("AD", ad(i), found = true)
        if (i == 0) addToBuffer("AD-ref", ad(i), found = true)
        if (i > 0) addToBuffer("AD-alt", ad(i), found = true)
        if (usedAlleles.contains(i)) addToBuffer("AD-used", ad(i), found = true)
        else addToBuffer("AD-not_used", ad(i), found = true)
      }
    }

    val skipTags = List("DP", "GQ", "AD", "AD-ref", "AD-alt", "AD-used", "AD-not_used", "general")

    for (tag <- additionalTags if !skipTags.contains(tag)) {
      val value = genotype.getAnyAttribute(tag)
      if (value == null) addToBuffer(tag, "notset", found = true)
      else addToBuffer(tag, value, found = true)
    }

    buffer.toMap
  }

  /** Function to check all general stats, all info expect sample/genotype specific stats */
  protected[tools] def checkGeneral(record: VariantContext,
                                    additionalTags: List[String]): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    addToBuffer("QUAL", Math.round(record.getPhredScaledQual), found = true)

    addToBuffer("SampleDistribution-Het",
                record.getGenotypes.count(genotype => genotype.isHet),
                found = true)
    addToBuffer("SampleDistribution-HetNonRef",
                record.getGenotypes.count(genotype => genotype.isHetNonRef),
                found = true)
    addToBuffer("SampleDistribution-Hom",
                record.getGenotypes.count(genotype => genotype.isHom),
                found = true)
    addToBuffer("SampleDistribution-HomRef",
                record.getGenotypes.count(genotype => genotype.isHomRef),
                found = true)
    addToBuffer("SampleDistribution-HomVar",
                record.getGenotypes.count(genotype => genotype.isHomVar),
                found = true)
    addToBuffer("SampleDistribution-Mixed",
                record.getGenotypes.count(genotype => genotype.isMixed),
                found = true)
    addToBuffer("SampleDistribution-NoCall",
                record.getGenotypes.count(genotype => genotype.isNoCall),
                found = true)
    addToBuffer("SampleDistribution-NonInformative",
                record.getGenotypes.count(genotype => genotype.isNonInformative),
                found = true)
    addToBuffer("SampleDistribution-Available",
                record.getGenotypes.count(genotype => genotype.isAvailable),
                found = true)
    addToBuffer("SampleDistribution-Called",
                record.getGenotypes.count(genotype => genotype.isCalled),
                found = true)
    addToBuffer("SampleDistribution-Filtered",
                record.getGenotypes.count(genotype => genotype.isFiltered),
                found = true)
    addToBuffer("SampleDistribution-Variant",
                record.getGenotypes.count(genotype =>
                  genotype.isHetNonRef || genotype.isHet || genotype.isHomVar),
                found = true)

    addToBuffer("general", "Total", found = true)
    addToBuffer("general", "Biallelic", record.isBiallelic)
    addToBuffer("general", "ComplexIndel", record.isComplexIndel)
    addToBuffer("general", "Filtered", record.isFiltered)
    addToBuffer("general", "FullyDecoded", record.isFullyDecoded)
    addToBuffer("general", "Indel", record.isIndel)
    addToBuffer("general", "Mixed", record.isMixed)
    addToBuffer("general", "MNP", record.isMNP)
    addToBuffer("general", "MonomorphicInSamples", record.isMonomorphicInSamples)
    addToBuffer("general", "NotFiltered", record.isNotFiltered)
    addToBuffer("general", "PointEvent", record.isPointEvent)
    addToBuffer("general", "PolymorphicInSamples", record.isPolymorphicInSamples)
    addToBuffer("general", "SimpleDeletion", record.isSimpleDeletion)
    addToBuffer("general", "SimpleInsertion", record.isSimpleInsertion)
    addToBuffer("general", "SNP", record.isSNP)
    addToBuffer("general", "StructuralIndel", record.isStructuralIndel)
    addToBuffer("general", "Symbolic", record.isSymbolic)
    addToBuffer("general", "SymbolicOrSV", record.isSymbolicOrSV)
    addToBuffer("general", "Variant", record.isVariant)

    val skipTags = List("QUAL", "general")

    for (tag <- additionalTags if !skipTags.contains(tag)) {
      val value = record.getAttribute(tag)
      if (value == null) addToBuffer(tag, "notset", found = true)
      else addToBuffer(tag, value, found = true)
    }

    buffer.toMap
  }

  protected[tools] def fillGeneral(additionalTags: List[String]): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    addToBuffer("QUAL", "not set", found = false)

    addToBuffer("SampleDistribution-Het", "not set", found = false)
    addToBuffer("SampleDistribution-HetNonRef", "not set", found = false)
    addToBuffer("SampleDistribution-Hom", "not set", found = false)
    addToBuffer("SampleDistribution-HomRef", "not set", found = false)
    addToBuffer("SampleDistribution-HomVar", "not set", found = false)
    addToBuffer("SampleDistribution-Mixed", "not set", found = false)
    addToBuffer("SampleDistribution-NoCall", "not set", found = false)
    addToBuffer("SampleDistribution-NonInformative", "not set", found = false)
    addToBuffer("SampleDistribution-Available", "not set", found = false)
    addToBuffer("SampleDistribution-Called", "not set", found = false)
    addToBuffer("SampleDistribution-Filtered", "not set", found = false)
    addToBuffer("SampleDistribution-Variant", "not set", found = false)

    addToBuffer("general", "Total", found = false)
    addToBuffer("general", "Biallelic", found = false)
    addToBuffer("general", "ComplexIndel", found = false)
    addToBuffer("general", "Filtered", found = false)
    addToBuffer("general", "FullyDecoded", found = false)
    addToBuffer("general", "Indel", found = false)
    addToBuffer("general", "Mixed", found = false)
    addToBuffer("general", "MNP", found = false)
    addToBuffer("general", "MonomorphicInSamples", found = false)
    addToBuffer("general", "NotFiltered", found = false)
    addToBuffer("general", "PointEvent", found = false)
    addToBuffer("general", "PolymorphicInSamples", found = false)
    addToBuffer("general", "SimpleDeletion", found = false)
    addToBuffer("general", "SimpleInsertion", found = false)
    addToBuffer("general", "SNP", found = false)
    addToBuffer("general", "StructuralIndel", found = false)
    addToBuffer("general", "Symbolic", found = false)
    addToBuffer("general", "SymbolicOrSV", found = false)
    addToBuffer("general", "Variant", found = false)

    val skipTags = List("QUAL", "general")

    for (tag <- additionalTags if !skipTags.contains(tag)) {
      addToBuffer(tag, "not set", found = false)
    }

    buffer.toMap
  }

  protected[tools] def fillGenotype(additionalTags: List[String]): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    addToBuffer("DP", "not set", found = false)
    addToBuffer("GQ", "not set", found = false)

    addToBuffer("general", "Total", found = false)
    addToBuffer("general", "Het", found = false)
    addToBuffer("general", "HetNonRef", found = false)
    addToBuffer("general", "Hom", found = false)
    addToBuffer("general", "HomRef", found = false)
    addToBuffer("general", "HomVar", found = false)
    addToBuffer("general", "Mixed", found = false)
    addToBuffer("general", "NoCall", found = false)
    addToBuffer("general", "NonInformative", found = false)
    addToBuffer("general", "Available", found = false)
    addToBuffer("general", "Called", found = false)
    addToBuffer("general", "Filtered", found = false)
    addToBuffer("general", "Variant", found = false)

    val skipTags = List("DP", "GQ", "AD", "AD-ref", "AD-alt", "AD-used", "AD-not_used", "general")

    for (tag <- additionalTags if !skipTags.contains(tag)) {
      addToBuffer(tag, 0, found = false)
    }

    buffer.toMap
  }
}
