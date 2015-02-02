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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.carp

import java.io.File

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.macs2.Macs2CallPeak
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input }
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

/**
 * Carp pipeline
 * Chip-Seq analysis pipeline
 * This pipeline performs QC,mapping and peak calling
 */
class Carp(val root: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  override def defaults = ConfigUtils.mergeMaps(Map(
    "mapping" -> Map("skip_markduplicates" -> true)
  ), super.defaults)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def makeLibrary(id: String) = new Library(id)
    class Library(libraryId: String) extends AbstractLibrary(libraryId) {
      val mapping = new Mapping(qscript)

      def addJobs(): Unit = {
        if (config.contains("R1")) {
          mapping.input_R1 = config("R1")
          if (config.contains("R2")) mapping.input_R2 = config("R2")
          mapping.libraryId = libraryId
          mapping.sampleId = sampleId
          mapping.outputDir = libDir

          mapping.init
          mapping.biopetScript
          addAll(mapping.functions)

        } else logger.error("Sample: " + sampleId + ": No R1 found for library: " + libraryId)
      }
    }

    val bamFile = createFile(".bam")
    val controls: List[String] = config("control", default = Nil)

    def addJobs(): Unit = {
      addLibsJobs()
      val bamFiles = libraries.map(_._2.mapping.finalBamFile).toList
      if (bamFiles.length == 1) {
        add(Ln(qscript, bamFiles.head, bamFile))
        val oldIndex = new File(bamFiles.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex = new File(bamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
        add(Ln(qscript, oldIndex, newIndex))
      } else if (bamFiles.length > 1) {
        val merge = new MergeSamFiles(qscript)
        merge.input = bamFiles
        merge.sortOrder = "coordinate"
        merge.output = bamFile
        add(merge)

        //TODO: Add BigWIg track
      }

      val macs2 = new Macs2CallPeak(qscript)
      macs2.treatment = bamFile
      macs2.name = sampleId
      macs2.outputdir = sampleDir + "macs2/" + macs2.name + "/"
      add(macs2)
    }
  }

  def init() {
  }

  def biopetScript() {
    // Define what the pipeline should do
    // First step is QC, this will be done with Flexiprep
    // Second step is mapping, this will be done with the Mapping pipeline
    // Third step is calling peaks on the bam files produced with the mapping pipeline, this will be done with MACS2
    logger.info("Starting CArP pipeline")

    addSamplesJobs

    for ((sampleId, sample) <- samples) {
      for (controlId <- sample.controls) {
        if (!samples.contains(controlId))
          throw new IllegalStateException("For sample: " + sampleId + " this control: " + controlId + " does not exist")
        val macs2 = new Macs2CallPeak(this)
        macs2.treatment = sample.bamFile
        macs2.control = samples(controlId).bamFile
        macs2.name = sampleId + "_VS_" + controlId
        macs2.outputdir = sample.sampleDir + "/" + "macs2/" + macs2.name + "/"
        add(macs2)
      }
    }
  }
}

object Carp extends PipelineCommand
