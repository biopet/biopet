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

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input }
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import nl.lumc.sasc.biopet.extensions.aligners.{ Bwa, Star, Bowtie, Stampy }
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

/**
 * Carp pipeline
 * Chip-Seq analysis pipeline
 * This pipeline performs QC,mapping and peak calling
 */
class Carp(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
  }

  class SampleOutput extends AbstractSampleOutput {
    var mappedBamFile: File = _
  }

  def init() {
  }

  def biopetScript() {
    // Define what the pipeline should do
    // First step is QC, this will be done with Flexiprep
    // Second step is mapping, this will be done with the Mapping pipeline
    // Third step is calling peaks on the bam files produced with the mapping pipeline, this will be done with MACS2
    logger.info("Starting CArP pipeline")

    runSamplesJobs

    //val macs2 = new Macs2CallPeak(this)
    //macs2.treatment = new File("patient.bam")
    //macs2.control = new File("control.bam")
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    val sampleID: String = sampleConfig("ID").toString

    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    val bamfiles = sampleOutput.libraries.map(_._2.mappedBamFile).toList
    sampleOutput.mappedBamFile = new File(globalSampleDir + sampleID + "/" + sampleID + ".bam")
    if (bamfiles.length == 1) {
      add(Ln(this, bamfiles.head, sampleOutput.mappedBamFile))
      val oldIndex = new File(bamfiles.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
      val newIndex = new File(sampleOutput.mappedBamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
      add(Ln(this, oldIndex, newIndex))
    } else if (bamfiles.length > 1) {
      val merge = new MergeSamFiles(this)
      merge.input = bamfiles
      merge.sortOrder = "coordinate"
      merge.output = sampleOutput.mappedBamFile
      add(merge)
    }

    return sampleOutput
  }

  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput

    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"

    if (runConfig.contains("R1")) {
      val mapping = new Mapping(this)

      mapping.skipMarkduplicates = config("skip_markduplicates", default = true) // we do the dedup marking using Sambamba

      mapping.input_R1 = new File(runConfig("R1").toString)
      if (runConfig.contains("R2")) mapping.input_R2 = new File(runConfig("R2").toString)
      mapping.RGLB = runConfig("ID").toString
      mapping.RGSM = sampleConfig("ID").toString
      if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
      if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
      if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
      mapping.outputDir = runDir

      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)

      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return libraryOutput
  }

}

object Carp extends PipelineCommand
