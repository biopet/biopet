/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet

import nl.lumc.sasc.biopet.core.{BiopetExecutable, MainCommand}

object BiopetExecutableProtected extends BiopetExecutable {
  def pipelines: List[MainCommand] = BiopetExecutablePublic.pipelines ::: List(
    nl.lumc.sasc.biopet.pipelines.gatk.Shiva,
    nl.lumc.sasc.biopet.pipelines.gatk.ShivaVariantcalling,
    nl.lumc.sasc.biopet.pipelines.gatk.Basty)

  def tools = BiopetExecutablePublic.tools
}