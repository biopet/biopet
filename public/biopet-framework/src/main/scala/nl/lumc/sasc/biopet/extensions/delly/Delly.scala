package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.CatVariants
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Ln

class Delly(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Argument(doc = "Work directory")
  var workdir: String = _

  @Output(doc = "Delly result VCF")
  var outputvcf: File = _

  var outputBaseName: String = _

  // select the analysis types DEL,DUP,INV,TRA
  var del: Boolean = config("DEL", default = false)
  var dup: Boolean = config("DUP", default = false)
  var inv: Boolean = config("INV", default = false)
  var tra: Boolean = config("TRA", default = false)

  override def init() {
  }

  def biopetScript() {
    // write the pipeline here
    logger.info("Configuring Delly pipeline")
    var outputFiles: Map[String, File] = Map()
    var vcfFiles: Map[String, File] = Map()

    this.outputBaseName = workdir + input.getName.substring(0, input.getName.lastIndexOf(".bam"))
    this.outputvcf = outputBaseName + ".delly.vcf"

    /// start delly and then copy the vcf into the root directory "<sample>.delly/"
    if (del) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "DEL"
      delly.outputvcf = outputBaseName + ".delly.del.vcf"
      add(delly)
      vcfFiles += ("DEL" -> delly.outputvcf)
    }
    if (dup) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "DUP"
      delly.outputvcf = outputBaseName + ".delly.dup.vcf"
      add(delly)
      vcfFiles += ("DUP" -> delly.outputvcf)
    }
    if (inv) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "INV"
      delly.outputvcf = outputBaseName + ".delly.inv.vcf"
      add(delly)
      vcfFiles += ("INV" -> delly.outputvcf)
    }
    if (tra) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "TRA"
      delly.outputvcf = outputBaseName + ".delly.tra.vcf"
      //      vcfFiles += ("TRA" -> delly.outputvcf)
      add(delly)
    }
    // we need to merge the vcf's
    var finalVCF = if (vcfFiles.size > 1) {
      // do merging
      // CatVariants is a $org.broadinstitute.gatk.utils.commandline.CommandLineProgram$;
      val variants = new CatVariants()
      variants.jobResourceRequests :+= "h_vmem=4G"
      variants.nCoresRequest = 1
      variants.memoryLimit = Option(2.0)
      variants.variant = vcfFiles.values.toList
      variants.reference = config("reference")
      variants.outputFile = this.outputvcf
      // add the job
      add(variants)
    } else {
      // TODO: pretify this
      val ln = Ln(this, vcfFiles.head._2, this.outputvcf, relative = true)
      add(ln)
      ln.out
    }

    outputFiles += ("vcf" -> this.outputvcf)
  }

  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".delly.vcf"
}

object Delly extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/extensions/svcallers/Delly/Delly.class"

  def apply(root: Configurable, input: File, runDir: String): Delly = {
    val dellypipeline = new Delly(root)
    dellypipeline.input = input
    dellypipeline.workdir = runDir
    dellypipeline.init
    dellypipeline.biopetScript
    return dellypipeline
  }

}