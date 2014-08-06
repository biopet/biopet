/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import scala.io.Source

import argonaut._, Argonaut._
import scalaz._, Scalaz._

class Fastqc(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Fastqc(root) {
  def getDataBlock(name: String): Array[String] = { // Based on Fastqc v0.10.1
    val outputDir = output.getAbsolutePath.stripSuffix(".zip")
    val dataFile = new File(outputDir + "/fastqc_data.txt")
    if (!dataFile.exists) return null
    val data = Source.fromFile(dataFile).mkString
    for (block <- data.split(">>END_MODULE\n")) {
      val b = if (block.startsWith("##FastQC")) block.substring(block.indexOf("\n") + 1) else block
      if (b.startsWith(">>" + name))
        return for (line <- b.split("\n"))
          yield line
    }
    return null
  }

  def getEncoding: String = {
    val block = getDataBlock("Basic Statistics")
    if (block == null) return null
    for (
      line <- block if (line.startsWith("Encoding"))
    ) return line.stripPrefix("Encoding\t")
    return null // Could be default Sanger with a warning in the log
  }

  def getSummary: Json = {
    val subfixs = Map("plot_duplication_levels" -> "Images/duplication_levels.png",
      "plot_kmer_profiles" -> "Images/kmer_profiles.png",
      "plot_per_base_gc_content" -> "Images/per_base_gc_content.png",
      "plot_per_base_n_content" -> "Images/per_base_n_content.png",
      "plot_per_base_quality" -> "Images/per_base_quality.png",
      "plot_per_base_sequence_content" -> "Images/per_base_sequence_content.png",
      "plot_per_sequence_gc_content" -> "Images/per_sequence_gc_content.png",
      "plot_per_sequence_quality" -> "Images/per_sequence_quality.png",
      "plot_sequence_length_distribution" -> "Images/sequence_length_distribution.png",
      "fastqc_data" -> "fastqc_data.txt")
    val dir = output.getAbsolutePath.stripSuffix(".zip") + "/"
    var outputMap:Map[String,Map[String,String]] = Map()
    for ((k,v) <- subfixs) outputMap += (k -> Map("path" -> (dir+v)))

    val temp = ("" := outputMap) ->: jEmptyObject
    return temp.fieldOrEmptyObject("")
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
    return fastqcCommand
  }
}
