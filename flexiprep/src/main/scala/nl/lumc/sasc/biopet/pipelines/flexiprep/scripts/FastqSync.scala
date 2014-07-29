package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

class FastqSync(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("sync_paired_end_reads.py")

  @Input(doc = "Start fastq")
  var input_start_fastq: File = _

  @Input(doc = "R1 input")
  var input_R1: File = _

  @Input(doc = "R2 input")
  var input_R2: File = _

  @Output(doc = "R1 output")
  var output_R1: File = _

  @Output(doc = "R2 output")
  var output_R2: File = _

  //No output Annotation so file 
  var output_stats: File = _

  def cmdLine = getPythonCommand +
    required(input_start_fastq) +
    required(input_R1) +
    required(input_R2) +
    required(output_R1) +
    required(output_R2) +
    " > " +
    required(output_stats)
    
  def getSummary: Json = {
    return jNull
  }
}

object FastqSync {
  def mergeSummarys(jsons:List[Json]): Json = {
    return jNull
  }
}