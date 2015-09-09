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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.{ File, FileOutputStream, PrintWriter }

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.samtools.util.Interval
import htsjdk.variant.variantcontext.{ Allele, Genotype, VariantContext }
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommand, ToolCommandFuntion }
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.sys.process.{ Process, ProcessLogger }
import scala.util.Random

/**
 * This tool will generate statistics from a vcf file
 *
 * Created by pjvan_thof on 1/10/15.
 */
class VcfStats(val root: Configurable) extends ToolCommandFuntion with Summarizable with Reference {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var input: File = _

  @Input
  protected var index: File = null

  @Output
  protected var generalStats: File = null

  @Output
  protected var genotypeStats: File = null

  override def defaultCoreMemory = 3.0
  override def defaultThreads = 3

  protected var outputDir: File = _

  var infoTags: List[String] = Nil
  var genotypeTags: List[String] = Nil
  var allInfoTags = false
  var allGenotypeTags = false
  var reference: File = _

  override def beforeGraph(): Unit = {
    reference = referenceFasta()
    index = new File(input.getAbsolutePath + ".tbi")
  }

  /** Set output dir and a output file */
  def setOutputDir(dir: File): Unit = {
    outputDir = dir
    generalStats = new File(dir, "general.tsv")
    genotypeStats = new File(dir, "genotype-general.tsv")
    jobOutputFile = new File(dir, ".vcfstats.out")
  }

  /** Creates command to execute extension */
  override def commandLine = super.commandLine +
    required("-I", input) +
    required("-o", outputDir) +
    repeat("--infoTag", infoTags) +
    repeat("--genotypeTag", genotypeTags) +
    conditional(allInfoTags, "--allInfoTags") +
    conditional(allGenotypeTags, "--allGenotypeTags") +
    required("-R", reference)

  /** Returns general stats to the summary */
  def summaryStats: Map[String, Any] = {
    Map("info" -> (for (
      line <- Source.fromFile(generalStats).getLines().toList.tail;
      values = line.split("\t") if values.size >= 2 && !values(0).isEmpty
    ) yield values(0) -> values(1).toInt
    ).toMap)
  }

  /** return only general files to summary */
  def summaryFiles: Map[String, File] = Map(
    "general_stats" -> generalStats,
    "genotype_stats" -> genotypeStats
  )

