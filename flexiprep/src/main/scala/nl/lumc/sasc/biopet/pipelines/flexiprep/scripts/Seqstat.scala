package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import nl.lumc.sasc.biopet.function.fastq.Fastqc
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.PythonCommandLineFunction

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
}
