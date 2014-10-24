package nl.lumc.sasc.biopet.tools

import java.io.{ BufferedInputStream, File, FileInputStream, PrintWriter }
import java.util.zip.GZIPInputStream
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import scala.io.Source
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class FastqSplitter(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output fastq files", shortName = "output", required = true)
  var output: List[File] = Nil

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)

  override def commandLine = super.commandLine + required("-I", input) + repeat("-o", output)
}

object FastqSplitter extends ToolCommand {
  case class Args (inputFile:File = null, outputFile:List[File] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required() valueName("<file>") action { (x, c) =>
      c.copy(inputFile = x) } text("out is a required file property")
    opt[File]('o', "output") required() unbounded() valueName("<file>") action { (x, c) =>
      c.copy(outputFile = x :: c.outputFile) } text("out is a required file property")
  }
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)  
    
    val groupsize = 100
    val output = for (file <- commandArgs.outputFile) yield new PrintWriter(file)
    val inputStream = {
      if (commandArgs.inputFile.getName.endsWith(".gz") || commandArgs.inputFile.getName.endsWith(".gzip")) Source.fromInputStream(
        new GZIPInputStream(
          new BufferedInputStream(
            new FileInputStream(commandArgs.inputFile)))).bufferedReader
      else Source.fromFile(commandArgs.inputFile).bufferedReader
    }
    while (inputStream.ready) {
      for (writter <- output) {
        for (t <- 1 to groupsize) {
          for (t <- 1 to (4)) {
            if (inputStream.ready) {
              writter.write(inputStream.readLine + "\n")
            }
          }
        }
      }
    }
    for (writter <- output) writter.close
  }
}
