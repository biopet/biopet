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

  def init() {
    if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    addSamplesJobs()
  }

  def summaryFile: File = new File(outputDir, "Kopisu.summary.json")

  def summaryFiles: Map[String, File] = Map()

  def summarySettings: Map[String, Any] = Map()

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def summaryFiles: Map[String, File] = Map()

    def summaryStats: Map[String, Any] = Map()

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      def summaryFiles: Map[String, File] = Map()

      def summaryStats: Map[String, Any] = Map()

      def addJobs(): Unit = {

      }
    }

    def addJobs(): Unit = {

    }
  }

  def addMultiSampleJobs(): Unit = {
  }
}

object Kopisu extends PipelineCommand
