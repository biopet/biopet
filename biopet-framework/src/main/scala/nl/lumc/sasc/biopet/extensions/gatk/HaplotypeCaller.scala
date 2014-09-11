package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.variant.GATKVCFIndexType

class HaplotypeCaller(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.HaplotypeCaller with GatkGeneral {
  override def afterGraph {
    super.afterGraph
    
    min_mapping_quality_score = config("minMappingQualityScore", default = 20)
    if (config.contains("scattercount")) scatterCount = config("scattercount")
    if (config.contains("dbsnp")) this.dbsnp = config("dbsnp")
    nct = config("threads", default = 3)
    bamOutput = config("bamOutput")
    memoryLimit = Option(nct.getOrElse(1) * 2)
    if (config.contains("allSitePLs")) this.allSitePLs = config("allSitePLs")

    // GVCF options
    if (config("emitRefConfidence", default = "GVCF").getString == "GVCF") {
      emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.GVCF
      variant_index_type = GATKVCFIndexType.LINEAR
      variant_index_parameter = Option(128000)
    }

    if (config("inputtype", default = "dna").getString == "rna") {
      dontUseSoftClippedBases = config("dontusesoftclippedbases", default = true)
      recoverDanglingHeads = config("recoverdanglingheads", default = true)
      stand_call_conf = config("stand_call_conf", default = 0.0001)
      stand_emit_conf = config("stand_emit_conf", default = 0)
    } else {
      dontUseSoftClippedBases = config("dontusesoftclippedbases", default = false)
      recoverDanglingHeads = config("recoverdanglingheads", default = false)
      stand_call_conf = config("stand_call_conf", default = 0.0001)
      stand_emit_conf = config("stand_emit_conf", default = 0)
    }
    if (bamOutput != null && nct.getOrElse(1) > 1) {
      nct = Option(1)
      logger.warn("BamOutput is on, nct/threads is forced to set on 1, this option is only for debug")
    }
  }
}
