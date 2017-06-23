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
package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.{PipelineCommand, MultiSampleQScript}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 30/08/15.
  */
class MultisamplePipeline(val parent: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  def init: Unit = {}

  def biopetScript: Unit = {
    addSamplesJobs() // This executes jobs for all samples
  }

  def addMultiSampleJobs: Unit = {
    // this code will be executed after all code of all samples is executed
  }

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
