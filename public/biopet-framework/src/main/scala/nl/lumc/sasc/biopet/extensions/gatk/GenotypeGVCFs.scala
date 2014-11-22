package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class GenotypeGVCFs(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.GenotypeGVCFs with GatkGeneral {
  annotation ++= config("annotation", default = Seq("FisherStrand", "QualByDepth", "ChromosomeCounts")).asStringList

  if (config.contains("dbsnp")) dbsnp = config("dbsnp")
  if (config.contains("scattercount", "genotypegvcfs")) scatterCount = config("scattercount")

  if (config("inputtype", default = "dna").asString == "rna") {
    stand_call_conf = config("stand_call_conf", default = 20)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  } else {
    stand_call_conf = config("stand_call_conf", default = 30)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  }
}

object GenotypeGVCFs {
  def apply(root: Configurable, gvcfFiles: List[File], output: File): GenotypeGVCFs = {
    val gg = new GenotypeGVCFs(root)
    gg.variant = gvcfFiles
    gg.out = output
    return gg
  }
}