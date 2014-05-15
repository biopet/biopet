package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class FastqcToContams(private var config: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  analysisName = "getContams"
  
  @Argument(doc="Pyhton exe", shortName="script") var python_exe: String = "python"
  @Input(doc="Pyhton script", shortName="script")
  var python_script: File = new File("/data/DIV5/SASC/project-057-Florentine/analysis/pipelines/magpie/modules/gatk01/modules/flexiprep/scripts/fastqc_contam.py")
  @Input(doc="Fastqc output", shortName="fastqc", required=true) var fastqc_output: File = _
  @Input(doc="Contams input", shortName="fastqc", required=true) var contams_file: File = _
  @Output(doc="Output file", shortName="out", required=true) var out: File = _
  
  
  def commandLine = {
    required(python_exe) + 
    required(python_script) +
    required(fastqc_output.getParent()) +
    required("-c",contams_file) + 
    " > " +
    required(out)
  }
}