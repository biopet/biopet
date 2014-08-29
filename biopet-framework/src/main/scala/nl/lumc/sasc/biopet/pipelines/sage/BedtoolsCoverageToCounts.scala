package nl.lumc.sasc.biopet.pipelines.sage

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.collection.JavaConversions._
import scala.collection.SortedMap
import scala.collection.mutable.Map
import scala.io.Source

class BedtoolsCoverageToCounts(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fasta", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
    
  override def commandLine = super.commandLine + 
    required("-I", input) + 
    required("-o", output)
}

object BedtoolsCoverageToCounts {
  var input: File = _
  var output: File = _
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    for (t <- 0 until args.size) {
      args(t) match {
        case "-I" => input = new File(args(t+1))
        case "-o" => output = new File(args(t+1))
        case _ =>
      }
    }
    if (input == null || !input.exists) throw new IllegalStateException("Input file not found, use -I")
    if (output == null) throw new IllegalStateException("Output file not found, use -o")
    
    val counts:Map[String, Long] = Map()
    for (line <- Source.fromFile(input).getLines) {
      val values = line.split("\t")
      val gene = values(3)
      val count = values(6).toLong
      if (counts.contains(gene)) counts(gene) += count
      else counts += gene -> count
    }
  
    val sortedCounts:SortedMap[String, Long] = SortedMap(counts.toArray:_*)
    
    val writer = new PrintWriter(output)
    for ((seq,count) <- sortedCounts) {
      if (count > 0) writer.println(seq + "\t" + count)
    }
    writer.close
  }
}