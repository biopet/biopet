package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class FastqSync(private var config: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  analysisName = "FastqSync"
  
  @Argument(doc="Pyhton exe", shortName="script") var python_exe: String = "python"
  @Input(doc="Pyhton script", shortName="script")
  var python_script: File = new File("/home/jfjlaros/projects/ngs-misc/trunk/src/sync_paired_end_reads.py")
  
  @Input(doc="Start fastq") var input_start_fastq: File = _
  @Input(doc="R1 input") var input_R1: File = _
  @Input(doc="R2 input") var input_R2: File = _
  @Output(doc="R1 output") var output_R1: File = _
  @Output(doc="R2 output") var output_R2: File = _
  @Output(doc="stats output") var output_stats: File = _
  
  def commandLine = {
    required(python_exe) + 
    required(python_script) +
    required(input_start_fastq) +
    required(input_R1) +
    required(input_R2) +
    required(output_R1) +
    required(output_R2) +
    " > " +
    required(output_stats)
  }
}