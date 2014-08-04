/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.lumc.sasc.biopet.pipelines.flexiprep

import scala.io.Source

import nl.lumc.sasc.biopet.extensions.Ln
import org.broadinstitute.gatk.utils.commandline.{ Input }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class Cutadapt(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) {
  @Input(doc = "Fastq contams file", required = false)
  var contams_file: File = _
  
  override def beforeCmd() {
    super.beforeCmd
    getContamsFromFile
  }
  
  override def cmdLine = {
    if (!opt_adapter.isEmpty || !opt_anywhere.isEmpty || !opt_front.isEmpty) {
      analysisName = getClass.getName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      val lnOut = new Ln(this)
      lnOut.in = fastq_input
      lnOut.out = fastq_output
      lnOut.relative = true
      lnOut.cmd
    }
  }
  
  def getContamsFromFile {
    if (contams_file != null) {
      if (contams_file.exists()) {
        for (line <- Source.fromFile(contams_file).getLines) {
          var s: String = line.substring(line.lastIndexOf("\t") + 1, line.size)
          if (default_clip_mode == "3") opt_adapter += s
          else if (default_clip_mode == "5") opt_front += s
          else if (default_clip_mode == "both") opt_anywhere += s
          else {
            opt_adapter += s
            logger.warn("Option default_clip_mode should be '3', '5' or 'both', falling back to default: '3'")
          }
          logger.info("Adapter: " + s + " found in: " + fastq_input)
        }
      } else logger.warn("File : " + contams_file + " does not exist")
    }
  }
  
  def getSummary: Json = {
    return jNull
  }
}

object Cutadapt {
  def mergeSummarys(jsons: List[Json]): Json = {
    return jNull
  }
}
