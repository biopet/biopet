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

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.sambamba.{ SambambaIndex, SambambaMerge, SambambaMarkdup }
import nl.lumc.sasc.biopet.extensions.svcallers.pindel.Pindel
import nl.lumc.sasc.biopet.extensions.svcallers.{ Breakdancer, Delly, CleverCaller }

import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.queue.engine.JobRunInfo

class Yamsvp(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  var reference: File = config("reference", required = true)
  var finalBamFiles: List[File] = Nil

  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
  }

  class SampleOutput extends AbstractSampleOutput {
    var vcf: Map[String, List[File]] = Map()
    var mappedBamFile: File = _
  }

  override def init() {
    if (outputDir == null)
      throw new IllegalStateException("Output directory is not specified in the config / argument")
    else if (!outputDir.endsWith("/"))
      outputDir += "/"
  }

  def biopetScript() {
    // write the pipeline here
    // start with QC, alignment, call sambamba, call sv callers, reporting

    // read config and set all parameters for the pipeline
    logger.info("Starting YAM SV Pipeline")
    runSamplesJobs
    //

  }

  override def onExecutionDone(jobs: Map[QFunction, JobRunInfo], success: Boolean) {
    logger.info("YAM SV Pipeline has run .......................")
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var outputFiles: Map[String, List[File]] = Map()
    var libraryFastqFiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir: String = outputDir + sampleID + "/"
    val alignmentDir: String = sampleDir + "alignment/"

    val svcallingDir: String = sampleDir + "svcalls/"

    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    for ((libraryID, libraryOutput) <- sampleOutput.libraries) {
      // this is extending the libraryBamfiles list like '~=' in D or .append in Python or .push_back in C++
      libraryBamfiles ++= List(libraryOutput.mappedBamFile)
    }

    val bamFile: File =
      if (libraryBamfiles.size == 1) {
        // When the sample has only 1 run, make a link in the main alignment directory
        val alignmentlink = Ln(root, libraryBamfiles.head,
          alignmentDir + sampleID + ".merged.bam", true)
        add(alignmentlink, isIntermediate = true)
        alignmentlink.out
      } else if (libraryBamfiles.size > 1) {
        val mergeSamFiles = new SambambaMerge(root)
        mergeSamFiles.input = libraryBamfiles
        mergeSamFiles.output = alignmentDir + sampleID + ".merged.bam"
        add(mergeSamFiles, isIntermediate = true)
        mergeSamFiles.output
      } else null

    val bamMarkDup = SambambaMarkdup(root, bamFile)
    add(bamMarkDup)

    /// bamfile will be used as input for the SV callers. First run Clever
    //    val cleverVCF : File = sampleDir + "/" + sampleID + ".clever.vcf"

    val cleverDir = svcallingDir + sampleID + ".clever/"
    val clever = CleverCaller(this, bamMarkDup.output, this.reference, svcallingDir, cleverDir)
    sampleOutput.vcf += ("clever" -> List(clever.outputvcf))
    add(clever)

    val clever_vcf = Ln(this, clever.outputvcf, svcallingDir + sampleID + ".clever.vcf", relative = true)
    add(clever_vcf)

    val breakdancerDir = svcallingDir + sampleID + ".breakdancer/"
    val breakdancer = Breakdancer(this, bamMarkDup.output, this.reference, breakdancerDir)
    sampleOutput.vcf += ("breakdancer" -> List(breakdancer.outputvcf))
    addAll(breakdancer.functions)

    val bd_vcf = Ln(this, breakdancer.outputvcf, svcallingDir + sampleID + ".breakdancer.vcf", relative = true)
    add(bd_vcf)

    val dellyDir = svcallingDir + sampleID + ".delly/"
    val delly = Delly(this, bamMarkDup.output, dellyDir)
    sampleOutput.vcf += ("delly" -> List(delly.outputvcf))
    addAll(delly.functions)

    val delly_vcf = Ln(this, delly.outputvcf, svcallingDir + sampleID + ".delly.vcf", relative = true)
    add(delly_vcf)

    // for pindel we should use per library config collected into one config file
    //    val pindelDir = svcallingDir + sampleID + ".pindel/"
    //    val pindel = Pindel(this, analysisBam, this.reference, pindelDir)
    //    sampleOutput.vcf += ("pindel" -> List(pindel.outputvcf))
    //    addAll(pindel.functions)
    //
    //    val pindel_vcf = Ln(this, pindel.outputvcf, svcallingDir + sampleID + ".pindel.vcf", relative = true)
    //    add(pindel_vcf)
    //
    return sampleOutput
  }

  // Called for each run from a sample

  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput

    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val alignmentDir: String = outputDir + sampleID + "/alignment/"
    val runDir: String = alignmentDir + "run_" + runID + "/"

    if (runConfig.contains("R1")) {
      val mapping = new Mapping(this)

      // TODO: check and test config[aligner] in json
      // yamsvp/aligner -> value
      // this setting causes error if not defined?
      mapping.aligner = config("aligner", default = "bwa")
      mapping.skipFlexiprep = false
      mapping.skipMarkduplicates = true // we do the dedup marking using Sambamba

      if (runConfig.contains("R1")) mapping.input_R1 = new File(runConfig("R1").toString)
      if (runConfig.contains("R2")) mapping.input_R2 = new File(runConfig("R2").toString)
      mapping.paired = (mapping.input_R2 != null)
      mapping.RGLB = runConfig("ID").toString
      mapping.RGSM = sampleConfig("ID").toString
      if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
      if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
      if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
      mapping.outputDir = runDir

      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)

      // start sambamba dedup

      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return libraryOutput
    //    logger.debug(outputFiles)
    //    return outputFiles
  }
}

object Yamsvp extends PipelineCommand