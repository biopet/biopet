package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import htsjdk.samtools.{ SAMSequenceDictionary, SamReaderFactory }
import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.extensions.Cnmops
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.collection.JavaConversions._

/**
 * Created by wyleung on 2-6-16.
 */
class CnmopsMethod(val root: Configurable) extends CnvMethod with Reference {
  def name = "cnmops"

  def biopetScript: Unit = {

    // we repeat running cnmops for all chromosomes
    val cnmopsJobs = referenceDict.getSequences.map(contig => {
      val cnmops = new Cnmops(this)
      cnmops.chromosome = contig.getSequenceName
      cnmops.input = inputBams.flatMap {
        case (sampleName, bamFile) => Some(bamFile)
        case _                     => None
      }.toList
      cnmops.outputDir = Some(new File(outputDir, contig.getSequenceName))
      cnmops.beforeGraph
      cnmops
    }).toList

    addAll(cnmopsJobs)
    // adding output files to the outputSummary
    cnmopsJobs.foreach(job => {
      addOutput(job.chromosome, job.rawOutput)
    })

    addSummaryJobs()
  }
}
