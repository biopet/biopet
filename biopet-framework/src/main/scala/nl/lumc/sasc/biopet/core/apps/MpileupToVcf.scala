package nl.lumc.sasc.biopet.core.apps

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.collection.mutable.Map
import scala.io.Source
import scala.math.round

class MpileupToVcf(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fasta", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _
  
  var minDP:Option[Int] = config("min_dp")
  var minAP:Option[Int] = config("min_ap")
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
    
  override def commandLine = super.commandLine + 
    required("-I", input) + 
    required("-o", output) + 
    required("-minDP", minDP) + 
    required("-minAP", minAP)
}

object MpileupToVcf {
  var input: File = _
  var output: File = _
  var minDP = 8
  var minAP = 2
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    for (t <- 0 until args.size) {
      args(t) match {
        case "-I" => input = new File(args(t+1))
        case "-o" => output = new File(args(t+1))
        case "-minDP" => minDP = args(t+1).toInt
        case "-minAP" => minAP = args(t+1).toInt
        case _ =>
      }
    }
    if (input != null && !input.exists) throw new IllegalStateException("Input file does not exist")
    if (output == null) throw new IllegalStateException("Output file not found, use -o")
    
    val writer = new PrintWriter(output)
    writer.println("##fileformat=VCFv4.2")
    writer.println("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Total Depth\">")
    writer.println("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">")
    writer.println("##INFO=<ID=RFC,Number=1,Type=Integer,Description=\"Reference Forward Reads\">")
    writer.println("##INFO=<ID=RRC,Number=1,Type=Integer,Description=\"Reference Reverse Reads\">")
    writer.println("##INFO=<ID=AFC,Number=1,Type=Integer,Description=\"Alternetive Forward Reads\">")
    writer.println("##INFO=<ID=ARC,Number=1,Type=Integer,Description=\"Alternetive Reverse Reads\">")
    writer.println("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO")
    val inputStream = if (input != null) Source.fromFile(input).getLines else Source.stdin.getLines
    for (line <- inputStream) {
      val values = line.split("\t")
      val chr = values(0)
      val pos = values(1)
      val ref = values(2)
      val reads = values(3).toInt
      val mpileup = values(4)
      val qual = values(5)
      
      class Counts(var forward:Int, var reverse:Int)
      val counts: Map[String, Counts] = Map(ref -> new Counts(0,0))
      
      def addCount(s:String) {
        val upper = s.toUpperCase
        if (!counts.contains(upper)) counts += upper -> new Counts(0,0)
        if (s(0).isLower) counts(upper).reverse += 1
        else counts(upper).forward += 1
      }
      
      var t = 0
      while(t < mpileup.size) {
        mpileup(t) match {
          case ',' => {
              addCount(ref.toLowerCase)
              t += 1
          }
          case '.' => {
              addCount(ref.toUpperCase)
              t += 1
          }
          case '^' => t += 2
          case '$' => t += 1
          case '+' | '-' => {
              t += 1
              var size = ""
              var insert = ""
              while (mpileup(t).isDigit) {
                size += mpileup(t)
                t += 1
              }
              for (c <- t until t + size.toInt) insert = insert + mpileup(c)
              t += size.toInt
              //println(size + "\t" + insert)
          }
          case _ =>  {
              addCount(mpileup(t).toString)
              t += 1
          }
        }
      }
      
      if (reads >= minDP) for ((key, value) <- counts if key != ref if value.forward+value.reverse >= minAP) {
        val info: String = "DP=" + reads + ":RFC=" + counts(ref).forward + ":RRC=" + counts(ref).reverse +
                          ":AFC=" + value.forward + ":ARC=" + value.reverse + 
                          ":AF=" + round((value.forward+value.reverse).toDouble/reads*1E4).toDouble/1E2 + "%"
        val outputLine: Array[String] = Array(chr, pos, ".", ref, key, ".", ".", info)
        writer.println(outputLine.mkString("\t"))
      }
    }
    writer.close
  }
}