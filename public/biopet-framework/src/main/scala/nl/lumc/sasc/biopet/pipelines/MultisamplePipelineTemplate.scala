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

/** Template for a multisample pipeline */
class MultisamplePipelineTemplate(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  /** Location of summary file */
  def summaryFile: File = new File(outputDir, "MultisamplePipelineTemplate.summary.json")

  /** File to add to the summary */
  def summaryFiles: Map[String, File] = Map()

  /** Pipeline settings to add to the summary */
  def summarySettings: Map[String, Any] = Map()

  /** Function to make a sample */
  def makeSample(id: String) = new Sample(id)

  /** This class will contain jobs and libraries for a sample */
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    /** Sample specific files for summary */
    def summaryFiles: Map[String, File] = Map()

    /** Sample specific stats for summary */
    def summaryStats: Map[String, Any] = Map()

    /** Function to make a library */
    def makeLibrary(id: String) = new Library(id)

    /** This class will contain all jobs for a library */
    class Library(libId: String) extends AbstractLibrary(libId) {
      /** Library specific files for summary */
      def summaryFiles: Map[String, File] = Map()

      /** Library specific stats for summary */
      def summaryStats: Map[String, Any] = Map()

      /** Method to add library jobs */
      protected def addJobs(): Unit = {
      }
    }

    /** Method to add sample jobs */
    protected def addJobs(): Unit = {
    }
  }

  /** Method where multisample jobs are added */
  def addMultiSampleJobs(): Unit = {
  }

  /** This is executed before the script starts */
  def init(): Unit = {
  }

  /** Method where jobs must be added */
  def biopetScript() {
  }
}

/** Object to let to generate a main method */
object MultisamplePipelineTemplate extends PipelineCommand