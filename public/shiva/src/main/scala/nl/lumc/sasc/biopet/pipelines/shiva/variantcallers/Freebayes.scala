package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.File

import nl.lumc.sasc.biopet.extensions.{ Tabix, Bgzip }
import nl.lumc.sasc.biopet.utils.config.Configurable

/** default mode of freebayes */
class Freebayes(val root: Configurable) extends Variantcaller {
  val name = "freebayes"
  protected def defaultPrio = 7

  def biopetScript {
    val fb = new nl.lumc.sasc.biopet.extensions.Freebayes(this)
    fb.bamfiles = inputBams.values.toList
    fb.outputVcf = new File(outputDir, namePrefix + ".freebayes.vcf")
    add(fb | new Bgzip(this) > outputFile)

    add(Tabix.apply(this, outputFile))
  }
}
