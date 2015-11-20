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

import java.io.File

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ Reference, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.gatk.{ CombineVariants, GenotypeConcordance }
import nl.lumc.sasc.biopet.extensions.tools.VcfStats
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers._
import nl.lumc.sasc.biopet.utils.{ BamUtils, Logging }
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Common trait for ShivaVariantcalling
 *
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaVariantcallingTrait extends SummaryQScript with SampleLibraryTag with Reference {
  qscript: QScript =>

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  /** Executed before script */
  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
  }

  var referenceVcf: Option[File] = config("reference_vcf")

  var referenceVcfRegions: Option[File] = config("reference_vcf_regions")

  /** Name prefix, can override this methods if neeeded */
  def namePrefix: String = {
    (sampleId, libId) match {
      case (Some(s), Some(l)) => s + "-" + l
      case (Some(s), _)       => s
      case _                  => config("name_prefix")
    }
  }

  override def defaults = Map("bcftoolscall" -> Map("f" -> List("GQ")))

  /** Final merged output files of all variantcaller modes */
  def finalFile = new File(outputDir, namePrefix + ".final.vcf.gz")

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("variantcallers")

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        Logging.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name)).sortBy(_.prio)

    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty, "must select at least 1 variantcaller, choices are: " + callersList.map(_.name).mkString(", "))

    val cv = new CombineVariants(qscript)
    cv.outputFile = finalFile
    cv.setKey = "VariantCaller"
    cv.genotypeMergeOptions = Some("PRIORITIZE")
    cv.rodPriorityList = callers.map(_.name).mkString(",")
    for (caller <- callers) {
      caller.inputBams = inputBams
      add(caller)
      cv.addInput(caller.outputFile, caller.name)

      val vcfStats = new VcfStats(qscript)
      vcfStats.input = caller.outputFile
      vcfStats.setOutputDir(new File(caller.outputDir, "vcfstats"))
      add(vcfStats)
      addSummarizable(vcfStats, namePrefix + "-vcfstats-" + caller.name)

      referenceVcf.foreach(referenceVcfFile => {
        val gc = new GenotypeConcordance(this)
        gc.evalFile = caller.outputFile
        gc.compFile = referenceVcfFile
        gc.outputFile = new File(caller.outputDir, s"$namePrefix-genotype_concordance.${caller.name}.txt")
        referenceVcfRegions.foreach(gc.intervals ::= _)
        add(gc)
        addSummarizable(gc, s"$namePrefix-genotype_concordance-${caller.name}")
      })
    }
    add(cv)

    val vcfStats = new VcfStats(qscript)
    vcfStats.input = finalFile
    vcfStats.setOutputDir(new File(outputDir, "vcfstats"))
    vcfStats.infoTags :+= cv.setKey
    add(vcfStats)
    addSummarizable(vcfStats, namePrefix + "-vcfstats-final")

    referenceVcf.foreach(referenceVcfFile => {
      val gc = new GenotypeConcordance(this)
      gc.evalFile = finalFile
      gc.compFile = referenceVcfFile
      gc.outputFile = new File(outputDir, s"$namePrefix-genotype_concordance.final.txt")
      referenceVcfRegions.foreach(gc.intervals ::= _)
      add(gc)
      addSummarizable(gc, s"$namePrefix-genotype_concordance-final")
    })

    addSummaryJobs()
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[Variantcaller] = List(new Freebayes(this), new RawVcf(this), new Bcftools(this), new BcftoolsSingleSample(this))

  /** Location of summary file */
  def summaryFile = new File(outputDir, "ShivaVariantcalling.summary.json")

  /** Settings for the summary */
  def summarySettings = Map("variantcallers" -> configCallers.toList)

  /** Files for the summary */
  def summaryFiles: Map[String, File] = {
    val callers: Set[String] = config("variantcallers")
    callersList.filter(x => callers.contains(x.name)).map(x => x.name -> x.outputFile).toMap + ("final" -> finalFile)
  }
}