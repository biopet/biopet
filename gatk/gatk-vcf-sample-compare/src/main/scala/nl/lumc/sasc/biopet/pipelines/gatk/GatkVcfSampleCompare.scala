package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK
import org.broadinstitute.gatk.queue.extensions.gatk.SelectVariants
import org.broadinstitute.gatk.queue.extensions.gatk.VariantEval
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }

class GatkVcfSampleCompare(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Sample vcf file(s)", shortName = "V")
  var vcfFiles: List[File] = _

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Target bed", shortName = "targetBed", required = false)
  var targetBed: File = _
  
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
    if (targetBed == null) targetBed = config("targetBed")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    vcfFile = vcfFiles.head
    if (vcfFiles.size > 1) {
      
    } else vcfFile = vcfFiles.head
    
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
        if (targetBed != null) variantEval.L = Seq(targetBed)
        add(variantEval)
      }
    }
  }
}

object GatkVcfSampleCompare extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkVcfSampleCompare.class"
}
