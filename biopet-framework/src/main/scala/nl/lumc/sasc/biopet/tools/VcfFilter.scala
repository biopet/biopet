package nl.lumc.sasc.biopet.tools

import htsjdk.variant.variantcontext.writer.AsyncVariantContextWriter
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
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
    optional("-minSampleDepth", minSampleDepth) +
    optional("-minTotalDepth", minTotalDepth) +
    optional("-minAlternateDepth", minAlternateDepth) + 
    optional("-minSamplesPass", minSamplesPass) +
    conditional(filterRefCalls, "-filterRefCalls")
}

object VcfFilter {
  var inputVcf: File = _
  var outputVcf: File = _
  var minSampleDepth = -1
  var minTotalDepth = -1
  var minAlternateDepth = -1
  var minSamplesPass = 0
  var filterRefCalls = false
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    for (t <- 0 until args.size) {
      args(t) match {
        case "-I" => inputVcf =  new File(args(t+1))
        case "-o" => outputVcf = new File(args(t+1))
        case "-minSampleDepth" => minSampleDepth = args(t+1).toInt
        case "-minTotalDepth" => minTotalDepth = args(t+1).toInt
        case "-minAlternateDepth" => minAlternateDepth = args(t+1).toInt
        case "-minSamplesPass" => minSamplesPass = args(t+1).toInt
        case "-filterRefCalls" => filterRefCalls = true
        case _ =>
      }
    }
    if (inputVcf == null) throw new IllegalStateException("No inputVcf, use -I")
    if (outputVcf == null) throw new IllegalStateException("No outputVcf, use -o")
    
    val reader = new VCFFileReader(inputVcf, false)
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().setOutputFile(outputVcf).build)
    writer.writeHeader(reader.getFileHeader)
    for (record <- reader) {
      val genotypes = for (genotype <- record.getGenotypes) yield {
        genotype.getDP >= minSampleDepth && 
        List(genotype.getAD:_*).tail.count(_ >= minAlternateDepth) > 0 &&
        !(filterRefCalls && genotype.isHomRef)
      }
      
      if (record.getAttributeAsInt("DP", -1) >= minTotalDepth && genotypes.count(_ == true) >= minSamplesPass)
        writer.add(record)
    }
    reader.close
    writer.close
  }
}