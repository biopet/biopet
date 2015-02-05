package nl.lumc.sasc.biopet.tools

import java.io.{ FileOutputStream, PrintWriter, File }

import htsjdk.variant.variantcontext.{ VariantContext, Genotype }
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, ToolCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.sys.process.{ Process, ProcessLogger }
import htsjdk.samtools.util.Interval

/**
 * Created by pjvan_thof on 1/10/15.
 */
class VcfStats(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var input: File = _

  protected var outputDir: String = _

  /**
   * Set output dir and a output file
   * @param dir
   */
  def setOutputDir(dir: String): Unit = {
    outputDir = dir
    this.jobOutputFile = new File(dir + File.separator + ".vcfstats.out")
  }

  /**
   * Creates command to execute extension
   * @return
   */
  override def commandLine = super.commandLine +
    required("-I", input) +
    required("-o", outputDir)
}

object VcfStats extends ToolCommand {
  /** Commandline argument */
  case class Args(inputFile: File = null, outputDir: String = null, intervals: Option[File] = None) extends AbstractArgs

  /** Parsing commandline arguments */
  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[String]('o', "outputDir") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputDir = x)
    }
    //TODO: add interval argument
    /*
    opt[File]('i', "intervals") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(intervals = Some(x))
    }
    */
  }

  /** Class to store sample to sample compare stats */
  class SampleToSampleStats {
    /** Number of genotypes match with other sample */
    var genotypeOverlap: Int = 0

    /** Number of alleles also found in other sample */
    var alleleOverlap: Int = 0

    /** Add an other class */
    def +=(other: SampleToSampleStats) {
      this.genotypeOverlap += other.genotypeOverlap
      this.alleleOverlap += other.alleleOverlap
    }
  }

  /** class to store all sample relative stats */
  class SampleStats {
    /** Stores all genotype relative stats */
    val genotypeStats: mutable.Map[String, mutable.Map[Any, Int]] = mutable.Map()

    /** Stores sample to sample compare stats */
    val sampleToSample: mutable.Map[String, SampleToSampleStats] = mutable.Map()

    /** Add an other class */
    def +=(other: SampleStats): Unit = {
      for ((key, value) <- other.sampleToSample) {
        if (this.sampleToSample.contains(key)) this.sampleToSample(key) += value
        else this.sampleToSample(key) = value
      }
      for ((field, fieldMap) <- other.genotypeStats) {
        val thisField = this.genotypeStats.get(field)
        if (thisField.isDefined) mergeStatsMap(thisField.get, fieldMap)
        else this.genotypeStats += field -> fieldMap
      }
    }
  }

  class Stats {
    /** Stores are general stats */
    val generalStats: mutable.Map[String, mutable.Map[Any, Int]] = mutable.Map()

    /** Stores all sample/genotype specific stats */
    val samplesStats: mutable.Map[String, SampleStats] = mutable.Map()

    /** Add an other class */
    def +=(other: Stats): Stats = {
      for ((key, value) <- other.samplesStats) {
        if (this.samplesStats.contains(key)) this.samplesStats(key) += value
        else this.samplesStats(key) = value
      }
      for ((field, fieldMap) <- other.generalStats) {
        val thisField = this.generalStats.get(field)
        if (thisField.isDefined) mergeStatsMap(thisField.get, fieldMap)
        else this.generalStats += field -> fieldMap
      }
      this
    }
  }

  /**
   * Merge m2 into m1
   * @param m1
   * @param m2
   */
  def mergeStatsMap(m1: mutable.Map[Any, Int], m2: mutable.Map[Any, Int]): Unit = {
    for (key <- m2.keySet)
      m1(key) = m1.getOrElse(key, 0) + m2(key)
  }

  /**
   * Merge m2 into m1
   * @param m1
   * @param m2
   */
  def mergeNestedStatsMap(m1: mutable.Map[String, mutable.Map[Any, Int]], m2: Map[String, Map[Any, Int]]): Unit = {
    for ((field, fieldMap) <- m2) {
      if (m1.contains(field)) {
        for ((key, value) <- fieldMap) {
          if (m1(field).contains(key)) m1(field)(key) += value
          else m1(field)(key) = value
        }
      } else m1(field) = mutable.Map(fieldMap.toList: _*)
    }
  }

  protected var commandArgs: Args = _

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    logger.info("Started")
    val argsParser = new OptParser
    commandArgs = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(commandArgs.inputFile, true)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder.toList

    val intervals: List[Interval] = (
      for (
        seq <- header.getSequenceDictionary.getSequences;
        chunks = seq.getSequenceLength / 10000000;
        i <- 1 until chunks
      ) yield {
        val size = seq.getSequenceLength / chunks
        val begin = size * (i - 1) + 1
        val end = if (i >= chunks) seq.getSequenceLength else size * i
        new Interval(seq.getSequenceName, begin, end)
      }
    ).toList

    val totalBases = intervals.foldRight(0L)(_.length() + _)

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

    val statsChunks = for (interval <- intervals.par) yield {
      val reader = new VCFFileReader(commandArgs.inputFile, true)
      var chunkCounter = 0
      val stats = createStats
      logger.info("Starting on: " + interval)

      for (record <- reader.query(interval.getSequence, interval.getStart, interval.getEnd)
           if record.getStart <= interval.getEnd) {
        mergeNestedStatsMap(stats.generalStats, checkGeneral(record))
        for (sample1 <- samples) yield {
          val genotype = record.getGenotype(sample1)
          mergeNestedStatsMap(stats.samplesStats(sample1).genotypeStats, checkGenotype(record, genotype))
          for (sample2 <- samples) {
            val genotype2 = record.getGenotype(sample2)
            if (genotype.getAlleles == genotype2.getAlleles)
              stats.samplesStats(sample1).sampleToSample(sample2).genotypeOverlap += 1
            stats.samplesStats(sample1).sampleToSample(sample2).alleleOverlap += genotype.getAlleles.count(allele => genotype2.getAlleles.exists(_.basesMatch(allele)))
          }
        }
        chunkCounter += 1
      }
      status(chunkCounter, interval)
      stats
    }

    val stats = statsChunks.toList.fold(createStats)(_ += _)

    logger.info("Done reading vcf records")

    writeField("QUAL", stats.generalStats.getOrElse("QUAL", mutable.Map()))
    writeField("general", stats.generalStats.getOrElse("general", mutable.Map()))
    writeGenotypeFields(stats, commandArgs.outputDir + "/genotype_", samples)
    writeOverlap(stats, _.genotypeOverlap, commandArgs.outputDir + "/sample_compare/genotype_overlap", samples)
    writeOverlap(stats, _.alleleOverlap, commandArgs.outputDir + "/sample_compare/allele_overlap", samples)

    logger.info("Done")
  }

  /**
   * Function to check all general stats, all info expect sample/genotype specific stats
   * @param record
   * @return
   */
  protected def checkGeneral(record: VariantContext): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any): Unit = {
      val map = buffer.getOrElse(key, Map())
      buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
    }

    buffer += "QUAL" -> Map(record.getPhredScaledQual -> 1)

    addToBuffer("general", "Total")
    if (record.isBiallelic) addToBuffer("general", "Biallelic")
    if (record.isComplexIndel) addToBuffer("general", "ComplexIndel")
    if (record.isFiltered) addToBuffer("general", "Filtered")
    if (record.isFullyDecoded) addToBuffer("general", "FullyDecoded")
    if (record.isIndel) addToBuffer("general", "Indel")
    if (record.isMixed) addToBuffer("general", "Mixed")
    if (record.isMNP) addToBuffer("general", "MNP")
    if (record.isMonomorphicInSamples) addToBuffer("general", "MonomorphicInSamples")
    if (record.isNotFiltered) addToBuffer("general", "NotFiltered")
    if (record.isPointEvent) addToBuffer("general", "PointEvent")
    if (record.isPolymorphicInSamples) addToBuffer("general", "PolymorphicInSamples")
    if (record.isSimpleDeletion) addToBuffer("general", "SimpleDeletion")
    if (record.isSimpleInsertion) addToBuffer("general", "SimpleInsertion")
    if (record.isSNP) addToBuffer("general", "SNP")
    if (record.isStructuralIndel) addToBuffer("general", "StructuralIndel")
    if (record.isSymbolic) addToBuffer("general", "Symbolic")
    if (record.isSymbolicOrSV) addToBuffer("general", "SymbolicOrSV")
    if (record.isVariant) addToBuffer("general", "Variant")

    buffer.toMap
  }

  /**
   * Function to check sample/genotype specific stats
   * @param record
   * @param genotype
   * @return
   */
  protected def checkGenotype(record: VariantContext, genotype: Genotype): Map[String, Map[Any, Int]] = {
    val buffer = mutable.Map[String, Map[Any, Int]]()

    def addToBuffer(key: String, value: Any): Unit = {
      val map = buffer.getOrElse(key, Map())
      buffer += key -> (map + (value -> (map.getOrElse(value, 0) + 1)))
    }

    buffer += "DP" -> Map((if (genotype.hasDP) genotype.getDP else "not set") -> 1)
    buffer += "GQ" -> Map((if (genotype.hasGQ) genotype.getGQ else "not set") -> 1)

    val usedAlleles = (for (allele <- genotype.getAlleles) yield record.getAlleleIndex(allele)).toList

    addToBuffer("general", "Total")
    if (genotype.isHet) addToBuffer("general", "Het")
    if (genotype.isHetNonRef) addToBuffer("general", "HetNonRef")
    if (genotype.isHom) addToBuffer("general", "Hom")
    if (genotype.isHomRef) addToBuffer("general", "HomRef")
    if (genotype.isHomVar) addToBuffer("general", "HomVar")
    if (genotype.isMixed) addToBuffer("general", "Mixed")
    if (genotype.isNoCall) addToBuffer("general", "NoCall")
    if (genotype.isNonInformative) addToBuffer("general", "NonInformative")
    if (genotype.isAvailable) addToBuffer("general", "Available")
    if (genotype.isCalled) addToBuffer("general", "Called")
    if (genotype.isFiltered) addToBuffer("general", "Filtered")

    if (genotype.hasAD) {
      val ad = genotype.getAD
      for (i <- 0 until ad.size if ad(i) > 0) {
        addToBuffer("AD", ad(i))
        if (i == 0) addToBuffer("AD-ref", ad(i))
        if (i > 0) addToBuffer("AD-alt", ad(i))
        if (usedAlleles.exists(_ == i)) addToBuffer("AD-used", ad(i))
        else addToBuffer("AD-not_used", ad(i))
      }
    }

    buffer.toMap
  }

  /**
   * Function to write stats to tsv files
   * @param stats
   * @param prefix
   * @param samples
   */
  protected def writeGenotypeFields(stats: Stats, prefix: String, samples: List[String]) {
    val fields = List("DP", "GQ", "AD", "AD-ref", "AD-alt", "AD-used", "AD-not_used", "general")
    for (field <- fields) {
      writeGenotypeField(stats, prefix, samples, field)
    }
  }

  /**
   * Function to write 1 specific genotype field
   * @param stats
   * @param prefix
   * @param samples
   * @param field
   */
  protected def writeGenotypeField(stats: Stats, prefix: String, samples: List[String], field: String): Unit = {
    val file = new File(prefix + field + ".tsv")
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println(samples.mkString(field + "\t", "\t", ""))
    val keySet = (for (sample <- samples) yield stats.samplesStats(sample).genotypeStats.getOrElse(field, Map[Any, Int]()).keySet).fold(Set[Any]())(_ ++ _)
    for (key <- keySet.toList.sortWith(sortAnyAny(_, _))) {
      val values = for (sample <- samples) yield stats.samplesStats(sample).genotypeStats.getOrElse(field, Map[Any, Int]()).getOrElse(key, 0)
      writer.println(values.mkString(key + "\t", "\t", ""))
    }
    writer.close()
    plotLine(file)
  }

  /**
   * Function to write 1 specific general field
   * @param prefix
   * @param data
   * @return
   */
  protected def writeField(prefix: String, data: mutable.Map[Any, Int]): File = {
    val file = new File(commandArgs.outputDir + "/" + prefix + ".tsv")
    println(file)
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println("\t" + prefix)
    for (key <- data.keySet.toList.sortWith(sortAnyAny(_, _))) {
      writer.println(key + "\t" + data(key))
    }
    writer.close()
    file
  }

  /**
   * Function to sort Any values
   * @param a
   * @param b
   * @return
   */
  def sortAnyAny(a: Any, b: Any): Boolean = {
    a match {
      case ai: Int => {
        b match {
          case bi: Int    => ai < bi
          case bi: Double => ai < bi
          case _          => a.toString < b.toString
        }
      }
      case _ => a.toString < b.toString
    }
  }

  /**
   * Function to write sample to sample compare tsv's / heatmaps
   * @param stats
   * @param function function to extract targeted var in SampleToSampleStats
   * @param prefix
   * @param samples
   */
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

  /**
   * Plots heatmaps on target tsv file
   * @param file
   */
  def plotHeatmap(file: File) {
    executeRscript("plotHeatmap.R", Array(file.getAbsolutePath,
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.png",
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.clustering.png",
      file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.dendrogram.png"))
  }

  /**
   * Plots line graph with target tsv file
   * @param file
   */
  def plotLine(file: File) {
    executeRscript("plotXY.R", Array(file.getAbsolutePath,
      file.getAbsolutePath.stripSuffix(".tsv") + ".xy.png"))
  }

  /**
   * Function to execute Rscript as subproces
   * @param resource
   * @param args
   */
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
