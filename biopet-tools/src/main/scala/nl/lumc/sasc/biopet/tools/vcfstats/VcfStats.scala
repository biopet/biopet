/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.{File, FileOutputStream, IOException, PrintWriter}

import htsjdk.samtools.util.Interval
import htsjdk.variant.variantcontext.{Genotype, VariantContext}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import nl.lumc.sasc.biopet.utils.{ConfigUtils, FastaUtils, ToolCommand, VcfUtils}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

/**
  * This tool will generate statistics from a vcf file
  *
  * Created by pjvan_thof on 1/10/15.
  */
object VcfStats extends ToolCommand {

  /** Commandline argument */
  case class Args(inputFile: File = null,
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
                  sparkMaster: Option[String] = None)
      extends AbstractArgs

  val generalWiggleOptions = List(
    "Total",
    "Biallelic",
    "ComplexIndel",
    "Filtered",
    "FullyDecoded",
    "Indel",
    "Mixed",
    "MNP",
    "MonomorphicInSamples",
    "NotFiltered",
    "PointEvent",
    "PolymorphicInSamples",
    "SimpleDeletion",
    "SimpleInsertion",
    "SNP",
    "StructuralIndel",
    "Symbolic",
    "SymbolicOrSV",
    "Variant"
  )

  val genotypeWiggleOptions = List("Total",
                                   "Het",
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

  /** Parsing commandline arguments */
  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(inputFile = x)
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

  //protected var cmdArgs: Args = _

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

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    logger.info("Started")
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Init spark context")

    val conf = new SparkConf()
      .setAppName(this.getClass.getSimpleName)
      .setMaster(cmdArgs.sparkMaster.getOrElse(s"local[${cmdArgs.localThreads}]"))
    val sparkContext = new SparkContext(conf)

    logger.info("Spark context is up")

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

    val bedRecords = (cmdArgs.intervals match {
      case Some(i) =>
        BedRecordList.fromFile(i).validateContigs(cmdArgs.referenceFile)
      case _ => BedRecordList.fromReference(cmdArgs.referenceFile)
    }).combineOverlap.scatter(cmdArgs.binSize)

    val intervals: List[Interval] =
      BedRecordList.fromList(bedRecords.flatten).toSamIntervals.toList

    val totalBases = bedRecords.flatten.map(_.length.toLong).sum

    // Reading vcf records
    logger.info("Start reading vcf records")

    var variantCounter = 0L
    var baseCounter = 0L

    def status(count: Int, interval: Interval): Unit = {
      variantCounter += count
      baseCounter += interval.length()
      val fraction = baseCounter.toFloat / totalBases * 100
      logger.info(interval + " done, " + count + " rows processed")
      logger.info("total: " + variantCounter + " rows processed, " + fraction + "% done")
    }

    // Triple for loop to not keep all bins in memory
    val statsFutures = for (intervals <- Random
                              .shuffle(bedRecords))
      yield
        Future {
          val chunkStats = for (intervals <- intervals.grouped(25)) yield {
            val binStats = for (interval <- intervals.par) yield {
              val reader = new VCFFileReader(cmdArgs.inputFile, true)
              var chunkCounter = 0
              val stats = Stats.emptyStats(samples)
              logger.info("Starting on: " + interval)

              val samInterval = interval.toSamInterval

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

              if (cmdArgs.writeBinStats) {
                val binOutputDir =
                  new File(cmdArgs.outputDir, "bins" + File.separator + samInterval.getContig)

                stats.writeGenotypeField(
                  samples,
                  "general",
                  binOutputDir,
                  prefix = "genotype-" + samInterval.getStart + "-" + samInterval.getEnd)
                stats.writeField("general",
                                 binOutputDir,
                                 prefix = samInterval.getStart + "-" + samInterval.getEnd)
              }

              status(chunkCounter, samInterval)
              stats
            }
            binStats.toList.fold(Stats.emptyStats(samples))(_ += _)
          }
          chunkStats.toList.fold(Stats.emptyStats(samples))(_ += _)
        }
    val stats = statsFutures.foldLeft(Stats.emptyStats(samples)) {
      case (a, b) => a += Await.result(b, Duration.Inf)
    }

    logger.info("Done reading vcf records")

    val allWriter = new PrintWriter(new File(cmdArgs.outputDir, "stats.json"))
    val json = ConfigUtils.mapToJson(
      stats.getAllStats(
        FastaUtils.getCachedDict(cmdArgs.referenceFile).getSequences.map(_.getSequenceName).toList,
        samples,
        adGenotypeTags,
        adInfoTags,
        sampleDistributions))
    allWriter.println(json.nospaces)
    allWriter.close()

    // Write general wiggle tracks
    for (field <- cmdArgs.generalWiggle) {
      val file = new File(cmdArgs.outputDir, "wigs" + File.separator + "general-" + field + ".wig")
      writeWiggle(intervals, field, "count", file, genotype = false, cmdArgs.outputDir)
    }

    // Write sample wiggle tracks
    for (field <- cmdArgs.genotypeWiggle; sample <- samples) {
      val file = new File(cmdArgs.outputDir,
                          "wigs" + File.separator + "genotype-" + sample + "-" + field + ".wig")
      writeWiggle(intervals, field, sample, file, genotype = true, cmdArgs.outputDir)
    }

    writeOverlap(stats,
                 _.genotypeOverlap,
                 cmdArgs.outputDir + "/sample_compare/genotype_overlap",
                 samples)
    writeOverlap(stats,
                 _.alleleOverlap,
                 cmdArgs.outputDir + "/sample_compare/allele_overlap",
                 samples)

    sparkContext.stop()
    logger.info("Done")
  }

