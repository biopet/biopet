/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

class GenotypeGVCFs(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.GenotypeGVCFs with GatkGeneral {

  @Output(required = false)
  protected var vcfIndex: File = _

  annotation ++= config("annotation", default = Seq(), freeVar = false).asStringList

  if (config.contains("dbsnp")) dbsnp = config("dbsnp")
  if (config.contains("scattercount", "genotypegvcfs")) scatterCount = config("scattercount")

  if (config("inputtype", default = "dna").asString == "rna") {
    stand_call_conf = config("stand_call_conf", default = 20)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  } else {
    stand_call_conf = config("stand_call_conf", default = 30)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  }

  override def freezeFieldValues(): Unit = {
    super.freezeFieldValues()
    if (out.getName.endsWith(".vcf.gz")) vcfIndex = new File(out.getAbsolutePath + ".tbi")
  }
}

object GenotypeGVCFs {
  def apply(root: Configurable, gvcfFiles: List[File], output: File): GenotypeGVCFs = {
    val gg = new GenotypeGVCFs(root)
    gg.variant = gvcfFiles
    gg.out = output
    if (gg.out.getName.endsWith(".vcf.gz")) gg.vcfIndex = new File(gg.out.getAbsolutePath + ".tbi")
    gg
  }
}