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

  var workDir: File = _

  @Output(doc = "Delly result VCF")
  var outputVcf: File = _

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

    this.outputBaseName = workDir + input.getName.substring(0, input.getName.lastIndexOf(".bam"))
    this.outputVcf = outputBaseName + ".delly.vcf"

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
    val finalVCF = if (vcfFiles.size > 1) {
      // do merging
      // CatVariants is a $org.broadinstitute.gatk.utils.commandline.CommandLineProgram$;
      //TODO: convert to biopet extension
      val variants = new CatVariants()
      variants.variant = vcfFiles.values.toList
      variants.outputFile = this.outputVcf
      variants.reference = referenceFasta()
      // add the job
      add(variants)
      Some(outputVcf)
    } else if (vcfFiles.size == 1) {
      // TODO: pretify this
      val ln = Ln(this, vcfFiles.head._2, this.outputVcf, relative = true)
      add(ln)
      Some(ln.output)
    } else None

    finalVCF.foreach(file => outputFiles += ("vcf" -> file))
  }
}

object Delly extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/extensions/svcallers/Delly/Delly.class"

  def apply(root: Configurable, input: File, runDir: File): Delly = {
    val dellyPipeline = new Delly(root)
    dellyPipeline.input = input
    dellyPipeline.workDir = runDir
    dellyPipeline.init()
    dellyPipeline.biopetScript()
    dellyPipeline
  }

}