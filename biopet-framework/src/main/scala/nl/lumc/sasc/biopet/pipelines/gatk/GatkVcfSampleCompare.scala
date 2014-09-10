package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.{ CommandLineGATK, SelectVariants, VariantEval , CombineVariants}
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }

class GatkVcfSampleCompare(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Sample vcf file(s)", shortName = "V")
  var vcfFiles: List[File] = _

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Target bed", shortName = "targetBed", required = false)
  var targetBed: List[File] = Nil
  
  @Argument(doc = "Samples", shortName = "sample", required = false)
  var samples: List[String] = Nil
  
  var vcfFile: File = _
  var sampleVcfs:Map[String, File] = Map()
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = reference
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
  
  def init() {
    if (reference == null) reference = config("reference")
    if (config.contains("target_bed")) 
      for (bed <- config("target_bed").getList) 
        targetBed :+= bed.toString
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    vcfFile = if (vcfFiles.size > 1) {
      val combineVariants = new CombineVariants with gatkArguments
      combineVariants.variant = vcfFiles
      combineVariants.out = outputDir + "merge.vcf"
      add(combineVariants)
      combineVariants.out
    } else vcfFiles.head
    
    for (sample <- samples) {
      sampleVcfs += (sample -> new File(outputDir + sample + File.separator + sample + ".vcf"))
      val selectVariants = new SelectVariants with gatkArguments
      selectVariants.variant = vcfFile
      selectVariants.out = sampleVcfs(sample)
      selectVariants.sample_name = Seq(sample)
      selectVariants.excludeNonVariants = true
      add(selectVariants)
    }
    
    for ((sample,sampleVcf) <- sampleVcfs) {
      val sampleDir = outputDir + sample + File.separator
      for ((compareSample,compareSampleVcf) <- sampleVcfs) {
        val variantEval = new VariantEval with gatkArguments
        variantEval.eval = Seq(sampleVcf)
        variantEval.comp = Seq(compareSampleVcf)
        variantEval.out = new File(sampleDir + sample + "-" + compareSample + ".eval.txt")
        variantEval.noST = true
        variantEval.ST = Seq("VariantType", "CompRod")
        variantEval.noEV = true
        variantEval.EV = Seq("CompOverlap")
        if (targetBed != null) variantEval.L = targetBed
        add(variantEval)
      }
    }
  }
}

object GatkVcfSampleCompare extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkVcfSampleCompare.class"
}
