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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.{File, FileNotFoundException}

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.io.Source
import htsjdk.samtools.util.SequenceUtil.reverseComplement
import org.broadinstitute.gatk.utils.commandline.Output

/**
  * FastQC wrapper with added functionality for the Flexiprep pipeline
  *
  * This wrapper implements additional methods for parsing FastQC output files and aggregating everything in a summary
  * object. The current implementation is based on FastQC v0.10.1.
  */
class Fastqc(root: Configurable)
    extends nl.lumc.sasc.biopet.extensions.Fastqc(root)
    with Summarizable {

  /** Allow reporting of all found (potentially adapter) sequences in the FastQC */
  var sensitiveAdapterSearch: Boolean = config("sensitiveAdapterSearch", default = false)
  var enableRCtrimming: Boolean = config("enableRCtrimming", default = false)

  /** Class for storing a single FastQC module result */
  protected case class FastQCModule(name: String, status: String, lines: Seq[String])

  /** Default FastQC output directory containing actual results */
  // this is a def instead of a val since the value depends on the variable `output`, which is null on class creation
  def outputDir: File = new File(output.getAbsolutePath.stripSuffix(".zip"))

  /** Default FastQC output data file */
  // this is a def instead of a val since the value depends on the variable `output`, which is null on class creation
  def dataFile: File = new File(outputDir, "fastqc_data.txt")

  /**
    * FastQC QC modules.
    *
    * @return Mapping of FastQC module names and its contents as array of strings (one item per line)
    * @throws FileNotFoundException if the FastQC data file can not be found.
    * @throws IllegalStateException if the module lines have no content or mapping is empty.
    */
  def qcModules: Map[String, FastQCModule] = {
    val fastQCLog = Source.fromFile(dataFile)
    val fqModules: Map[String, FastQCModule] = fastQCLog
    // drop all the characters before the first module delimiter (i.e. '>>')
      .dropWhile(_ != '>')
      // pull everything into a string
      .mkString
      // split into modules
      .split(">>END_MODULE\n")
      // make map of module name -> module lines
      .map {
        case (modString) =>
          // module name is in the first line, without '>>' and before the tab character
          val Array(firstLine, otherLines) = modString
          // drop all '>>' character (start of module)
            .dropWhile(_ == '>')
            // split first line and others
            .split("\n", 2)
            // and slice them
            .slice(0, 2)
          // extract module name and module status
          val Array(modName, modStatus) = firstLine
            .split("\t", 2)
            .slice(0, 2)
          modName -> FastQCModule(modName, modStatus, otherLines.split("\n").toSeq)
      }
      .toMap

    fastQCLog.close()
    if (fqModules.isEmpty)
      throw new IllegalStateException("Empty FastQC data file " + dataFile.toString)
    else fqModules
  }

  /**
    * Retrieves a map to be used for plotting GC distribution
    *
    * @return a map with GC content [Int] as key and Count [Double] as value
    */
  def gcDistribution: Option[Map[Int, Double]] = {
    qcModules.get("Per sequence GC content").map { module =>
      module.lines
        .filter(!_.startsWith("#"))
        .map { line =>
          val tuple = line.split("\t")
          tuple(0).toInt -> tuple(1).toDouble
        }
        .toMap
    }
  }

  /**
    * Retrieves the FASTQ file encoding as computed by FastQC.
    *
    * @return encoding name
    * @throws NoSuchElementException when the "Basic Statistics" key does not exist in the mapping or
    *                                when a line starting with "Encoding" does not exist.
    */
  def encoding: String = {
    if (dataFile.exists) // On a dry run this file does not yet exist
      qcModules("Basic Statistics") //FIXME: not save
      .lines
        .dropWhile(!_.startsWith("Encoding"))
        .head
        .stripPrefix("Encoding\t")
        .stripSuffix("\t")
    else ""
  }

  protected case class BasePositionStats(mean: Double,
                                         median: Double,
                                         lowerQuartile: Double,
                                         upperQuartile: Double,
                                         percentile10th: Double,
                                         percentile90th: Double) {

    def toMap =
      Map(
        "mean" -> mean,
        "median" -> median,
        "lower_quartile" -> lowerQuartile,
        "upper_quartile" -> upperQuartile,
        "percentile_10th" -> percentile10th,
        "percentile_90th" -> percentile90th
      )
  }

  /**
    * Retrieves the base quality per position values as computed by FastQc.
    */
  def perBaseSequenceQuality: Map[String, Map[String, Double]] =
    if (dataFile.exists) {
      qcModules.get("Per base sequence quality") match {
        case None => Map()
        case Some(qcModule) =>
          val tableContents = for {
            line <- qcModule.lines if !(line.startsWith("#") || line.startsWith(">"))
            values = line.split("\t") if values.size == 7
          } yield
            (values(0),
             BasePositionStats(values(1).toDouble,
                               values(2).toDouble,
                               values(3).toDouble,
                               values(4).toDouble,
                               values(5).toDouble,
                               values(6).toDouble).toMap)
          tableContents.toMap
      }
    } else Map()

  def perBaseSequenceContent: Map[String, Map[String, Double]] =
    if (dataFile.exists) {
      qcModules.get("Per base sequence content") match {
        case None => Map()
        case Some(qcModule) =>
          val bases = qcModule.lines.head.split("\t").tail
          val tableContents = for {
            line <- qcModule.lines if !(line.startsWith("#") || line.startsWith(">"))
            values = line.split("\t") if values.size == 5
          } yield (values(0), bases.zip(values.tail.map(_.toDouble)).toMap)
          tableContents.toMap
      }
    } else Map()

  /**
    * Retrieves overrepresented sequences found by FastQ.
    *
    * @return a [[Set]] of [[AdapterSequence]] objects.
    */
  def foundAdapters: Set[AdapterSequence] = {
    if (dataFile.exists) { // On a dry run this file does not yet exist
      val modules = qcModules

      /** Returns a list of adapter and/or contaminant sequences known to FastQC */
      def getFastqcSeqs(file: Option[File]): Set[AdapterSequence] = file match {
        case None => Set.empty[AdapterSequence]
        case Some(f) =>
          (for {
            line <- Source.fromFile(f).getLines()
            if !line.startsWith("#")
            values = line.split("\t+")
            if values.size >= 2
          } yield AdapterSequence(values(0), values(1))).toSet
      }

      val adapterSet = getFastqcSeqs(adapters)
      val contaminantSet = getFastqcSeqs(contaminants)

      val foundAdapterNames: Seq[String] = modules.get("Overrepresented sequences") match {
        case None => Seq.empty[String]
        case Some(qcModule) =>
          for (line <- qcModule.lines if !(line.startsWith("#") || line.startsWith(">"));
               values = line.split("\t") if values.size >= 4) yield values(3)
      }

      // select full sequences from known adapters and contaminants
      // based on overrepresented sequences results
      val fromKnownList: Set[AdapterSequence] = contaminantSet
        .filter(x => foundAdapterNames.exists(_.startsWith(x.name)))

      val fromKnownListRC: Set[AdapterSequence] = if (enableRCtrimming) fromKnownList.map { x =>
        AdapterSequence(x.name + "_RC", reverseComplement(x.seq))
      } else Set.empty

      // list all sequences found by FastQC
      val fastQCFoundSequences: Seq[AdapterSequence] = if (sensitiveAdapterSearch) {
        modules.get("Overrepresented sequences") match {
          case None => Seq.empty
          case Some(qcModule) =>
            for (line <- qcModule.lines if !(line.startsWith("#") || line.startsWith(">"));
                 values = line.split("\t") if values.size >= 4)
              yield AdapterSequence(values(3), values(0))
        }
      } else Seq()

      val foundAdapters = modules.get("Adapter Content").map { x =>
        val header = x.lines.head.split("\t").tail.zipWithIndex
        val lines = x.lines.tail.map(_.split("\t").tail)
        val found = header
          .filter(h => lines.exists(x => x(h._2).toFloat > adapterCutoff))
          .map(_._1)
        adapterSet.filter(x => found.contains(x.name))
      }

      fromKnownList ++ fastQCFoundSequences ++ fromKnownListRC ++ foundAdapters.getOrElse(Seq())
    } else Set()
  }

  val adapterCutoff: Float = config("adapter_cutoff", default = 0.001)

  @Output
  private var outputFiles: List[File] = Nil

  def summaryFiles: Map[String, File] = {
    val outputFiles = Map(
      "plot_duplication_levels" -> ("Images" + File.separator + "duplication_levels.png"),
      "plot_kmer_profiles" -> ("Images" + File.separator + "kmer_profiles.png"),
      "plot_per_base_gc_content" -> ("Images" + File.separator + "per_base_gc_content.png"),
      "plot_per_base_n_content" -> ("Images" + File.separator + "per_base_n_content.png"),
      "plot_per_base_quality" -> ("Images" + File.separator + "per_base_quality.png"),
      "plot_per_base_sequence_content" -> ("Images" + File.separator + "per_base_sequence_content.png"),
      "plot_per_sequence_gc_content" -> ("Images" + File.separator + "per_sequence_gc_content.png"),
      "plot_per_sequence_quality" -> ("Images" + File.separator + "per_sequence_quality.png"),
      "plot_sequence_length_distribution" -> ("Images" + File.separator + "sequence_length_distribution.png"),
      "fastqc_data" -> "fastqc_data.txt"
    ).map(x => x._1 -> new File(outputDir, x._2))

    outputFiles.foreach(this.outputFiles :+= _._2)

    outputFiles ++ Map("fastq_file" -> this.fastqfile)
  }

  def summaryStats: Map[String, Any] = Map(
    "per_base_sequence_quality" -> perBaseSequenceQuality,
    "per_base_sequence_content" -> perBaseSequenceContent,
    "adapters" -> foundAdapters.map(x => x.name -> x.seq).toMap,
    "gc_distribution" -> gcDistribution
  )
}

object Fastqc {

  def apply(root: Configurable, fastqfile: File, outDir: File): Fastqc = {
    val fastqcCommand = new Fastqc(root)
    fastqcCommand.fastqfile = fastqfile
    var filename: String = fastqfile.getName
    if (filename.endsWith(".gz")) filename = filename.substring(0, filename.length - 3)
    if (filename.endsWith(".gzip")) filename = filename.substring(0, filename.length - 5)
    if (filename.endsWith(".fastq")) filename = filename.substring(0, filename.length - 6)
    //if (filename.endsWith(".fq")) filename = filename.substring(0,filename.size - 3)
    fastqcCommand.output = new File(outDir, filename + "_fastqc.zip")
    fastqcCommand.beforeGraph()
    fastqcCommand
  }
}
