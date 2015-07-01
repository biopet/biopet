/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

class VariantEval(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantEval with GatkGeneral {
  override def beforeGraph {
    super.beforeGraph
  }
}

object VariantEval {
  def apply(root: Configurable, sample: File, compareWith: File,
            output: File): VariantEval = {
    val vareval = new VariantEval(root)
    vareval.eval = Seq(sample)
    vareval.comp = Seq(compareWith)
    vareval.out = output
    vareval.beforeGraph
    return vareval
  }

  def apply(root: Configurable, sample: File, compareWith: File,
            output: File, ST: Seq[String], EV: Seq[String]): VariantEval = {
    val vareval = new VariantEval(root)
    vareval.eval = Seq(sample)
    vareval.comp = Seq(compareWith)
    vareval.out = output
    vareval.noST = true
    vareval.ST = ST
    vareval.noEV = true
    vareval.EV = EV
    vareval.beforeGraph
    return vareval
  }

}