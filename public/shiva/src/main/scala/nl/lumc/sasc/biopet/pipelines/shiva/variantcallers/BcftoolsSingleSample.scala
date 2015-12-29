package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.File

import nl.lumc.sasc.biopet.extensions.{ Ln, Tabix }
import nl.lumc.sasc.biopet.extensions.bcftools.{ BcftoolsMerge, BcftoolsCall }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.utils.config.Configurable

/** default mode of bcftools */
class BcftoolsSingleSample(val root: Configurable) extends Variantcaller {
  val name = "bcftools_singlesample"
  protected def defaultPrio = 8

  def biopetScript {
    val sampleVcfs = for ((sample, inputBam) <- inputBams.toList) yield {
      val mp = new SamtoolsMpileup(this)
      mp.input :+= inputBam
      mp.u = true
      mp.reference = referenceFasta()

      val bt = new BcftoolsCall(this)
      bt.O = Some("z")
      bt.v = true
      bt.c = true
      bt.output = new File(outputDir, sample + ".vcf.gz")

      add(mp | bt)
      add(Tabix(this, bt.output))
      bt.output
    }

    if (sampleVcfs.size > 1) {
      val bcfmerge = new BcftoolsMerge(this)
      bcfmerge.input = sampleVcfs
      bcfmerge.output = outputFile
      bcfmerge.O = Some("z")
      add(bcfmerge)
    } else add(Ln.apply(this, sampleVcfs.head, outputFile))
    add(Tabix(this, outputFile))
  }
}
