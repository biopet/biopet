package nl.lumc.sasc.biopet.core.apps

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.samtools.{SamtoolsMpileup, SamtoolsView}
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.io.Source
import scala.math.round

class MpileupToVcf(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input mpileup file", shortName = "mpileup", required = false)
  var inputMpileup: File = _
  
  @Input(doc = "Input bam file", shortName = "bam", required = false)
  var inputBam: File = _
  
  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _
  
  var minDP:Option[Int] = config("min_dp")
  var minAP:Option[Int] = config("min_ap")
  var sample: String = _
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
  
  if (config.contains("target_bed")) defaults ++= Map("samtoolsmpileup" -> Map("interval_bed" -> config("target_bed").getStringList.head))
  defaults ++= Map("samtoolsview" -> Map("b" -> true, "h" -> true))
  
  override def commandLine = {
    (if (inputMpileup == null) {
      val samtoolsView = new SamtoolsView(this)
      val samtoolsMpileup = new SamtoolsMpileup(this)
      samtoolsView.input = inputBam
      samtoolsView.cmdPipe + " | " + samtoolsMpileup.cmdPipeInput + " | "
    } else "") + 
      super.commandLine + 
      required("-o", output) + 
      required("-minDP", minDP) + 
      required("-minAP", minAP) + 
      required("-sample", sample) + 
      (if (inputBam == null) required("-I", inputMpileup) else "")
  }
}

object MpileupToVcf {
  var input: File = _
  var output: File = _
  var sample: String = _
  var minDP = 8
  var minAP = 2
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    import scala.collection.mutable.Map
    for (t <- 0 until args.size) {
      args(t) match {
        case "-I" => input = new File(args(t+1))
        case "-o" => output = new File(args(t+1))
        case "-minDP" => minDP = args(t+1).toInt
        case "-minAP" => minAP = args(t+1).toInt
        case "-sample" => sample = args(t+1)
        case _ =>
      }
    }
    if (input != null && !input.exists) throw new IllegalStateException("Input file does not exist")
    if (output == null) throw new IllegalStateException("Output file not found, use -o")
    if (sample == null) throw new IllegalStateException("Output not given, pls use -sample")
    
    val writer = new PrintWriter(output)
    writer.println("##fileformat=VCFv4.2")
    writer.println("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Total Depth\">")
    writer.println("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Total Depth\">")
    writer.println("##FORMAT=<ID=FREQ,Number=1,Type=String,Description=\"Allele Frequency\">")
    writer.println("##FORMAT=<ID=RFC,Number=1,Type=Integer,Description=\"Reference Forward Reads\">")
    writer.println("##FORMAT=<ID=RRC,Number=1,Type=Integer,Description=\"Reference Reverse Reads\">")
    writer.println("##FORMAT=<ID=AFC,Number=1,Type=Integer,Description=\"Alternetive Forward Reads\">")
    writer.println("##FORMAT=<ID=ARC,Number=1,Type=Integer,Description=\"Alternetive Reverse Reads\">")
    writer.println("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t" + sample)
    val inputStream = if (input != null) Source.fromFile(input).getLines else Source.stdin.getLines
    for (line <- inputStream; 
         val values = line.split("\t");
         if values.size > 5) {
      val chr = values(0)
      val pos = values(1)
      val ref = values(2)
      val reads = values(3).toInt
      val mpileup = values(4)
      val qual = values(5)
      
      class Counts(var forward:Int, var reverse:Int)
      val counts: Map[String, Counts] = Map(ref.toUpperCase -> new Counts(0,0))
      
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
          }
          case 'a' | 'c' | 't' | 'g' | 'A' | 'C' | 'T' | 'G' =>  {
              addCount(mpileup(t).toString)
              t += 1
          }
          case _ => t += 1
        }
      }
      
      if (reads >= minDP) for ((key, value) <- counts if key != ref.toUpperCase if value.forward+value.reverse >= minAP) {
        val info: String = "DP=" + reads
        val format: String = "DP:RFC:RRC:AFC:ARC:FREQ\t" + reads + ":" + 
                          counts(ref.toUpperCase).forward + ":" + counts(ref.toUpperCase).reverse +
                          ":" + value.forward + ":" + value.reverse + 
                          ":" + round((value.forward+value.reverse).toDouble/reads*1E4).toDouble/1E2 + "%"
        val outputLine: Array[String] = Array(chr, pos, ".", ref.toUpperCase, key, ".", ".", info, format)
        writer.println(outputLine.mkString("\t"))
      }
    }
    writer.close
  }
}