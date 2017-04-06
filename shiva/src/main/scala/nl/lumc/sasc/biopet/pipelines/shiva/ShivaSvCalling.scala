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
package nl.lumc.sasc.biopet.pipelines.shiva

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference }
import nl.lumc.sasc.biopet.extensions.Pysvtools
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaSvCallingReport.histogramBinBoundaries
import nl.lumc.sasc.biopet.pipelines.shiva.svcallers._
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{ BamUtils, Logging }
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._

/**
 * Common trait for ShivaVariantcalling
 *
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaSvCalling(val parent: Configurable) extends QScript with SummaryQScript with Reference {
  qscript =>

  def this() = this(null)

  var outputMergedVCFbySample: Map[String, File] = Map()
  var outputMergedVCF: File = _

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected[shiva] var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  /** Executed before script */
  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
    outputMergedVCF = new File(outputDir, "allsamples.merged.vcf")
  }

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("sv_callers", default = Set("breakdancer", "clever", "delly"))

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        Logging.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name))

    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty, "Please select at least 1 SV caller, choices are: " + callersList.map(_.name).mkString(", "))

    callers.foreach { caller =>
      caller.inputBams = inputBams
      caller.outputDir = new File(outputDir, caller.name)
      add(caller)
    }

    // merge VCF by sample
    for ((sample, bamFile) <- inputBams) {
      if (callers.size > 1) {
        var sampleVCFS: List[Option[File]] = List.empty
        callers.foreach { caller =>
          sampleVCFS ::= caller.outputVCF(sample)
        }
        val mergeSVcalls = new Pysvtools(this)
        mergeSVcalls.input = sampleVCFS.flatten
        mergeSVcalls.output = new File(outputDir, sample + ".merged.vcf")
        add(mergeSVcalls)
        outputMergedVCFbySample += (sample -> mergeSVcalls.output)
      } else {
        outputMergedVCFbySample += (sample -> callers.head.outputVCF(sample).get)
      }
    }

    if (inputBams.size > 1) {
      // merge all files from all samples in project
      val mergeSVcallsProject = new Pysvtools(this)
      mergeSVcallsProject.input = outputMergedVCFbySample.values.toList
      mergeSVcallsProject.output = outputMergedVCF
      add(mergeSVcallsProject)
    }
    // merging the VCF calls by project
    // basicly this will do all samples from this pipeline run
    // group by "tags"
    // sample tagging is however not available within this pipeline

    for ((sample, mergedResultFile) <- outputMergedVCFbySample) {
      lazy val counts = getVariantCounts(mergedResultFile)
      addSummarizable(new Summarizable {
        def summaryFiles = Map("output_vcf" -> mergedResultFile)
        def summaryStats = counts
      }, "parse_sv_vcf", Some(sample))
    }

    addSummaryJobs()
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[SvCaller] = List(new Breakdancer(this), new Clever(this), new Delly(this), new Pindel(this))

  /** Settings for the summary */
  def summarySettings = Map("sv_callers" -> configCallers.toList, "hist_bin_boundaries" -> histogramBinBoundaries)

  /** Files for the summary */
  def summaryFiles: Map[String, File] = if (inputBams.size > 1) Map("final_mergedvcf" -> outputMergedVCF) else Map.empty

  /** Parses a vcf-file and counts sv-s by type and size. Sv-s are divided to different size classes, the boundaries between these classes are those given in ShivaSvCallingReport.histogramBinBoundaries. */
  def getVariantCounts(vcfFile: File): Map[String, Any] = {
    val delCounts, insCounts, dupCounts, invCounts = Array.fill(histogramBinBoundaries.size + 1) { 0 }
    var traCount = 0

    val reader = new VCFFileReader(vcfFile, false)
    for (record <- reader) {
      record.getAttributeAsString("SVTYPE", "") match {
        case "TRA" | "CTX" | "ITX" => traCount += 1
        case svType => {
          val size = record.getEnd - record.getStart
          var i = 0
          while (i < histogramBinBoundaries.size && size > histogramBinBoundaries(i)) i += 1
          svType match {
            case "DEL" => delCounts(i) += 1
            case "INS" => insCounts(i) += 1
            case "DUP" => dupCounts(i) += 1
            case "INV" => invCounts(i) += 1
            case _     => logger.warn(s"Vcf file contains a record of unknown type: file-$vcfFile, type-$svType")
          }
        }
      }
    }
    reader.close()

    Map("DEL" -> delCounts, "INS" -> insCounts, "DUP" -> dupCounts, "INV" -> invCounts, "TRA" -> traCount)
  }

}

object ShivaSvCalling extends PipelineCommand