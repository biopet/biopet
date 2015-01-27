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
import nl.lumc.sasc.biopet.core.{ BiopetQScript, MultiSampleQScript, PipelineCommand }

import nl.lumc.sasc.biopet.extensions.sambamba.{ SambambaIndex, SambambaMerge }
import nl.lumc.sasc.biopet.extensions.svcallers.pindel.Pindel
import nl.lumc.sasc.biopet.extensions.svcallers.{ Breakdancer, Clever }

import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.queue.engine.JobRunInfo

class Yamsvp(val root: Configurable) extends QScript with BiopetQScript { //with MultiSampleQScript {
  def this() = this(null)

  var reference: File = config("reference", required = true)
  var finalBamFiles: List[File] = Nil
  /*
  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
  }

  class SampleOutput extends AbstractSampleOutput {
    var vcf: Map[String, List[File]] = Map()
    var mappedBamFile: File = _
  }
*/
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
    //runSamplesJobs
    //

  }

  override def onExecutionDone(jobs: Map[QFunction, JobRunInfo], success: Boolean) {
    logger.info("YAM SV Pipeline has run .......................")
  }
  /*
  def runSingleSampleJobs(sampleID: String): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var outputFiles: Map[String, List[File]] = Map()
    var libraryFastqFiles: List[File] = List()
    val sampleDir: String = outputDir + sampleID + "/"
    val alignmentDir: String = sampleDir + "alignment/"

    val svcallingDir: String = sampleDir + "svcalls/"

    sampleOutput.libraries = runLibraryJobs(sampleID)
    for ((libraryID, libraryOutput) <- sampleOutput.libraries) {
      // this is extending the libraryBamfiles list like '~=' in D or .append in Python or .push_back in C++
      libraryBamfiles ++= List(libraryOutput.mappedBamFile)
    }

    val bamFile: File =
      if (libraryBamfiles.size == 1) libraryBamfiles.head
      else if (libraryBamfiles.size > 1) {
        val mergeSamFiles = new SambambaMerge(root)
        mergeSamFiles.input = libraryBamfiles
        mergeSamFiles.output = alignmentDir + sampleID + ".merged.bam"
        add(mergeSamFiles)
        mergeSamFiles.output
      } else null

    val bamIndex = SambambaIndex(root, bamFile)
    add(bamIndex)

    /// bamfile will be used as input for the SV callers. First run Clever
    //    val cleverVCF : File = sampleDir + "/" + sampleID + ".clever.vcf"

    val cleverDir = svcallingDir + sampleID + ".clever/"
    val clever = Clever(this, bamFile, this.reference, svcallingDir, cleverDir)
    clever.deps = List(bamIndex.output)
    sampleOutput.vcf += ("clever" -> List(clever.outputvcf))
    add(clever)

    val breakdancerDir = svcallingDir + sampleID + ".breakdancer/"
    val breakdancer = Breakdancer(this, bamFile, this.reference, breakdancerDir)
    sampleOutput.vcf += ("breakdancer" -> List(breakdancer.outputvcf))
    addAll(breakdancer.functions)

    // for pindel we should use per library config collected into one config file
    //    val pindelDir = svcallingDir + sampleID + ".pindel/"
    //    val pindel = Pindel(this, bamFile, this.reference, pindelDir)
    //    sampleOutput.vcf += ("pindel" -> List(pindel.outputvcf))
    //    addAll(pindel.functions)
    //
    //    
    return sampleOutput
  }

  // Called for each run from a sample

  def runSingleLibraryJobs(libraryId: String, sampleID: String): LibraryOutput = {
    val libraryOutput = new LibraryOutput

    val alignmentDir: String = outputDir + sampleID + "/alignment/"
    val runDir: String = alignmentDir + "run_" + libraryId + "/"

    if (config.contains("R1")) {
      val mapping = new Mapping(this)

      mapping.aligner = config("aligner", default = "stampy")
      mapping.skipFlexiprep = false
      mapping.skipMarkduplicates = true // we do the dedup marking using Sambamba

      mapping.input_R1 = config("R1")
      mapping.input_R2 = config("R2")
      mapping.paired = (mapping.input_R2 != null)
      mapping.RGLB = libraryId
      mapping.RGSM = sampleID
      mapping.RGPL = config("PL")
      mapping.RGPU = config("PU")
      mapping.RGCN = config("CN")
      mapping.outputDir = runDir

      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)

      // start sambamba dedup

      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for library: " + libraryId)
    return libraryOutput
    //    logger.debug(outputFiles)
    //    return outputFiles
  }
  */
}

object Yamsvp extends PipelineCommand