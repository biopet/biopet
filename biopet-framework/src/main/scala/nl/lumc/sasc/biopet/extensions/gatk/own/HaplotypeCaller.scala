package nl.lumc.sasc.biopet.extensions.gatk.own

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.variant.GATKVCFIndexType
import org.broadinstitute.gatk.queue.extensions.gatk.CatVariantsGatherer
import org.broadinstitute.gatk.queue.extensions.gatk.LocusScatterFunction
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument, Gather }

class HaplotypeCaller(val root: Configurable) extends GatkGeneral with ScatterGatherableFunction {
  val analysis = "HaplotypeCaller"
  scatterClass = classOf[LocusScatterFunction]

  if (config.contains("scattercount")) scatterCount = config("scattercount")

  @Input(required = false)
  var dbsnp: File = config("dbsnp")

  @Input(required = true)
  var input: List[File] = Nil

  @Output(required = true)
  @Gather(classOf[CatVariantsGatherer])
  var output: File = _

  var samplePloidy: Option[Int] = config("ploidy")

  var allSitePLs: Boolean = config("allSitePLs", default = false)

  var standCallConf: Option[Double] = config("stand_call_conf", default = 5.0)
  var standEmitConf: Option[Double] = config("stand_emit_conf", default = 0.0)
  var minMappingQualityScore: Option[Int] = config("min_mapping_quality_score", default = 20)

  override def afterGraph {
    super.afterGraph
    memoryLimit = Option(threads * memoryLimit.getOrElse(2.0))
  }

  override def commandLine = super.commandLine +
    repeat("-I", input) +
    required("-o", output) +
    optional("--sample_ploidy", samplePloidy) +
    conditional(allSitePLs, "--allSitePLs") +
    optional("--stand_call_conf", standCallConf) +
    optional("--stand_emit_conf", standEmitConf) +
    optional("--dbsnp", dbsnp) +
    optional("--min_mapping_quality_score", minMappingQualityScore)

  //  def useGvcf() {
  //    emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.GVCF
  //    variant_index_type = GATKVCFIndexType.LINEAR
  //    variant_index_parameter = config("variant_index_parameter", default = 128000)
  //  }
}