  //FIXME: does only work correct for reference and not with a bed file
  protected def writeWiggle(intervals: List[Interval],
                            row: String,
                            column: String,
                            outputFile: File,
                            genotype: Boolean,
                            outputDir: File): Unit = {
    val groupedIntervals =
      intervals.groupBy(_.getContig).map { case (k, v) => k -> v.sortBy(_.getStart) }
    outputFile.getParentFile.mkdirs()
    val writer = new PrintWriter(outputFile)
    writer.println("track type=wiggle_0")
    for ((chr, intervals) <- groupedIntervals) yield {
      val length = intervals.head.length()
      writer.println(s"fixedStep chrom=$chr start=1 step=$length span=$length")
      for (interval <- intervals) {
        val file = {
          if (genotype)
            new File(
              outputDir,
              "bins" + File.separator + chr + File.separator + "genotype-" + interval.getStart + "-" + interval.getEnd + "-general.tsv")
          else
            new File(
              outputDir,
              "bins" + File.separator + chr + File.separator + interval.getStart + "-" + interval.getEnd + "-general.tsv")
        }
        writer.println(valueFromTsv(file, row, column).getOrElse(0))
      }
    }
    writer.close()
  }

  /**
    * Gets single value from a tsv file
    * @param file Input tsv file
    * @param row Row id
    * @param column column id
    * @return value
    */
  def valueFromTsv(file: File, row: String, column: String): Option[String] = {
    val reader = Source.fromFile(file)
    val it = reader.getLines()
    val index = it.next().split("\t").indexOf(column)

    val value = it.find(_.startsWith(row))
    reader.close()

    value.collect { case x => x.split("\t")(index) }
  }

  protected[tools] def fillGeneral(
      additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
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

    Map("total" -> buffer.toMap)
  }

  /** Function to check all general stats, all info expect sample/genotype specific stats */
  protected[tools] def checkGeneral(
      record: VariantContext,
      additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
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

    Map(record.getContig -> buffer.toMap, "total" -> buffer.toMap)
  }

  protected[tools] def fillGenotype(
      additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
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

    Map("total" -> buffer.toMap)

  }

  /** Function to check sample/genotype specific stats */
  protected[tools] def checkGenotype(
      record: VariantContext,
      genotype: Genotype,
      additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
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

    Map(record.getContig -> buffer.toMap, "total" -> buffer.toMap)
  }

  /** Function to write sample to sample compare tsv's / heatmaps */
  def writeOverlap(stats: Stats,
                   function: SampleToSampleStats => Int,
                   prefix: String,
                   samples: List[String],
                   plots: Boolean = true): Unit = {
    val absFile = new File(prefix + ".abs.tsv")
    val relFile = new File(prefix + ".rel.tsv")

    absFile.getParentFile.mkdirs()

    val absWriter = new PrintWriter(absFile)
    val relWriter = new PrintWriter(relFile)

    absWriter.println(samples.mkString("\t", "\t", ""))
    relWriter.println(samples.mkString("\t", "\t", ""))
    for (sample1 <- samples) {
      val values = for (sample2 <- samples)
        yield function(stats.samplesStats(sample1).sampleToSample(sample2))

      absWriter.println(values.mkString(sample1 + "\t", "\t", ""))

      val total = function(stats.samplesStats(sample1).sampleToSample(sample1))
      relWriter.println(values.map(_.toFloat / total).mkString(sample1 + "\t", "\t", ""))
    }
    absWriter.close()
    relWriter.close()

    if (plots) plotHeatmap(relFile)
  }

  /** Plots heatmaps on target tsv file */
  def plotHeatmap(file: File) {
    executeRscript(
      "plotHeatmap.R",
      Array(
        file.getAbsolutePath,
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.png",
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.clustering.png",
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.dendrogram.png"
      )
    )
  }

  /** Plots line graph with target tsv file */
  def plotLine(file: File) {
    executeRscript(
      "plotXY.R",
      Array(file.getAbsolutePath, file.getAbsolutePath.stripSuffix(".tsv") + ".xy.png"))
  }

  /** Function to execute Rscript as subproces */
  def executeRscript(resource: String, args: Array[String]): Unit = {
    val is = getClass.getResourceAsStream(resource)
    val file = File.createTempFile("script.", "." + resource)
    file.deleteOnExit()
    val os = new FileOutputStream(file)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()

    val command: String = "Rscript " + file + " " + args.mkString(" ")

    logger.info("Starting: " + command)
    try {
      val process = Process(command).run(ProcessLogger(x => logger.debug(x), x => logger.debug(x)))
      if (process.exitValue() == 0) logger.info("Done: " + command)
      else {
        logger.warn("Failed: " + command)
        if (!logger.isDebugEnabled) logger.warn("Use -l debug for more info")
      }
    } catch {
      case e: IOException =>
        logger.warn("Failed: " + command)
        logger.debug(e)
    }
  }
}
