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
package nl.lumc.sasc.biopet.pipelines.kopisu

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
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

    return sampleOutput
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    return libraryOutput
  }
}

object Kopisu extends PipelineCommand
