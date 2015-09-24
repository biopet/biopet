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
/*
 * Structural variation calling
 */

package nl.lumc.sasc.biopet.pipelines.yamsvp

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.breakdancer.Breakdancer
import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.extensions.igvtools.IGVToolsCount
import nl.lumc.sasc.biopet.extensions.sambamba.{ SambambaMarkdup, SambambaMerge }
//import nl.lumc.sasc.biopet.extensions.pindel.Pindel
import nl.lumc.sasc.biopet.extensions.delly.Delly
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.engine.JobRunInfo
import org.broadinstitute.gatk.queue.function._

class Yamsvp(val root: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)
  def summaryFile = null
  def summaryFiles = Map()
  def summarySettings = Map()

  var reference: File = config("reference")

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    def summaryFiles = Map()
    def summaryStats = Map()

    val alignmentDir: String = sampleDir + "alignment/"
    val svcallingDir: String = sampleDir + "svcalls/"

    def makeLibrary(id: String) = new Library(id)
    class Library(libraryId: String) extends AbstractLibrary(libraryId) {
      //      val runDir: String = alignmentDir + "run_" + libraryId + "/"
      def summaryFiles = Map()
      def summaryStats = Map()

      val mapping = new Mapping(qscript)
      mapping.libId = Some(libraryId)
      mapping.sampleId = Some(sampleId)

      protected def addJobs(): Unit = {
        mapping.input_R1 = config("R1")
        mapping.input_R2 = config("R2")
        mapping.outputDir = libDir

        mapping.init()
        mapping.biopetScript()
        qscript.addAll(mapping.functions)
      }
    }
    protected def addJobs(): Unit = {
      val libraryBamfiles = libraries.map(_._2.mapping.finalBamFile).toList

      val bamFile: File = if (libraryBamfiles.size == 1) {
        val alignmentlink = Ln(qscript, libraryBamfiles.head,
          alignmentDir + sampleId + ".merged.bam", relative = true)
        alignmentlink.isIntermediate = true
        add(alignmentlink)
        alignmentlink.output
      } else if (libraryBamfiles.size > 1) {
        val mergeSamFiles = new SambambaMerge(qscript)
        mergeSamFiles.input = libraryBamfiles
        mergeSamFiles.output = sampleDir + sampleId + ".merged.bam"
        mergeSamFiles.isIntermediate = true
        add(mergeSamFiles)
        mergeSamFiles.output
      } else null

      val bamMarkDup = SambambaMarkdup(qscript, bamFile)
      add(bamMarkDup)

      addAll(BamMetrics(qscript, bamMarkDup.output, alignmentDir + "metrics" + File.separator).functions)

      // create an IGV TDF file
      val tdfCount = IGVToolsCount(qscript, bamMarkDup.output, config("genome_name", default = "hg19"))
      add(tdfCount)

      /// bamfile will be used as input for the SV callers. First run Clever
      //    val cleverVCF : File = sampleDir + "/" + sampleID + ".clever.vcf"

      val cleverDir = svcallingDir + sampleId + ".clever/"
      val clever = CleverCaller(qscript, bamMarkDup.output, qscript.reference, svcallingDir, cleverDir)
      add(clever)

      val clever_vcf = Ln(qscript, clever.outputvcf, svcallingDir + sampleId + ".clever.vcf", relative = true)
      add(clever_vcf)

      val breakdancerDir = svcallingDir + sampleId + ".breakdancer/"
      val breakdancer = Breakdancer(qscript, bamMarkDup.output, qscript.reference, breakdancerDir)
      addAll(breakdancer.functions)

      val bd_vcf = Ln(qscript, breakdancer.outputvcf, svcallingDir + sampleId + ".breakdancer.vcf", relative = true)
      add(bd_vcf)

      val dellyDir = svcallingDir + sampleId + ".delly/"
      val delly = Delly(qscript, bamMarkDup.output, dellyDir)
      addAll(delly.functions)

      val delly_vcf = Ln(qscript, delly.outputvcf, svcallingDir + sampleId + ".delly.vcf", relative = true)
      add(delly_vcf)

      // for pindel we should use per library config collected into one config file
      //    val pindelDir = svcallingDir + sampleID + ".pindel/"
      //    val pindel = Pindel(qscript, analysisBam, this.reference, pindelDir)
      //    sampleOutput.vcf += ("pindel" -> List(pindel.outputvcf))
      //    addAll(pindel.functions)
      //
      //    val pindel_vcf = Ln(qscript, pindel.outputvcf, svcallingDir + sampleID + ".pindel.vcf", relative = true)
      //    add(pindel_vcf)
      //
    }
  }

  def addMultiSampleJobs() = {}

  def init() {
  }

  def biopetScript() {
    logger.info("Starting YAM SV Pipeline")
    addSamplesJobs()
  }

  override def onExecutionDone(jobs: Map[QFunction, JobRunInfo], success: Boolean) {
    logger.info("YAM SV Pipeline has run .......................")
  }
}

object Yamsvp extends PipelineCommand