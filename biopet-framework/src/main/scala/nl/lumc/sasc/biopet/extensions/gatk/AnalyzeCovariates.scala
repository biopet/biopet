package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class AnalyzeCovariates(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.AnalyzeCovariates with GatkGeneral {
}
  
object AnalyzeCovariates {
  def apply(root: Configurable, before:File, after:File, plots:File): AnalyzeCovariates = {
    val ac = new AnalyzeCovariates(root)
    ac.before = before
    ac.after = after
    ac.plots = plots
    return ac
  }
}