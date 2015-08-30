package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.{ PipelineCommand, MultiSampleQScript }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 30/08/15.
 */
class MultisamplePipeline(val root: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  def init: Unit = {
  }

  def biopetScript: Unit = {
    addSamplesJobs() // This executes jobs for all samples
  }

  def addMultiSampleJobs: Unit = {
    // this code will be executed after all code of all samples is executed
  }

  def summaryFile: File = new File(outputDir, "MultisamplePipeline.summary.json")

  //TODO: Add summary
  def summaryFiles: Map[String, File] = Map()

  //TODO: Add summary
  def summarySettings: Map[String, Any] = Map()

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      //TODO: Add summary
      def summaryFiles: Map[String, File] = Map()

      //TODO: Add summary
      def summaryStats: Map[String, Any] = Map()

      def addJobs: Unit = {
        //TODO: add library specific jobs
      }
    }

    //TODO: Add summary
    def summaryFiles: Map[String, File] = Map()

    //TODO: Add summary
    def summaryStats: Map[String, Any] = Map()

    def addJobs: Unit = {
      addPerLibJobs() // This add jobs for each library
      //TODO: add sample specific jobs
    }
  }

}

object MultisamplePipeline extends PipelineCommand