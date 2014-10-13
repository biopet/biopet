package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.config.Configurable

class UnifiedGenotyper(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.UnifiedGenotyper with GatkGeneral {
  override def afterGraph {
    super.afterGraph
    
    genotype_likelihoods_model = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.BOTH
    if (config.contains("scattercount")) scatterCount = config("scattercount")
    if (config.contains("dbsnp")) this.dbsnp = config("dbsnp")
    this.sample_ploidy = config("ploidy")
    nct = config("threads", default = 3)
    memoryLimit = Option(nct.getOrElse(1) * 2)
    if (config.contains("allSitePLs")) this.allSitePLs = config("allSitePLs")
    if (config.contains("output_mode")){
      import org.broadinstitute.gatk.tools.walkers.genotyper.OutputMode._
      config("output_mode").getString match {
        case "EMIT_ALL_CONFIDENT_SITES" => output_mode = EMIT_ALL_CONFIDENT_SITES
        case "EMIT_ALL_SITES" => output_mode = EMIT_ALL_SITES
        case "EMIT_VARIANTS_ONLY" => output_mode = EMIT_VARIANTS_ONLY
        case e => logger.warn("output mode '" + e + "' does not exist")
      }
    }

    if (config("inputtype", default = "dna").getString == "rna") {
      stand_call_conf = config("stand_call_conf", default = 5)
      stand_emit_conf = config("stand_emit_conf", default = 0)
    } else {
      stand_call_conf = config("stand_call_conf", default = 5)
      stand_emit_conf = config("stand_emit_conf", default = 0)
    }
  }
}
