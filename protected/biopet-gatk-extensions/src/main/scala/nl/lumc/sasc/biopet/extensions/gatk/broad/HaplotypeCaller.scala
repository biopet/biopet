/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output
import org.broadinstitute.gatk.utils.variant.GATKVCFIndexType

class HaplotypeCaller(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.HaplotypeCaller with GatkGeneral {

  @Output(required = false)
  protected var vcfIndex: File = _

  override val defaultThreads = 1

  min_mapping_quality_score = config("minMappingQualityScore", default = 20)
  scatterCount = config("scattercount", default = 1)
  if (config.contains("dbsnp")) this.dbsnp = config("dbsnp")
  this.sample_ploidy = config("ploidy")
  if (config.contains("bamOutput")) bamOutput = config("bamOutput")
  if (config.contains("allSitePLs")) allSitePLs = config("allSitePLs")
  if (config.contains("output_mode")) {
    import org.broadinstitute.gatk.tools.walkers.genotyper.OutputMode._
    config("output_mode").asString match {
      case "EMIT_ALL_CONFIDENT_SITES" => output_mode = EMIT_ALL_CONFIDENT_SITES
      case "EMIT_ALL_SITES"           => output_mode = EMIT_ALL_SITES
      case "EMIT_VARIANTS_ONLY"       => output_mode = EMIT_VARIANTS_ONLY
      case e                          => logger.warn("output mode '" + e + "' does not exist")
    }
  }

  if (config("inputtype", default = "dna").asString == "rna") {
    dontUseSoftClippedBases = config("dontusesoftclippedbases", default = true)
    stand_call_conf = config("stand_call_conf", default = 5)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  } else {
    dontUseSoftClippedBases = config("dontusesoftclippedbases", default = false)
    stand_call_conf = config("stand_call_conf", default = 5)
    stand_emit_conf = config("stand_emit_conf", default = 0)
  }

  override def freezeFieldValues() {
    super.freezeFieldValues()
    if (out.getName.endsWith(".vcf.gz")) vcfIndex = new File(out.getAbsolutePath + ".tbi")
    if (bamOutput != null && nct.getOrElse(1) > 1) {
      logger.warn("BamOutput is on, nct/threads is forced to set on 1, this option is only for debug")
      nCoresRequest = Some(1)
    }
    nct = Some(getThreads)
    memoryLimit = Option(memoryLimit.getOrElse(2.0) * nct.getOrElse(1))
  }
}

object HaplotypeCaller {
  def apply(root: Configurable, inputFiles: List[File], outputFile: File): HaplotypeCaller = {
    val hc = new HaplotypeCaller(root)
    hc.input_file = inputFiles
    hc.out = outputFile
    if (hc.out.getName.endsWith(".vcf.gz")) hc.vcfIndex = new File(hc.out.getAbsolutePath + ".tbi")
    hc
  }

  def gvcf(root: Configurable, inputFile: File, outputFile: File): HaplotypeCaller = {
    val hc = apply(root, List(inputFile), outputFile)
    hc.emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.GVCF
    hc.variant_index_type = GATKVCFIndexType.LINEAR
    hc.variant_index_parameter = Some(hc.config("variant_index_parameter", default = 128000).asInt)
    hc
  }
}