/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.core

object BiopetExecutableProtected extends BiopetExecutable {
  def pipelines: List[MainCommand] = BiopetExecutablePublic.pipelines ::: List(
    nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration,
    nl.lumc.sasc.biopet.pipelines.basty.Basty)

  def tools = BiopetExecutablePublic.tools
}