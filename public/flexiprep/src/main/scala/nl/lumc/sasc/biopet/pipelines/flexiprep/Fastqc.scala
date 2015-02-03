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

class Fastqc(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Fastqc(root) {

  /**
   * FastQC QC modules.
   *
   * @return Mapping of FastQC module names and its contents as array of strings (one item per line)
   * @throws FileNotFoundException if the FastQC data file can not be found.
   * @throws IllegalStateException if the module mapping is empty.
   */
  @throws(classOf[FileNotFoundException])
  @throws(classOf[IllegalStateException])
  protected lazy val qcModules: Map[String, Array[String]] = {

    val outputDir = output.getAbsolutePath.stripSuffix(".zip")
    val dataFile = new File(outputDir, "fastqc_data.txt")

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
          val modName = modString
            // so we take all characters in the first line
            .takeWhile(_ != '\n')
            // and drop all characters that equals '>'
            .dropWhile(_ == '>')
            // and take all characters before the tab
            .takeWhile(_ != '\t')
          modName -> modString.split('\n')
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
  lazy val encoding: String =
    qcModules("Basic Statistics")
      .dropWhile(!_.startsWith("Encoding"))
      .head
      .stripPrefix("Encoding\t")

      if (file != null) {
        (for (
          line <- Source.fromFile(file).getLines(); if line.startsWith("#");
          values = line.split("\t*") if values.size >= 2
        ) yield Sequence(values(0), values(1))).toList
      } else Nil
    }

    val seqs = getSeqs(adapters) ::: getSeqs(contaminants)

    val block = getDataBlock("Overrepresented sequences")
    if (block == null) return Nil

    val found = for (
      line <- block if !line.startsWith("#");
      values = line.split("\t") if values.size >= 4
    ) yield values(3)
  /** Summary of the FastQC run, stored in a [[Json]] object */
  def summary: Json = {

    seqs.filter(x => found.exists(_.startsWith(x.name)))
  }
    val outputDir: String = output.getAbsolutePath.stripSuffix(".zip")
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
            name -> Map("path" -> (outputDir + relPath))
        }

    (("" := outputMap) ->: jEmptyObject)
      .fieldOrEmptyObject("")
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
