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
package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.bcftools.{BcftoolsMerge, BcftoolsView}
import nl.lumc.sasc.biopet.extensions.delly.DellyCallerCall
import nl.lumc.sasc.biopet.extensions.picard.{MergeVcfs, SortVcf}
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caller delly */
class Delly(val parent: Configurable) extends SvCaller {
  def name = "delly"

  val del: Boolean = config("DEL", default = true)
  val dup: Boolean = config("DUP", default = true)
  val inv: Boolean = config("INV", default = true)
  val bnd: Boolean = config("BND", default = true)
  val ins: Boolean = config("INS", default = true)

  def biopetScript() {

    for ((sample, bamFile) <- inputBams) {
      val dellyDir = new File(outputDir, sample)

      // Use bcftools merge to merge the bcf files. Output is an uncompressed vcf
      val mergeVariants = new MergeVcfs(this)
      mergeVariants.output = new File(dellyDir, sample + ".delly.vcf.gz")

      def dellyCaller(analysistype: String, outputName: String): Unit = {
        val delly = new DellyCallerCall(this)
        delly.input = bamFile
        delly.analysistype = analysistype
        delly.isIntermediate = true
        delly.outputbcf = new File(dellyDir, sample + s".delly.$outputName.bcf")
        add(delly)

        val view = new BcftoolsView(this)
        view.input = delly.outputbcf
        view.output = new File(dellyDir, sample + s".delly.$outputName.vcf.gz")
        view.outputType = "z"
        add(view)

        // bcf files must to be concatenated with bcftools merge
        mergeVariants.input :+= view.output
      }

      if (del) dellyCaller("DEL", "del")
      if (dup) dellyCaller("DUP", "dup")
      if (inv) dellyCaller("INV", "inv")
      if (inv) dellyCaller("INS", "ins")
      if (bnd) dellyCaller("BND", "tra")

      if (mergeVariants.input.isEmpty)
        Logging.addError("At least 1 SV-type should be selected for Delly")

      add(mergeVariants)

      addVCF(sample, mergeVariants.output)
    }
  }
}
