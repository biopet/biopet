/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

class AnalyzeCovariates(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.AnalyzeCovariates with GatkGeneral {
}

object AnalyzeCovariates {
  def apply(root: Configurable, before: File, after: File, plots: File): AnalyzeCovariates = {
    val ac = new AnalyzeCovariates(root)
    ac.before = before
    ac.after = after
    ac.plots = plots
    ac
  }
}