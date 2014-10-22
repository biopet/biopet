package nl.lumc.sasc.biopet.tools

import htsjdk.variant.variantcontext.writer.AsyncVariantContextWriter
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import java.util.ArrayList
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }
import scala.collection.JavaConversions._

class VcfFilter(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input vcf", shortName = "I", required = true)
  var inputVcf: File = _
    
  @Output(doc = "Output vcf", shortName = "o", required = false)
  var outputVcf: File = _
  
  var minSampleDepth: Option[Int] = _
  var minTotalDepth: Option[Int] = _
  var minAlternateDepth: Option[Int] = _
  var minSamplesPass: Option[Int] = _
  var filterRefCalls: Boolean = _
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
  
  override def afterGraph {
    minSampleDepth = config("min_sample_depth")
    minTotalDepth = config("min_total_depth")
    minAlternateDepth = config("min_alternate_depth")
    minSamplesPass = config("min_samples_pass")
    filterRefCalls = config("filter_ref_calls")
  }
  
  override def commandLine = super.commandLine + 
    required("-I", inputVcf) + 
    required("-o", outputVcf) +
    optional("--minSampleDepth", minSampleDepth) +
    optional("--minTotalDepth", minTotalDepth) +
    optional("--minAlternateDepth", minAlternateDepth) + 
    optional("--minSamplesPass", minSamplesPass) +
    conditional(filterRefCalls, "--filterRefCalls")
}

object VcfFilter extends ToolCommand {
  case class Args (inputVcf:File = null, outputVcf:File = null, minSampleDepth: Int = -1, minTotalDepth: Int = -1,
                   minAlternateDepth: Int = -1, minSamplesPass: Int = 0, minBamAlternateDepth: Int = 0, filterRefCalls: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputVcf") required() maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(inputVcf = x) }
    opt[File]('o', "outputVcf") required() maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(outputVcf = x) } text("output file, default to stdout")
    opt[Int]("minSampleDepth") unbounded() action { (x, c) =>
      c.copy(minSampleDepth = x ) }
    opt[Int]("minTotalDepth") unbounded() action { (x, c) =>
      c.copy(minTotalDepth = x ) }
    opt[Int]("minAlternateDepth") unbounded() action { (x, c) =>
      c.copy(minAlternateDepth = x) }
    opt[Int]("minSamplesPass") unbounded() action { (x, c) =>
      c.copy(minSamplesPass = x) }
    opt[Int]("minBamAlternateDepth") unbounded() action { (x, c) =>
      c.copy(minBamAlternateDepth = x) }
    opt[Unit]("filterRefCalls") unbounded() action { (x, c) => 
      c.copy(filterRefCalls = true) }
  }
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)
    
    val reader = new VCFFileReader(commandArgs.inputVcf, false)
    val header = reader.getFileHeader
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().setOutputFile(commandArgs.outputVcf).build)
    writer.writeHeader(header)
    
    val bamADFields = (for (line <- header.getInfoHeaderLines if line.getID.startsWith("BAM-AD-")) yield line.getID).toList
    val bamDPFields = (for (line <- header.getInfoHeaderLines if line.getID.startsWith("BAM-DP-")) yield line.getID).toList
        
    for (record <- reader) {
      val genotypes = for (genotype <- record.getGenotypes) yield {
        val DP = if (genotype.hasDP) genotype.getDP else -1
        val AD = if (genotype.hasAD) List(genotype.getAD:_*) else Nil
        DP >= commandArgs.minSampleDepth && 
        (if (!AD.isEmpty) AD.tail.count(_ >= commandArgs.minAlternateDepth) > 0 else true) &&
        !(commandArgs.filterRefCalls && genotype.isHomRef)
      }
      
      val bamADvalues = (for (field <- bamADFields) yield {
        record.getAttribute(field, new ArrayList) match {
          case t:ArrayList[_] if t.length > 1 => {
              for (i <- 1 until t.size) yield {
                t(i) match {
                  case a:Int => a > commandArgs.minBamAlternateDepth
                  case a:String => a.toInt > commandArgs.minBamAlternateDepth
                  case _ => false
                }
              }
          }
          case _ => List(false)
        }
      }).flatten
          
      if (record.getAttributeAsInt("DP", -1) >= commandArgs.minTotalDepth && 
          genotypes.count(_ == true) >= commandArgs.minSamplesPass &&
          bamADvalues.count(_ == true) >= commandArgs.minSamplesPass)
        writer.add(record)
    }
    reader.close
    writer.close
  }
}