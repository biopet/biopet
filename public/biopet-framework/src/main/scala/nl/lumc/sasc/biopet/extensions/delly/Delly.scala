package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Ln
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.CatVariants

class Delly(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  var workdir: File = _

  @Output(doc = "Delly result VCF")
  var outputvcf: File = _

  var outputBaseName: File = _

  // select the analysis types DEL,DUP,INV,TRA
  var del: Boolean = config("DEL", default = true)
  var dup: Boolean = config("DUP", default = true)
  var inv: Boolean = config("INV", default = true)
  var tra: Boolean = config("TRA", default = true)

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
      //TODO: convert to biopet extension
      val variants = new CatVariants()
      variants.variant = vcfFiles.values.toList
      variants.outputFile = this.outputvcf
      variants.reference = referenceFasta()
      // add the job
      add(variants)
      this.outputvcf
    } else {
      // TODO: pretify this
      val ln = Ln(this, vcfFiles.head._2, this.outputvcf, relative = true)
      add(ln)
      ln.output
    }

    outputFiles += ("vcf" -> this.outputvcf)
  }

  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".delly.vcf"
}

object Delly extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/extensions/svcallers/Delly/Delly.class"

  def apply(root: Configurable, input: File, runDir: File): Delly = {
    val dellypipeline = new Delly(root)
    dellypipeline.input = input
    dellypipeline.workdir = runDir
    dellypipeline.init()
    dellypipeline.biopetScript()
    dellypipeline
  }

}