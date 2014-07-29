package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import nl.lumc.sasc.biopet.extensions.fastq.Fastqc
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
  var fastqc: Fastqc = _
  
  override def beforeCmd {
    if (fastqc != null && fmt == null) {
      fastqc.getEncoding match {
        case null => {}
        case s if (s.contains("Sanger / Illumina 1.9")) => fmt = "sanger"
        case s if (s.contains("Illumina <1.3")) => fmt = "solexa"
        case s if (s.contains("Illumina 1.3")) => fmt = "illumina"
        case s if (s.contains("Illumina 1.5")) => fmt = "illumina"
        //case _ => null
      }
    }
  }
  
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
  def apply(root: Configurable, fastqfile: File, fastqc: Fastqc, outDir: String): Seqstat = {
    val seqstat = new Seqstat(root)
    val ext = fastqfile.getName.substring(fastqfile.getName.lastIndexOf("."))
    seqstat.input_fastq = fastqfile
    seqstat.fastqc = fastqc
    seqstat.out = new File(outDir + fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    if (fastqc != null) seqstat.deps ::= fastqc.output
    return seqstat
  }
  
  def mergeSummarys(jsons:List[Json]): Json = {
    return jNull
  }
}
