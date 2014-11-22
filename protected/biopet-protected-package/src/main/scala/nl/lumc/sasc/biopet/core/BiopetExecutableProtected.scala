package nl.lumc.sasc.biopet.core

trait BiopetExecutableProtected extends BiopetExecutablePublic {
  override def pipelines: List[MainCommand] = super.pipelines ::: List(
    nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare,
    nl.lumc.sasc.biopet.pipelines.basty.Basty)
}
