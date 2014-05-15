package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Zcat(private var config: Config) extends CommandLineFunction {
	def this() = this(new Config(Map()))
	this.analysisName = "zcat"
	  
    @Input(doc="Zipped file") var in: File = _
    @Output(doc="Unzipped file") var out: File = _
    
    def commandLine = "zcat %s > %s".format(in, out)
}