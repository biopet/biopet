package nl.lumc.sasc.biopet.core

object BiopetExecutableProtected extends BiopetExecutable {
  def pipelines: List[MainCommand] = BiopetExecutablePublic.pipelines ::: List(
    nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare,
    nl.lumc.sasc.biopet.pipelines.basty.Basty)
  
  def tools = BiopetExecutablePublic.tools
}