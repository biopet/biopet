package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.extensions.Tabix
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsCall
import nl.lumc.sasc.biopet.extensions.samtools.{FixMpileup, SamtoolsMpileup}
import nl.lumc.sasc.biopet.utils.config.Configurable

/** default mode of bcftools */
class Bcftools(val root: Configurable) extends Variantcaller {
  val name = "bcftools"
  protected def defaultPrio = 8

  def biopetScript {
    val mp = new SamtoolsMpileup(this)
    mp.input = inputBams.values.toList
    mp.u = true
    mp.reference = referenceFasta()

    val bt = new BcftoolsCall(this)
    bt.O = Some("z")
    bt.v = true
    bt.c = true

    add(mp | new FixMpileup(this) | bt > outputFile)
    add(Tabix(this, outputFile))
  }
}