  override def addToQscriptSummary(qscript: SummaryQScript, name: String): Unit = {
    val data = Source.fromFile(genotypeStats).getLines().map(_.split("\t")).toArray

    for (s <- 1 until data(0).size) {
      val sample = data(0)(s)
      val stats = Map("genotype" -> (for (f <- 1 until data.length) yield {
        data(f)(0) -> data(f)(s)
      }).toMap)

      val sum = new Summarizable {
        override def summaryFiles: Map[String, File] = Map()
        override def summaryStats: Map[String, Any] = stats
      }

      qscript.addSummarizable(sum, name, Some(sample))
    }
  }
}

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
                  genotypeWiggle: List[String] = Nil) extends AbstractArgs

  private val generalWiggleOptions = List("Total", "Biallelic", "ComplexIndel", "Filtered", "FullyDecoded", "Indel", "Mixed",
    "MNP", "MonomorphicInSamples", "NotFiltered", "PointEvent", "PolymorphicInSamples",
    "SimpleDeletion", "SimpleInsertion", "SNP", "StructuralIndel", "Symbolic",
    "SymbolicOrSV", "Variant")

  private val genotypeWiggleOptions = List("Total", "Het", "HetNonRef", "Hom", "HomRef", "HomVar", "Mixed", "NoCall", "NonInformative",
    "Available", "Called", "Filtered", "Variant")

  /** Parsing commandline arguments */
  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } validate {
      x => if (x.exists) success else failure("Input VCF required")
    } text "Input VCF file (required)"
    opt[File]('R', "referenceFile") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFile = x)
    } validate {
      x => if (x.exists) success else failure("Reference file required")
    } text "Fasta reference which was used to call input VCF (required)"
    opt[File]('o', "outputDir") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    } validate {
      x => if (x == null) failure("Output directory required") else success
    } text "Path to directory for output (required)"
    opt[File]('i', "intervals") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(intervals = Some(x))
    } text "Path to interval (BED) file (optional)"
    opt[String]("infoTag") unbounded () valueName "<tag>" action { (x, c) =>
      c.copy(infoTags = x :: c.infoTags)
    } text "Summarize these info tags. Default is all tags"
    opt[String]("genotypeTag") unbounded () valueName "<tag>" action { (x, c) =>
      c.copy(genotypeTags = x :: c.genotypeTags)
    } text "Summarize these genotype tags. Default is all tags"
    opt[Unit]("allInfoTags") unbounded () action { (x, c) =>
      c.copy(allInfoTags = true)
    } text "Summarize all info tags. Default false"
    opt[Unit]("allGenotypeTags") unbounded () action { (x, c) =>
      c.copy(allGenotypeTags = true)
    } text "Summarize all genotype tags. Default false"
    opt[Int]("binSize") unbounded () action { (x, c) =>
      c.copy(binSize = x)
    } text "Binsize in estimated base pairs"
    opt[Unit]("writeBinStats") unbounded () action { (x, c) =>
      c.copy(writeBinStats = true)
    } text "Write bin statistics. Default False"
    opt[String]("generalWiggle") unbounded () action { (x, c) =>
      c.copy(generalWiggle = x :: c.generalWiggle, writeBinStats = true)
    } validate {
      x => if (generalWiggleOptions.contains(x)) success else failure(s"""Nonexistent field $x""")
    } text
      """
        |Create a wiggle track with bin size <binSize> for any of the following statistics:
        |Total
        |Biallelic
        |ComplexIndel
        |Filtered
        |FullyDecoded
        |Indel
        |Mixed
        |MNP
        |MonomorphicInSamples
        |NotFiltered
        |PointEvent
        |PolymorphicInSamples
        |SimpleDeletion
        |SimpleInsertion
        |SNP
        |StructuralIndel
        |Symbolic
        |SymbolicOrSV
        |Variant
      """.stripMargin
    opt[String]("genotypeWiggle") unbounded () action { (x, c) =>
      c.copy(genotypeWiggle = x :: c.genotypeWiggle, writeBinStats = true)
    } validate {
      x => if (genotypeWiggleOptions.contains(x)) success else failure(s"""Non-existent field $x""")
    } text
      """
        |Create a wiggle track with bin size <binSize> for any of the following genotype fields:
        |Total
        |Het
        |HetNonRef
        |Hom
        |HomRef
        |HomVar
        |Mixed
        |NoCall
        |NonInformative
        |Available
        |Called
        |Filtered
        |Variant
      """.stripMargin
  }

  /**
   * Class to store sample to sample compare stats
   * @param genotypeOverlap Number of genotypes match with other sample
   * @param alleleOverlap Number of alleles also found in other sample
   */
  case class SampleToSampleStats(var genotypeOverlap: Int = 0,
                                 var alleleOverlap: Int = 0) {
    /** Add an other class */
    def +=(other: SampleToSampleStats) {
      this.genotypeOverlap += other.genotypeOverlap
      this.alleleOverlap += other.alleleOverlap
    }
  }

  /**
   * class to store all sample relative stats
   * @param genotypeStats Stores all genotype relative stats
   * @param sampleToSample Stores sample to sample compare stats
   */
  case class SampleStats(genotypeStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map(),
                         sampleToSample: mutable.Map[String, SampleToSampleStats] = mutable.Map()) {
    /** Add an other class */
    def +=(other: SampleStats): Unit = {
      for ((key, value) <- other.sampleToSample) {
        if (this.sampleToSample.contains(key)) this.sampleToSample(key) += value
        else this.sampleToSample(key) = value
      }
      for ((chr, chrMap) <- other.genotypeStats; (field, fieldMap) <- chrMap) {
        if (!this.genotypeStats.contains(chr)) genotypeStats += (chr -> mutable.Map[String, mutable.Map[Any, Int]]())
        val thisField = this.genotypeStats(chr).get(field)
        if (thisField.isDefined) mergeStatsMap(thisField.get, fieldMap)
        else this.genotypeStats(chr) += field -> fieldMap
      }
    }
  }

  /**
   * General stats class to store vcf stats
   * @param generalStats Stores are general stats
   * @param samplesStats Stores all sample/genotype specific stats
   */
  case class Stats(generalStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map(),
                   samplesStats: mutable.Map[String, SampleStats] = mutable.Map()) {
    /** Add an other class */
    def +=(other: Stats): Stats = {
      for ((key, value) <- other.samplesStats) {
        if (this.samplesStats.contains(key)) this.samplesStats(key) += value
        else this.samplesStats(key) = value
      }
      for ((chr, chrMap) <- other.generalStats; (field, fieldMap) <- chrMap) {
        if (!this.generalStats.contains(chr)) generalStats += (chr -> mutable.Map[String, mutable.Map[Any, Int]]())
        val thisField = this.generalStats(chr).get(field)
        if (thisField.isDefined) mergeStatsMap(thisField.get, fieldMap)
        else this.generalStats(chr) += field -> fieldMap
      }
      this
    }
  }

  /** Merge m2 into m1 */
  def mergeStatsMap(m1: mutable.Map[Any, Int], m2: mutable.Map[Any, Int]): Unit = {
    for (key <- m2.keySet)
      m1(key) = m1.getOrElse(key, 0) + m2(key)
  }

  /** Merge m2 into m1 */
  def mergeNestedStatsMap(m1: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]],
                          m2: Map[String, Map[String, Map[Any, Int]]]): Unit = {
    for ((chr, chrMap) <- m2; (field, fieldMap) <- chrMap) {
      if (m1.contains(chr)) {
        if (m1(chr).contains(field)) {
          for ((key, value) <- fieldMap) {
            if (m1(chr)(field).contains(key)) m1(chr)(field)(key) += value
            else m1(chr)(field)(key) = value
          }
        } else m1(chr)(field) = mutable.Map(fieldMap.toList: _*)
      } else m1(chr) = mutable.Map(field -> mutable.Map(fieldMap.toList: _*))
    }
  }

  protected var cmdArgs: Args = _

  val defaultGenotypeFields = List("DP", "GQ", "AD", "AD-ref", "AD-alt", "AD-used", "AD-not_used", "general")

  val defaultInfoFields = List("QUAL", "general", "AC", "AF", "AN", "DP")

  val sampleDistributions = List("Het", "HetNonRef", "Hom", "HomRef", "HomVar", "Mixed", "NoCall",
    "NonInformative", "Available", "Called", "Filtered", "Variant")

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    logger.info("Started")
    val argsParser = new OptParser
    cmdArgs = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(cmdArgs.inputFile, true)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder.toList

    reader.close()

    val adInfoTags = {
      (for (
        infoTag <- cmdArgs.infoTags if !defaultInfoFields.contains(infoTag)
      ) yield {
        require(header.getInfoHeaderLine(infoTag) != null, "Info tag '" + infoTag + "' not found in header of vcf file")
        infoTag
      }) ::: (for (
        line <- header.getInfoHeaderLines if cmdArgs.allInfoTags if !defaultInfoFields.contains(line.getID) if !cmdArgs.infoTags.contains(line.getID)
      ) yield {
        line.getID
      }).toList ::: defaultInfoFields
    }

    val adGenotypeTags = (for (
      genotypeTag <- cmdArgs.genotypeTags if !defaultGenotypeFields.contains(genotypeTag)
    ) yield {
      require(header.getFormatHeaderLine(genotypeTag) != null, "Format tag '" + genotypeTag + "' not found in header of vcf file")
      genotypeTag
    }) ::: (for (
      line <- header.getFormatHeaderLines if cmdArgs.allGenotypeTags if !defaultGenotypeFields.contains(line.getID) if !cmdArgs.genotypeTags.contains(line.getID) if line.getID != "PL"
    ) yield {
      line.getID
    }).toList ::: defaultGenotypeFields

    val bedRecords = (cmdArgs.intervals match {
      case Some(intervals) => BedRecordList.fromFile(intervals).validateContigs(cmdArgs.referenceFile)
      case _               => BedRecordList.fromReference(cmdArgs.referenceFile)
    }).combineOverlap.scatter(cmdArgs.binSize)

    val intervals: List[Interval] = bedRecords.toSamIntervals.toList

    val totalBases = bedRecords.length

    // Reading vcf records
    logger.info("Start reading vcf records")

    def createStats: Stats = {
      val stats = new Stats
      //init stats
      for (sample1 <- samples) {
        stats.samplesStats += sample1 -> new SampleStats
        for (sample2 <- samples) {
          stats.samplesStats(sample1).sampleToSample += sample2 -> new SampleToSampleStats
        }
      }
      stats
    }

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
    val stats = (for (intervals <- Random.shuffle(intervals).grouped(intervals.size / (if (intervals.size > 10) 4 else 1)).toList.par) yield {
      val chunkStats = for (intervals <- intervals.grouped(25)) yield {
        val binStats = for (interval <- intervals.par) yield {
          val reader = new VCFFileReader(cmdArgs.inputFile, true)
          var chunkCounter = 0
          val stats = createStats
          logger.info("Starting on: " + interval)

          for (
            record <- reader.query(interval.getContig, interval.getStart, interval.getEnd) if record.getStart <= interval.getEnd
          ) {
            mergeNestedStatsMap(stats.generalStats, checkGeneral(record, adInfoTags))
            for (sample1 <- samples) yield {
              val genotype = record.getGenotype(sample1)
              mergeNestedStatsMap(stats.samplesStats(sample1).genotypeStats, checkGenotype(record, genotype, adGenotypeTags))
              for (sample2 <- samples) {
                val genotype2 = record.getGenotype(sample2)
                if (genotype.getAlleles == genotype2.getAlleles)
                  stats.samplesStats(sample1).sampleToSample(sample2).genotypeOverlap += 1
                stats.samplesStats(sample1).sampleToSample(sample2).alleleOverlap += alleleOverlap(genotype.getAlleles.toList, genotype2.getAlleles.toList)
              }
            }
            chunkCounter += 1
          }
          reader.close()

          if (cmdArgs.writeBinStats) {
            val binOutputDir = new File(cmdArgs.outputDir, "bins" + File.separator + interval.getContig)

            writeGenotypeField(stats, samples, "general", binOutputDir, prefix = "genotype-" + interval.getStart + "-" + interval.getEnd)
            writeField(stats, "general", binOutputDir, prefix = interval.getStart + "-" + interval.getEnd)
          }

          status(chunkCounter, interval)
          stats
        }
        binStats.toList.fold(createStats)(_ += _)
      }
      chunkStats.toList.fold(createStats)(_ += _)
    }).toList.fold(createStats)(_ += _)

    logger.info("Done reading vcf records")

    // Writing info fields to tsv files
    val infoOutputDir = new File(cmdArgs.outputDir, "infotags")
    writeField(stats, "general", cmdArgs.outputDir)
    for (field <- adInfoTags.distinct.par) {
      writeField(stats, field, infoOutputDir)
      for (line <- new FastaSequenceFile(cmdArgs.referenceFile, true).getSequenceDictionary.getSequences) {
        val chr = line.getSequenceName
        writeField(stats, field, new File(infoOutputDir, "chrs" + File.separator + chr), chr = chr)
      }
    }

    // Write genotype field to tsv files
    val genotypeOutputDir = new File(cmdArgs.outputDir, "genotypetags")
    writeGenotypeField(stats, samples, "general", cmdArgs.outputDir, prefix = "genotype")
    for (field <- adGenotypeTags.distinct.par) {
      writeGenotypeField(stats, samples, field, genotypeOutputDir)
      for (line <- new FastaSequenceFile(cmdArgs.referenceFile, true).getSequenceDictionary.getSequences) {
        val chr = line.getSequenceName
        writeGenotypeField(stats, samples, field, new File(genotypeOutputDir, "chrs" + File.separator + chr), chr = chr)
      }
    }

    // Write sample distributions to tsv files
    val sampleDistributionsOutputDir = new File(cmdArgs.outputDir, "sample_distributions")
    for (field <- sampleDistributions) {
      writeField(stats, "SampleDistribution-" + field, sampleDistributionsOutputDir)
    }

    // Write general wiggle tracks
    for (field <- cmdArgs.generalWiggle) {
      val file = new File(cmdArgs.outputDir, "wigs" + File.separator + "general-" + field + ".wig")
      writeWiggle(intervals, field, "count", file, genotype = false)
    }

    // Write sample wiggle tracks
    for (field <- cmdArgs.genotypeWiggle; sample <- samples) {
      val file = new File(cmdArgs.outputDir, "wigs" + File.separator + "genotype-" + sample + "-" + field + ".wig")
      writeWiggle(intervals, field, sample, file, genotype = true)
    }

    writeOverlap(stats, _.genotypeOverlap, cmdArgs.outputDir + "/sample_compare/genotype_overlap", samples)
    writeOverlap(stats, _.alleleOverlap, cmdArgs.outputDir + "/sample_compare/allele_overlap", samples)

    logger.info("Done")
  }

  //FIXME: does only work correct for reference and not with a bed file
  protected def writeWiggle(intervals: List[Interval], row: String, column: String, outputFile: File, genotype: Boolean): Unit = {
    val groupedIntervals = intervals.groupBy(_.getContig).map { case (k, v) => k -> v.sortBy(_.getStart) }
    outputFile.getParentFile.mkdirs()
    val writer = new PrintWriter(outputFile)
    writer.println("track type=wiggle_0")
    for ((chr, intervals) <- groupedIntervals) yield {
      val length = intervals.head.length()
      writer.println(s"fixedStep chrom=$chr start=1 step=$length span=$length")
      for (interval <- intervals) {
        val file = {
          if (genotype) new File(cmdArgs.outputDir, "bins" + File.separator + chr + File.separator + "genotype-" + interval.getStart + "-" + interval.getEnd + "-general.tsv")
          else new File(cmdArgs.outputDir, "bins" + File.separator + chr + File.separator + interval.getStart + "-" + interval.getEnd + "-general.tsv")
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

  /** Give back the number of alleles that overlap */
  def alleleOverlap(g1: List[Allele], g2: List[Allele], start: Int = 0): Int = {
    if (g1.isEmpty) start
    else {
      val found = g2.contains(g1.head)
      val g2tail = if (found) {
        val index = g2.indexOf(g1.head)
        g2.drop(index + 1) ++ g2.take(index)
      } else g2

      alleleOverlap(g1.tail, g2tail, if (found) start + 1 else start)
    }
  }

  /** Function to check all general stats, all info expect sample/genotype specific stats */
  protected[tools] def checkGeneral(record: VariantContext, additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    buffer += "QUAL" -> Map(Math.round(record.getPhredScaledQual) -> 1)

    addToBuffer("SampleDistribution-Het", record.getGenotypes.count(genotype => genotype.isHet), found = true)
    addToBuffer("SampleDistribution-HetNonRef", record.getGenotypes.count(genotype => genotype.isHetNonRef), found = true)
    addToBuffer("SampleDistribution-Hom", record.getGenotypes.count(genotype => genotype.isHom), found = true)
    addToBuffer("SampleDistribution-HomRef", record.getGenotypes.count(genotype => genotype.isHomRef), found = true)
    addToBuffer("SampleDistribution-HomVar", record.getGenotypes.count(genotype => genotype.isHomVar), found = true)
    addToBuffer("SampleDistribution-Mixed", record.getGenotypes.count(genotype => genotype.isMixed), found = true)
    addToBuffer("SampleDistribution-NoCall", record.getGenotypes.count(genotype => genotype.isNoCall), found = true)
    addToBuffer("SampleDistribution-NonInformative", record.getGenotypes.count(genotype => genotype.isNonInformative), found = true)
    addToBuffer("SampleDistribution-Available", record.getGenotypes.count(genotype => genotype.isAvailable), found = true)
    addToBuffer("SampleDistribution-Called", record.getGenotypes.count(genotype => genotype.isCalled), found = true)
    addToBuffer("SampleDistribution-Filtered", record.getGenotypes.count(genotype => genotype.isFiltered), found = true)
    addToBuffer("SampleDistribution-Variant", record.getGenotypes.count(genotype => genotype.isHetNonRef || genotype.isHet || genotype.isHomVar), found = true)

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

  /** Function to check sample/genotype specific stats */
  protected[tools] def checkGenotype(record: VariantContext, genotype: Genotype, additionalTags: List[String]): Map[String, Map[String, Map[Any, Int]]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any, found: Boolean): Unit = {
      val map = buffer.getOrElse(key, Map())
      if (found) buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
      else buffer += key -> (map + (value -> map.getOrElse(value, 0)))
    }

    buffer += "DP" -> Map((if (genotype.hasDP) genotype.getDP else "not set") -> 1)
    buffer += "GQ" -> Map((if (genotype.hasGQ) genotype.getGQ else "not set") -> 1)

    val usedAlleles = (for (allele <- genotype.getAlleles) yield record.getAlleleIndex(allele)).toList

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

  /** Function to write 1 specific genotype field */
  protected def writeGenotypeField(stats: Stats, samples: List[String], field: String, outputDir: File,
                                   prefix: String = "", chr: String = "total"): Unit = {
    val file = (prefix, chr) match {
      case ("", "total") => new File(outputDir, field + ".tsv")
      case (_, "total")  => new File(outputDir, prefix + "-" + field + ".tsv")
      case ("", _)       => new File(outputDir, chr + "-" + field + ".tsv")
      case _             => new File(outputDir, prefix + "-" + chr + "-" + field + ".tsv")
    }

    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println(samples.mkString(field + "\t", "\t", ""))
    val keySet = (for (sample <- samples) yield stats.samplesStats(sample).genotypeStats.getOrElse(chr, Map[String, Map[Any, Int]]()).getOrElse(field, Map[Any, Int]()).keySet).fold(Set[Any]())(_ ++ _)
    for (key <- keySet.toList.sortWith(sortAnyAny)) {
      val values = for (sample <- samples) yield stats.samplesStats(sample).genotypeStats.getOrElse(chr, Map[String, Map[Any, Int]]()).getOrElse(field, Map[Any, Int]()).getOrElse(key, 0)
      writer.println(values.mkString(key + "\t", "\t", ""))
    }
    writer.close()

    //FIXME: plotting of thise value is broken
    //plotLine(file)
  }

  /** Function to write 1 specific general field */
  protected def writeField(stats: Stats, field: String, outputDir: File, prefix: String = "", chr: String = "total"): File = {
    val file = (prefix, chr) match {
      case ("", "total") => new File(outputDir, field + ".tsv")
      case (_, "total")  => new File(outputDir, prefix + "-" + field + ".tsv")
      case ("", _)       => new File(outputDir, chr + "-" + field + ".tsv")
      case _             => new File(outputDir, prefix + "-" + chr + "-" + field + ".tsv")
    }

    val data = stats.generalStats.getOrElse(chr, mutable.Map[String, mutable.Map[Any, Int]]()).getOrElse(field, mutable.Map[Any, Int]())

    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    for (key <- data.keySet.toList.sortWith(sortAnyAny)) {
      writer.println(key + "\t" + data(key))
    }
    writer.close()
    file
  }

  /** Function to sort Any values */
  def sortAnyAny(a: Any, b: Any): Boolean = {
    a match {
      case ai: Int =>
        b match {
          case bi: Int    => ai < bi
          case bi: Double => ai < bi
          case _          => a.toString < b.toString
        }
      case _ => a.toString < b.toString
    }
  }

  /** Function to write sample to sample compare tsv's / heatmaps */
  def writeOverlap(stats: Stats, function: SampleToSampleStats => Int,
                   prefix: String, samples: List[String]): Unit = {
    val absFile = new File(prefix + ".abs.tsv")
    val relFile = new File(prefix + ".rel.tsv")

    absFile.getParentFile.mkdirs()

    val absWriter = new PrintWriter(absFile)
    val relWriter = new PrintWriter(relFile)

    absWriter.println(samples.mkString("\t", "\t", ""))
    relWriter.println(samples.mkString("\t", "\t", ""))
    for (sample1 <- samples) {
      val values = for (sample2 <- samples) yield function(stats.samplesStats(sample1).sampleToSample(sample2))

      absWriter.println(values.mkString(sample1 + "\t", "\t", ""))

      val total = function(stats.samplesStats(sample1).sampleToSample(sample1))
      relWriter.println(values.map(_.toFloat / total).mkString(sample1 + "\t", "\t", ""))
    }
    absWriter.close()
    relWriter.close()

    plotHeatmap(relFile)
  }

  /** Plots heatmaps on target tsv file */
  def plotHeatmap(file: File) {
    executeRscript("plotHeatmap.R", Array(file.getAbsolutePath,
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.png",
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.clustering.png",
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.dendrogram.png"))
  }

  /** Plots line graph with target tsv file */
  def plotLine(file: File) {
    executeRscript("plotXY.R", Array(file.getAbsolutePath,
      file.getAbsolutePath.stripSuffix(".tsv") + ".xy.png"))
  }

  /** Function to execute Rscript as subproces */
  def executeRscript(resource: String, args: Array[String]): Unit = {
    val is = getClass.getResourceAsStream(resource)
    val file = File.createTempFile("script.", "." + resource)
    val os = new FileOutputStream(file)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()

    val command: String = "Rscript " + file + " " + args.mkString(" ")

    logger.info("Starting: " + command)
    val process = Process(command).run(ProcessLogger(x => logger.debug(x), x => logger.debug(x)))
    if (process.exitValue() == 0) logger.info("Done: " + command)
    else {
      logger.warn("Failed: " + command)
      if (!logger.isDebugEnabled) logger.warn("Use -l debug for more info")
    }
  }
}
