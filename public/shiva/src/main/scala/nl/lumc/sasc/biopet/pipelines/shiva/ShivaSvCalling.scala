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

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ PipelineCommand, BiopetQScript, Reference, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.breakdancer.Breakdancer
import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.extensions.delly.Delly
import nl.lumc.sasc.biopet.tools.VcfStats
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.Input
import scala.collection.JavaConversions._

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

  protected var inputBams: Map[String, File] = Map()

  def addBamFile(file: File, sampleId: Option[String] = None): Unit = {
    sampleId match {
      case Some(sample)        => inputBams += sample -> file
      case _ if !file.exists() => throw new IllegalArgumentException("Bam file does not exits: " + file)
      case _ => {
        val inputSam = SamReaderFactory.makeDefault.open(file)
        val samples = inputSam.getFileHeader.getReadGroups.map(_.getSample).distinct
        if (samples.size == 1) {
          inputBams += samples.head -> file
        } else throw new IllegalArgumentException("Bam contains multiple sample IDs: " + file)
      }
    }
  }

  /** Executed before script */
  def init(): Unit = {
    inputBamsArg.foreach(addBamFile(_))
  }

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("sv_callers", default = Set("breakdancer", "clever", "delly"))

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        BiopetQScript.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name))

    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty, "must select at least 1 SV caller, choices are: " + callersList.map(_.name).mkString(", "))

    callers.foreach(_.addJobs())

    addSummaryJobs()
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[SvCaller] = List(new Breakdancer, new Clever, new Delly)

  /** General trait for a variantcaller mode */
  trait SvCaller {
    /** Name of mode, this should also be used in the config */
    val name: String

    /** Output dir for this mode */
    def outputDir = new File(qscript.outputDir, name)

    /** This should add the variantcaller jobs */
    def addJobs()
  }

  /** default mode of freebayes */
  class Breakdancer extends SvCaller {
    val name = "breakdancer"

    def addJobs() {
      //TODO: move minipipeline of breakdancer to here
      for ((sample, bamFile) <- inputBams) {
        val breakdancerDir = new File(outputDir, sample)
        val breakdancer = Breakdancer(qscript, bamFile, breakdancerDir)
        addAll(breakdancer.functions)
      }
    }
  }

  /** default mode of bcftools */
  class Clever extends SvCaller {
    val name = "clever"

    def addJobs() {
      //TODO: check double directories
      for ((sample, bamFile) <- inputBams) {
        val cleverDir = new File(outputDir, sample)
        val clever = CleverCaller(qscript, bamFile, cleverDir, cleverDir)
        add(clever)
      }
    }
  }

  /** Makes a vcf file from a mpileup without statistics */
  class Delly extends SvCaller {
    val name = "delly"

    def addJobs() {
      //TODO: Move mini delly pipeline to here
      for ((sample, bamFile) <- inputBams) {
        val dellyDir = new File(outputDir, sample)
        val delly = Delly(qscript, bamFile, dellyDir)
        addAll(delly.functions)
      }
    }
  }

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