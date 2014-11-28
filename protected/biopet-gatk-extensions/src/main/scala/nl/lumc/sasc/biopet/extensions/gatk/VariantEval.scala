package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class VariantEval(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantEval with GatkGeneral {
  override def afterGraph {
    super.afterGraph
  }
}

object VariantEval {
  def apply(root: Configurable, sample: File, compareWith: File,
            output: File): VariantEval = {
    val vareval = new VariantEval(root)
    vareval.eval = Seq(sample)
    vareval.comp = Seq(compareWith)
    vareval.out = output
    vareval.afterGraph
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
    vareval.afterGraph
    return vareval
  }

}