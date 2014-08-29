package nl.lumc.sasc.biopet.pipelines.sage

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import org.biojava3.sequencing.io.fastq.{SangerFastqReader, StreamListener, Fastq}
import scala.collection.JavaConversions._
import scala.collection.SortedMap
import scala.collection.mutable.Map
import java.io.FileReader

class CountFastq(val root: Configurable) extends BiopetJavaCommandLineFunction {
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

object CountFastq {
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
    val reader = new SangerFastqReader
    var count = 0
    System.err.println("Reading fastq file: " + input)
    val fileReader = new FileReader(input)
    reader.stream(fileReader, new StreamListener {
      def fastq(fastq:Fastq) {
        val seq = fastq.getSequence
        if (counts.contains(seq)) counts(seq) += 1
        else counts += (seq -> 1)
        count += 1
        if (count % 1000000 == 0) System.err.println(count + " sequences done")
      }
    })
    System.err.println(count + " sequences done")
    
    System.err.println("Sorting")
    val sortedCounts:SortedMap[String, Long] = SortedMap(counts.toArray:_*)
    
    System.err.println("Writting outputfile: " + output)
    val writer = new PrintWriter(output)
    for ((seq,count) <- sortedCounts) {
      writer.println(seq + "\t" + count)
    }
    writer.close
  }
}