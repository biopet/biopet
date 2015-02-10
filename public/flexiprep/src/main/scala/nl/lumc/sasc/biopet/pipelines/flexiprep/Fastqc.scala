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

package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.{ File, FileNotFoundException }

import scala.io.Source

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
 * FastQC wrapper with added functionality for the Flexiprep pipeline
 *
 * This wrapper implements additional methods for parsing FastQC output files and aggregating everything in a summary
 * object. The current implementation is based on FastQC v0.10.1.
 */
class Fastqc(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Fastqc(root) {

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
  @throws(classOf[FileNotFoundException])
  @throws(classOf[IllegalStateException])
  def qcModules: Map[String, FastQCModule] = {

    val fqModules = Source.fromFile(dataFile)
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

    if (fqModules.isEmpty) throw new IllegalStateException("Empty FastQC data file " + dataFile.toString)
    else fqModules
  }

  /**
   * Retrieves the FASTQ file encoding as computed by FastQC.
   *
   * @return encoding name
   * @throws NoSuchElementException when the "Basic Statistics" key does not exist in the mapping or
   *                                when a line starting with "Encoding" does not exist.
   */
  @throws(classOf[NoSuchElementException])
  def encoding: String =
    qcModules("Basic Statistics")
      .lines
      .dropWhile(!_.startsWith("Encoding"))
      .head
      .stripPrefix("Encoding\t")
      .stripSuffix("\t")

  /** Case class representing a known adapter sequence */
  protected case class AdapterSequence(name: String, seq: String)

  /**
   * Retrieves overrepresented sequences found by FastQ.
   *
   * @return a [[Set]] of [[AdapterSequence]] objects.
   */
  def foundAdapters: Set[AdapterSequence] = {

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

    val found = qcModules.get("Overrepresented sequences") match {
      case None => Seq.empty[String]
      case Some(qcModule) =>
        for (
          line <- qcModule.lines if !(line.startsWith("#") || line.startsWith(">"));
          values = line.split("\t") if values.size >= 4
        ) yield values(3)
    }

    // select full sequences from known adapters and contaminants
    // based on overrepresented sequences results
    (getFastqcSeqs(adapters) ++ getFastqcSeqs(contaminants))
      .filter(x => found.exists(_.startsWith(x.name)))
  }

  /** Summary of the FastQC run, stored in a [[Json]] object */
  def summary: Json = {

    val outputMap =
      Map("plot_duplication_levels" -> "Images/duplication_levels.png",
        "plot_kmer_profiles" -> "Images/kmer_profiles.png",
        "plot_per_base_gc_content" -> "Images/per_base_gc_content.png",
        "plot_per_base_n_content" -> "Images/per_base_n_content.png",
        "plot_per_base_quality" -> "Images/per_base_quality.png",
        "plot_per_base_sequence_content" -> "Images/per_base_sequence_content.png",
        "plot_per_sequence_gc_content" -> "Images/per_sequence_gc_content.png",
        "plot_per_sequence_quality" -> "Images/per_sequence_quality.png",
        "plot_sequence_length_distribution" -> "Images/sequence_length_distribution.png",
        "fastqc_data" -> "fastqc_data.txt")
        .map {
          case (name, relPath) =>
            name -> Map("path" -> (outputDir + File.pathSeparator + relPath))
        }

    ConfigUtils.mapToJson(outputMap)
  }
}

object Fastqc {

  def apply(root: Configurable, fastqfile: File, outDir: String): Fastqc = {
    val fastqcCommand = new Fastqc(root)
    fastqcCommand.fastqfile = fastqfile
    var filename: String = fastqfile.getName()
    if (filename.endsWith(".gz")) filename = filename.substring(0, filename.size - 3)
    if (filename.endsWith(".gzip")) filename = filename.substring(0, filename.size - 5)
    if (filename.endsWith(".fastq")) filename = filename.substring(0, filename.size - 6)
    //if (filename.endsWith(".fq")) filename = filename.substring(0,filename.size - 3)
    fastqcCommand.output = new File(outDir + "/" + filename + "_fastqc.zip")
    fastqcCommand.afterGraph
    fastqcCommand
  }
}
