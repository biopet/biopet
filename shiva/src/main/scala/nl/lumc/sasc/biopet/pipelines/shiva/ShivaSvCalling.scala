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
import nl.lumc.sasc.biopet.core.{ Reference, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.Pysvtools
import nl.lumc.sasc.biopet.pipelines.shiva.svcallers._
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{ BamUtils, Logging }
import org.broadinstitute.gatk.queue.QScript

/**
 * Common trait for ShivaVariantcalling
 *
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaSvCalling(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag with Reference {
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
      lazy val counts = getVariantCounts(mergedResultFile, ShivaSvCallingReport.histogramBinBoundaries)
      addSummarizable(new Summarizable {
        def summaryFiles = Map.empty
        def summaryStats = counts
      }, "variantsBySizeAndType", Some(sample))
    }
    addSummarizable(new Summarizable {
      def summaryFiles = Map.empty
      def summaryStats = ShivaSvCallingReport.histogramBinBoundaries
    }, "histBreaksForCounts")

    addSummaryJobs()
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[SvCaller] = List(new Breakdancer(this), new Clever(this), new Delly(this), new Pindel(this))

  /** Location of summary file */
  def summaryFile = new File(outputDir, "ShivaSvCalling.summary.json")

  /** Settings for the summary */
  def summarySettings = Map("sv_callers" -> configCallers.toList)

  /** Files for the summary */
  def summaryFiles: Map[String, File] = outputMergedVCFbySample ++ (if (inputBams.size > 1) Map("final_mergedvcf" -> outputMergedVCF) else Nil)

  def getVariantCounts(vcfFile: File, breaks: Array[Int]): Map[String, Any] = {
    val delCounts, insCounts, dupCounts, invCounts = Array.fill(breaks.size + 1) { 0 }
    var traCount = 0

    val iterator = new VCFFileReader(vcfFile, false).iterator
    while (iterator.hasNext) {
      val record = iterator.next
      val svType = record.getAttributeAsString("SVTYPE", "")
      if (svType == "TRA" || svType == "CTX" || svType == "ITX") {
        traCount += 1
      } else {
        val size = record.getEnd - record.getStart
        var i = 0
        while (i < breaks.size && size > breaks(i)) i += 1
        svType match {
          case "DEL" => delCounts(i) += 1
          case "INS" => insCounts(i) += 1
          case "DUP" => dupCounts(i) += 1
          case "INV" => invCounts(i) += 1
          case _     => logger.warn(s"Vcf file contains a record of unknown type: file-$vcfFile, type-$svType")
        }
      }
    }
    iterator.close()

    var counts: Map[String, Any] = Map()
    if (isConfiguredForType("DEL")) counts = Map("DEL" -> delCounts)
    if (isConfiguredForType("INS")) counts = counts + ("INS" -> insCounts)
    if (isConfiguredForType("DUP")) counts = counts + ("DUP" -> dupCounts)
    if (isConfiguredForType("INV")) counts = counts + ("INV" -> invCounts)
    if (isConfiguredForType("TRA")) counts = counts + ("TRA" -> traCount)
    counts
  }

  def isConfiguredForType(svType: String): Boolean = {
    true
  }

}