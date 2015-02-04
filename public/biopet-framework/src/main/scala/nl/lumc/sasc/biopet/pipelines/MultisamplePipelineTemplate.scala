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
package nl.lumc.sasc.biopet.pipelines

import nl.lumc.sasc.biopet.core.{ PipelineCommand, MultiSampleQScript, BiopetQScript }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript

class MultisamplePipelineTemplate(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    def makeLibrary(id: String) = new Library(id)
    class Library(libraryId: String) extends AbstractLibrary(libraryId) {
      protected def addJobs(): Unit = {
        // Library jobs
      }
    }

    protected def addJobs(): Unit = {
      // Sample jobs
    }
  }

  def addMultiSampleJobs(): Unit = {
  }

  def init(): Unit = {
  }

  def biopetScript() {
  }
}

object MultisamplePipelineTemplate extends PipelineCommand