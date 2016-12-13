/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
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
    val pipe = fb | new Bgzip(this) > outputFile
    add(pipe)

    add(Tabix.apply(this, outputFile))
  }
}
