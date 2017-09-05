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
package nl.lumc.sasc.biopet.extensions.freec

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline._

import scala.util.matching.Regex

class FreeC(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  override def defaults = Map("max_walltime_limit" -> 7200)

  @Input(doc = "BAMfile", required = true)
  var input: File = _

  var inputFormat: Option[String] = config("inputFormat")

  var outputPath: File = _

  @Output(doc = "Output", shortName = "out")
  protected var output: File = _

  @Output(doc = "FreeC GC_profile")
  private var _gcProfile: File = _
  def gcProfile = new File(outputPath, "GC_profile.cnp")

  @Output(doc = "FreeC Read numbers per bin")
  private var _sampleBins: File = _
  def sampleBins = new File(outputPath, input.getName + "_sample.cpn")

  @Output
  private var _cnvOutput: File = _
  def cnvOutput = new File(outputPath, input.getName + "_CNVs")

  @Output
  private var _bafOutput: File = _
  def bafOutput = new File(outputPath, input.getName + "_BAF.txt")

  @Output
  private var _ratioOutput: File = _
  def ratioOutput = new File(outputPath, input.getName + "_ratio.txt")

  @Output
  private var _ratioBedGraph: File = _
  def ratioBedGraph = new File(outputPath, input.getName + "_ratio.BedGraph")

  executable = config("exe", default = "freec", freeVar = false)
  var bedGraphOutput: Boolean = config("BedGraphOutput", default = false)
  var bedtools: Option[File] =
    config("exe", default = "bedtools", namespace = "bedtools", freeVar = false)
  var breakPointThreshold: Option[Double] = config("breakPointThreshold")
  var breakPointType: Option[Int] = config("breakPointType")

  var chrFiles: File = config("chrFiles")
  var chrLenFile: File = config("chrLenFile")

  var coefficientOfVariation: Option[Double] = config("coefficientOfVariation")
  var contamination: Option[Double] = config("contamination")
  var contaminationAdjustment: Boolean = config("contaminationAdjustment", default = false)

  var degree: Option[String] = config("degree")
  var forceGCcontentNormalization: Option[Int] = config("forceGCcontentNormalization")

  var gcContentProfile: Option[File] = config("GCcontentProfile")
  var gemMappabilityFile: Option[String] = config("gemMappabilityFile")

  var intercept: Option[Int] = config("intercept")
  var minCNAlength: Option[Int] = config("minCNAlength")
  var minMappabilityPerWindow: Option[Double] = config("minMappabilityPerWindow")
  var minExpectedGC: Option[Double] = config("minExpectedGC")
  var maxExpectedGC: Option[Double] = config("maxExpectedGC")
  var minimalSubclonePresence: Option[Int] = config("minimalSubclonePresence")

  var noisyData: Boolean = config("noisyData", default = false)
  //var outputDir: File
  var ploidy: Option[String] = config("ploidy")
  var printNA: Boolean = config("printNA", default = false)
  var readCountThreshold: Option[Int] = config("readCountThreshold")

  var sambamba: File = config("exe", namespace = "sambamba", default = "sambamba", freeVar = false)
  var sambambaThreads: Option[Int] = config("SambambaThreads")

  var samtools: File = config("exe", namespace = "samtools", default = "samtools", freeVar = false)

  var sex: Option[String] = config("sex")
  var step: Option[Int] = config("step")
  var telocentromeric: Option[Int] = config("telocentromeric")

  var uniqueMatch: Boolean = config("uniqueMatch", default = false)
  var window: Option[Int] = config("window")

  /** [sample] options */
  //  var mateFile: File = input
  var mateCopyNumberFile: Option[File] = config("mateCopyNumberFile")
  //  var inputFormat: Option[String] = config("inputFormat")
  var mateOrientation: Option[String] = config("mateOrientation")

  /** [BAF] options */
  var snpFile: Option[File] = config("snpFile")
  var minimalCoveragePerPosition: Option[Int] = config("minimalCoveragePerPosition")
  var makePileup: Option[File] = config("makePileup")
  var fastaFile: Option[File] = config("fastaFile")
  var minimalQualityPerPosition: Option[Int] = config("minimalQualityPerPosition")
  var shiftInQuality: Option[Int] = config("shiftInQuality")

  /** [target] */
  var captureRegions: Option[File] = config("captureRegions")

  // Control-FREEC v8.7 : calling copy number alterations and LOH regions using deep-sequencing data
  override def versionCommand: String = executable
  override def versionRegex: List[Regex] = """Control-FREEC v([0-9\.]+) : .*""".r :: Nil
  override def defaultThreads = 4
  override def defaultCoreMemory = 50

  private var configFile: File = _

  override def beforeGraph() {
    super.beforeGraph()

    _gcProfile = gcProfile
    _sampleBins = sampleBins
    _cnvOutput = cnvOutput
    _bafOutput = bafOutput
    _ratioOutput = ratioOutput
    _ratioBedGraph = ratioBedGraph

    configFile = new File(outputPath, input.getName + ".freec_config.txt")
    output = cnvOutput
  }

  override def beforeCmd(): Unit = {
    super.beforeCmd()

    outputPath.mkdirs()

    logger.info("Creating FREEC config file: " + configFile.getAbsolutePath)
    createConfigFile
  }

  protected def createConfigFile(): Unit = {
    val writer = new PrintWriter(configFile)

    val conf: String = "[general]" + "\n" +
      conditional(bedGraphOutput, "BedGraphOutput=TRUE", escape = false) + "\n" +
      required("bedtools=", bedtools, spaceSeparated = false, escape = false) + "\n" +
      optional("breakPointThreshold=", breakPointThreshold, spaceSeparated = false, escape = false) + "\n" +
      optional("breakPointType=", breakPointType, spaceSeparated = false, escape = false) + "\n" +
      required("chrFiles=", chrFiles, spaceSeparated = false, escape = false) + "\n" +
      required("chrLenFile=", chrLenFile, spaceSeparated = false, escape = false) + "\n" +
      optional("coefficientOfVariation=",
               coefficientOfVariation,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("contamination=", contamination, spaceSeparated = false, escape = false) + "\n" +
      conditional(contaminationAdjustment, "contaminationAdjustment=TRUE", escape = false) + "\n" +
      optional("degree=", degree, spaceSeparated = false, escape = false) + "\n" +
      optional("forceGCcontentNormalization=",
               forceGCcontentNormalization,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("GCcontentProfile=", gcContentProfile, spaceSeparated = false, escape = false) + "\n" +
      optional("gemMappabilityFile=", gemMappabilityFile, spaceSeparated = false, escape = false) + "\n" +
      optional("intercept=", intercept, spaceSeparated = false, escape = false) + "\n" +
      optional("minCNAlength=", minCNAlength, spaceSeparated = false, escape = false) + "\n" +
      optional("minMappabilityPerWindow=",
               minMappabilityPerWindow,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("minExpectedGC=", minExpectedGC, spaceSeparated = false, escape = false) + "\n" +
      optional("maxExpectedGC=", maxExpectedGC, spaceSeparated = false, escape = false) + "\n" +
      optional("minimalSubclonePresence=",
               minimalSubclonePresence,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("maxThreads=", getThreads, spaceSeparated = false, escape = false) + "\n" +
      conditional(noisyData, "noisyData=TRUE", escape = false) + "\n" +
      required("outputDir=", outputPath, spaceSeparated = false, escape = false) + "\n" +
      optional("ploidy=", ploidy, spaceSeparated = false, escape = false) + "\n" +
      conditional(printNA, "printNA=TRUE", escape = false) + "\n" +
      optional("readCountThreshold=", readCountThreshold, spaceSeparated = false, escape = false) + "\n" +
      required("sambamba=", sambamba, spaceSeparated = false, escape = false) + "\n" +
      optional("SambambaThreads=", sambambaThreads, spaceSeparated = false, escape = false) + "\n" +
      required("samtools=", samtools, spaceSeparated = false, escape = false) + "\n" +
      optional("sex=", sex, spaceSeparated = false, escape = false) + "\n" +
      optional("step=", step, spaceSeparated = false, escape = false) + "\n" +
      optional("telocentromeric=", telocentromeric, spaceSeparated = false, escape = false) + "\n" +
      conditional(uniqueMatch, "uniqueMatch=TRUE", escape = false) + "\n" +
      optional("window=", window, spaceSeparated = false, escape = false) + "\n" +
      "[sample]" + "\n" +
      required("mateFile=", input, spaceSeparated = false, escape = false) + "\n" +
      optional("mateCopyNumberFile=", mateCopyNumberFile, spaceSeparated = false, escape = false) + "\n" +
      required("inputFormat=", inputFormat, spaceSeparated = false, escape = false) + "\n" +
      required("mateOrientation=", mateOrientation, spaceSeparated = false, escape = false) + "\n" +
      "[BAF]" + "\n" +
      optional("SNPfile=", snpFile, spaceSeparated = false, escape = false) + "\n" +
      optional("minimalCoveragePerPosition=",
               minimalCoveragePerPosition,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("makePileup=", makePileup, spaceSeparated = false, escape = false) + "\n" +
      optional("fastaFile=", fastaFile, spaceSeparated = false, escape = false) + "\n" +
      optional("minimalQualityPerPosition=",
               minimalQualityPerPosition,
               spaceSeparated = false,
               escape = false) + "\n" +
      optional("shiftInQuality=", shiftInQuality, spaceSeparated = false, escape = false) + "\n" +
      "[target]" + "\n" +
      optional("captureRegions=", captureRegions, spaceSeparated = false, escape = false) + "\n"

    writer.write(conf)
    writer.close()
  }

  def cmdLine: String =
    required(executable) +
      required("--conf", configFile)
}
