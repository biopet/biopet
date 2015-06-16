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

import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript, PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{ Tabix, Bgzip, Gzip }
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsCall
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.tools.{ VcfStats, VcfFilter, MpileupToVcf }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.generic.Sorted

/**
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaVariantcallingTrait extends SummaryQScript with SampleLibraryTag with Reference {
  qscript =>

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  var inputBams: List[File] = Nil

  /** Name prefix, can override this methods if neeeded */
  def namePrefix: String = {
    (sampleId, libId) match {
      case (Some(sampleId), Some(libId)) => sampleId + "-" + libId
      case (Some(sampleId), _)           => sampleId
      case _                             => config("name_prefix")
    }
  }

  /** Executed before script */
  def init: Unit = {
  }

  /** Final merged output files of all variantcaller modes */
  def finalFile = new File(outputDir, namePrefix + ".final.vcf.gz")

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("variantcallers")

  /** This will add jobs for this pipeline */
  def biopetScript: Unit = {
    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        BiopetQScript.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name)).sortBy(_.prio)

    require(!inputBams.isEmpty, "No input bams found")
    require(!callers.isEmpty, "must select at least 1 variantcaller, choices are: " + callersList.map(_.name).mkString(", "))

    val cv = new CombineVariants(qscript)
    cv.outputFile = finalFile
    cv.setKey = "VariantCaller"
    cv.genotypeMergeOptions = Some("PRIORITIZE")
    cv.rodPriorityList = callers.map(_.name).mkString(",")
    for (caller <- callers) {
      caller.addJobs()
      cv.addInput(caller.outputFile, caller.name)

      val vcfStats = new VcfStats(qscript)
      vcfStats.input = caller.outputFile
      vcfStats.setOutputDir(new File(caller.outputDir, "vcfstats"))
      add(vcfStats)
      addSummarizable(vcfStats, namePrefix + "-vcfstats-" + caller.name)
    }
    add(cv)

    val vcfStats = new VcfStats(qscript)
    vcfStats.input = finalFile
    vcfStats.setOutputDir(new File(outputDir, "vcfstats"))
    vcfStats.infoTags :+= cv.setKey
    add(vcfStats)
    addSummarizable(vcfStats, namePrefix + "-vcfstats-final")

    addSummaryJobs
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[Variantcaller] = List(new Freebayes, new RawVcf, new Bcftools)

  /** General trait for a variantcaller mode */
  trait Variantcaller {
    /** Name of mode, this should also be used in the config */
    val name: String

    /** Output dir for this mode */
    def outputDir = new File(qscript.outputDir, name)

    /** Prio in merging  in the final file */
    protected val defaultPrio: Int

    /** Prio from the config */
    lazy val prio: Int = config("prio_" + name, default = defaultPrio)

    /** This should add the variantcaller jobs */
    def addJobs()

    /** Final output file of this mode */
    def outputFile: File
  }

  /** default mode of freebayes */
  class Freebayes extends Variantcaller {
    val name = "freebayes"
    protected val defaultPrio = 7

    /** Final output file of this mode */
    def outputFile = new File(outputDir, namePrefix + ".freebayes.vcf.gz")

    def addJobs() {
      val fb = new nl.lumc.sasc.biopet.extensions.Freebayes(qscript)
      fb.bamfiles = inputBams
      fb.outputVcf = new File(outputDir, namePrefix + ".freebayes.vcf")
      fb.isIntermediate = true
      add(fb)

      //TODO: need piping for this, see also issue #114
      val bz = new Bgzip(qscript)
      bz.input = List(fb.outputVcf)
      bz.output = outputFile
      add(bz)

      val ti = new Tabix(qscript)
      ti.input = bz.output
      ti.p = Some("vcf")
      add(ti)
    }
  }

  /** default mode of bcftools */
  class Bcftools extends Variantcaller {
    val name = "bcftools"
    protected val defaultPrio = 8

    /** Final output file of this mode */
    def outputFile = new File(outputDir, namePrefix + ".bcftools.vcf.gz")

    def addJobs() {
      val mp = new SamtoolsMpileup(qscript)
      mp.input = inputBams
      mp.u = true

      val bt = new BcftoolsCall(qscript)
      bt.O = "z"
      bt.v = true
      bt.c = true

      //TODO: add proper class with piping support, see also issue #114
      add(new CommandLineFunction {
        @Input
        var input = inputBams

        @Output
        var output = outputFile

        def commandLine: String = mp.cmdPipe + " | " + bt.cmdPipeInput + " > " + outputFile + " && tabix -p vcf " + outputFile
      })
    }
  }

  /** Makes a vcf file from a mpileup without statistics */
  class RawVcf extends Variantcaller {
    val name = "raw"

    // This caller is designed as fallback when other variantcallers fails to report
    protected val defaultPrio = Int.MaxValue

    /** Final output file of this mode */
    def outputFile = new File(outputDir, namePrefix + ".raw.vcf.gz")

    def addJobs() {
      val rawFiles = inputBams.map(bamFile => {
        val m2v = new MpileupToVcf(qscript)
        m2v.inputBam = bamFile
        m2v.output = new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.vcf")
        add(m2v)

        val vcfFilter = new VcfFilter(qscript) {
          override def configName = "vcffilter"
          override def defaults = ConfigUtils.mergeMaps(Map("min_sample_depth" -> 8,
            "min_alternate_depth" -> 2,
            "min_samples_pass" -> 1,
            "filter_ref_calls" -> true
          ), super.defaults)
        }
        vcfFilter.inputVcf = m2v.output
        vcfFilter.outputVcf = new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.filter.vcf.gz")
        add(vcfFilter)
        vcfFilter.outputVcf
      })

      val cv = new CombineVariants(qscript)
      cv.inputFiles = rawFiles
      cv.outputFile = outputFile
      cv.setKey = "null"
      cv.excludeNonVariants = true
      add(cv)
    }
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, "ShivaVariantcalling.summary.json")

  /** Settings for the summary */
  def summarySettings = Map("variantcallers" -> configCallers.toList)

  /** Files for the summary */
  def summaryFiles: Map[String, File] = {
    val callers: Set[String] = config("variantcallers")
    callersList.filter(x => callers.contains(x.name)).map(x => (x.name -> x.outputFile)).toMap + ("final" -> finalFile)
  }
}