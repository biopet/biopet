package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

class Seqstat(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("seq_stat.py")

  @Input(doc = "Fastq input", shortName = "fastqc", required = true)
  var input_fastq: File = _

  @Output(doc = "Output file", shortName = "out", required = true)
  var out: File = _

  var fmt: String = _

  def cmdLine = {
    getPythonCommand +
      optional("--fmt", fmt) +
      required("-o", out) +
      required(input_fastq)
  }

  def getSummary: Json = {
    return jNull
  }
}

object Seqstat {
  def apply(root: Configurable, fastqfile: File, outDir: String): Seqstat = {
    val seqstat = new Seqstat(root)
    val ext = fastqfile.getName.substring(fastqfile.getName.lastIndexOf("."))
    seqstat.input_fastq = fastqfile
    seqstat.out = new File(outDir + fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    return seqstat
  }

  def mergeSummarys(jsons: List[Json]): Json = {
    return jNull
  }
}
