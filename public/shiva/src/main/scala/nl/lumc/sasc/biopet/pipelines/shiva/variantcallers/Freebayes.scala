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
    fb.isIntermediate = true
    add(fb)

    //TODO: need piping for this, see also issue #114
    val bz = new Bgzip(this)
    bz.input = List(fb.outputVcf)
    bz.output = outputFile
    add(bz)

    add(Tabix.apply(this, bz.output))
  }
}
