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
    val outputDir = output.getName.stripSuffix(".zip")
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
    return jNull
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
