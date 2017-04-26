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
package nl.lumc.sasc.biopet.pipelines.gwastest

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetQScript, PipelineCommand, Reference}
import nl.lumc.sasc.biopet.extensions.Snptest
import nl.lumc.sasc.biopet.extensions.gatk.{CatVariants, SelectVariants}
import nl.lumc.sasc.biopet.extensions.tools.SnptestToVcf
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 16/03/16.
  */
class GwasTest(val parent: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  val inputVcf: File = config("input_vcf")

  val phenotypeFile: File = config("phenotype_file")

  override def dictRequired = true

  override def defaults = Map("snptest" -> Map("genotype_field" -> "GP"))

  /** Init for pipeline */
  def init(): Unit = {}

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val snpTests = BedRecordList
      .fromReference(referenceFasta())
      .scatter(config("bin_size", default = 1000000))
      .allRecords
      .map { region =>
        val name = s"${region.chr}-${region.start + 1}-${region.end}"

        val regionDir = new File(outputDir, "snptest" + File.separator + region.chr)
        val bedDir =
          new File(outputDir, ".queue" + File.separator + "regions" + File.separator + region.chr)
        bedDir.mkdirs()
        val bedFile = new File(bedDir, s"$name.bed")
        BedRecordList.fromList(List(region)).writeToFile(bedFile)
        bedFile.deleteOnExit()

        val sv = new SelectVariants(this)
        sv.variant = inputVcf
        sv.out = new File(regionDir, s"$name.vcf.gz")
        sv.intervals :+= bedFile
        sv.isIntermediate = true
        add(sv)

        val snptest = new Snptest(this)
        snptest.inputGenotypes :+= sv.out
        snptest.inputSampleFiles :+= phenotypeFile
        snptest.outputFile = Some(new File(regionDir, s"$name.snptest"))
        add(snptest)

        val snptestToVcf = new SnptestToVcf(this)
        snptestToVcf.inputInfo = snptest.outputFile.get
        snptestToVcf.outputVcf = new File(regionDir, s"$name.snptest.vcf.gz")
        snptestToVcf.contig = region.chr
        add(snptestToVcf)

        region -> snptestToVcf.outputVcf
      }

    val cv = new CatVariants(this)
    cv.variant = snpTests.map(_._2).toList
    cv.outputFile = new File(outputDir, "snptest" + File.separator + "snptest.vcf.gz")
    add(cv)
  }
}

object GwasTest extends PipelineCommand
