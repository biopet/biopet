package nl.lumc.sasc.biopet.core.apps

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.zip.GZIPInputStream
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import scala.io.Source
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.sting.commandline._

class FastqSplitter(val root:Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = "nl.lumc.sasc.biopet.core.apps.FastqSplitter"
  
  @Input(doc="Input fastq", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc="Output fastq files", shortName="output", required = true)
  var output: List[File] = Nil
  
  override def commandLine = super.commandLine + required(input) + repeat(output)
}

object FastqSplitter {
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val groupsize = 100
    val input = new File(args.head)
    val output:Array[PrintWriter] = new Array[PrintWriter](args.tail.size)
    for (t <- 1 to args.tail.size) output(t-1) = new PrintWriter(args(t))
    val inputStream = {
      if (input.getName.endsWith(".gz") || input.getName.endsWith(".gzip")) Source.fromInputStream(
        new GZIPInputStream(
          new BufferedInputStream(
            new FileInputStream(input)))).bufferedReader 
      else Source.fromFile(input).bufferedReader
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
        writter.flush
      }
    }
    for (writter <- output) writter.close
  }
}
