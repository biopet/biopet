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
package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import htsjdk.samtools.{SAMSequenceDictionary, SamReaderFactory}
import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.extensions.Cnmops
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.collection.JavaConversions._

/**
  * Created by wyleung on 2-6-16.
  */
class CnmopsMethod(val parent: Configurable) extends CnvMethod with Reference {
  def name = "cnmops"

  def biopetScript(): Unit = {

    // we repeat running cnmops for all chromosomes
    val cnmopsJobs = referenceDict.getSequences
      .map(contig => {
        val cnmops = new Cnmops(this)
        cnmops.chromosome = contig.getSequenceName
        cnmops.input = inputBams.flatMap {
          case (_, bamFile) => Some(bamFile)
          case _ => None
        }.toList
        cnmops.outputDir = Some(new File(outputDir, contig.getSequenceName))
        cnmops.beforeGraph()
        cnmops
      })
      .toList

    addAll(cnmopsJobs)
    // adding output files to the outputSummary
    cnmopsJobs.foreach(job => {
      addOutput(job.chromosome, job.rawOutput)
    })

    addSummaryJobs()
  }
}
