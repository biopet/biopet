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
package nl.lumc.sasc.biopet.pipelines.sage

import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.tools.PrefixFastq
import nl.lumc.sasc.biopet.tools.BedtoolsCoverageToCounts
import nl.lumc.sasc.biopet.scripts.SquishBed
import nl.lumc.sasc.biopet.tools.SageCountFastq
import nl.lumc.sasc.biopet.tools.SageCreateLibrary
import nl.lumc.sasc.biopet.tools.SageCreateTagCounts
import org.broadinstitute.gatk.queue.QScript

class Kopisu(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  @Input(doc = "Input bamfile", required = true)
  var bamFile: File = config("bam")

  class LibraryOutput extends AbstractLibraryOutput {
  }

  class SampleOutput extends AbstractSampleOutput {
  }

  def init() {
    if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    runSamplesJobs
  }

  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var libraryFastqFiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir: String = globalSampleDir + sampleID + "/"

    for ((library, libraryFiles) <- runLibraryJobs(sampleConfig)) {
    }

    return sampleOutput
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"
    if (runConfig.contains("bam")) {
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return libraryOutput
  }
}

object Kopisu extends PipelineCommand
