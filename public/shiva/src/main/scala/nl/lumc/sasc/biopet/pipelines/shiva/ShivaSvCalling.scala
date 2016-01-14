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
package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference, SampleLibraryTag }
import nl.lumc.sasc.biopet.pipelines.shiva.svcallers._
import nl.lumc.sasc.biopet.utils.{ BamUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Common trait for ShivaVariantcalling
 *
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaSvCalling(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag with Reference {
  qscript =>

  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  /** Executed before script */
  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
  }

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("sv_callers", default = Set("breakdancer", "clever", "delly", "pindel"))

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        Logging.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name))

    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty, "must select at least 1 SV caller, choices are: " + callersList.map(_.name).mkString(", "))

    callers.foreach { caller =>
      caller.outputDir = new File(outputDir, caller.name)
      add(caller)
    }

    addSummaryJobs()
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[SvCaller] = List(new Breakdancer(this), new Clever(this), new Delly(this), new Pindel(this))

  /** Location of summary file */
  def summaryFile = new File(outputDir, "ShivaSvCalling.summary.json")

  /** Settings for the summary */
  def summarySettings = Map("sv_callers" -> configCallers.toList)

  /** Files for the summary */
  def summaryFiles: Map[String, File] = {
    val callers: Set[String] = configCallers
    //callersList.filter(x => callers.contains(x.name)).map(x => x.name -> x.outputFile).toMap + ("final" -> finalFile)
    Map()
  }
}

object ShivaSvCalling extends PipelineCommand