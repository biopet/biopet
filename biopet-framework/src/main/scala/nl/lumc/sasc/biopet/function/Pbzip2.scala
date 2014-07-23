package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Pbzip2(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Zipped file")
  var input: File = _

  @Output(doc = "Unzipped file")
  var output: File = _

  executable = config("exe", default = "pbzip2")

  var decomrpess = true
  var memory: Option[Int] = config("memory")

  override val defaultVmem = (memory.getOrElse(1000) * 2 / 1000) + "G"
  override val defaultThreads = 2

  override def beforeCmd {
    if (!memory.isEmpty) memory = Option(memory.get * threads)
  }

  def cmdLine = required(executable) +
    conditional(decomrpess, "-d") +
    conditional(!decomrpess, "-z") +
    optional("-p", threads, spaceSeparated = false) +
    optional("-m", memory, spaceSeparated = false) +
    required("-c", output) +
    required(input)
}

object Pbzip2 {
  def apply(root: Configurable, input: File, output: File): Pbzip2 = {
    val pbzip2 = new Pbzip2(root)
    pbzip2.input = input
    pbzip2.output = output
    return pbzip2
  }
}