/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

class UnifiedGenotyper(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.UnifiedGenotyper with GatkGeneral {

  @Output(required = false)
  protected var vcfIndex: File = _

  if (config.contains("scattercount")) scatterCount = config("scattercount")
  if (config.contains("dbsnp")) this.dbsnp = config("dbsnp")
  sample_ploidy = config("ploidy")
  if (config.contains("allSitePLs")) this.allSitePLs = config("allSitePLs")

  stand_call_conf = config("stand_call_conf", default = 5)
  stand_emit_conf = config("stand_emit_conf", default = 0)

  if (config.contains("output_mode")) {
    import org.broadinstitute.gatk.tools.walkers.genotyper.OutputMode._
    config("output_mode").asString match {
      case "EMIT_ALL_CONFIDENT_SITES" => output_mode = EMIT_ALL_CONFIDENT_SITES
      case "EMIT_ALL_SITES"           => output_mode = EMIT_ALL_SITES
      case "EMIT_VARIANTS_ONLY"       => output_mode = EMIT_VARIANTS_ONLY
      case e                          => logger.warn("output mode '" + e + "' does not exist")
    }
  }

  override val defaultThreads = 1

  override def freezeFieldValues() {
    super.freezeFieldValues()

    genotype_likelihoods_model = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.BOTH
    nct = Some(getThreads)
    memoryLimit = Option(nct.getOrElse(1) * memoryLimit.getOrElse(2.0))
  }
}

object UnifiedGenotyper {
  def apply(root: Configurable, inputFiles: List[File], outputFile: File): UnifiedGenotyper = {
    val ug = new UnifiedGenotyper(root)
    ug.input_file = inputFiles
    ug.out = outputFile
    if (ug.out.getName.endsWith(".vcf.gz")) ug.vcfIndex = new File(ug.out.getAbsolutePath + ".tbi")
    ug
  }

}