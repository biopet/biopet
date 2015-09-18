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
package nl.lumc.sasc.biopet.extensions.freec

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class FreeC(val root: Configurable) extends BiopetCommandLineFunction with Reference {

  @Input(doc = "Pileup file", required = true)
  var input: File = null

  var outputPath: File = null

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

  executable = config("exe", default = "freec")

  var chrFiles: String = config("chrFiles")
  var chrLenFile: String = config("chrLenFile")
  var gemMappabilityFile: String = config("gemMappabilityFile")

  var ploidy: Option[Int] = config("ploidy", default = 2)
  var telocentromeric: Option[Int] = config("telocentromeric", default = 50000)
  // Default of 10k bins
  var window: Option[Int] = config("window", default = 10000)
  var snpFile: Option[String] = config("snpFile")

  var samtoolsExe: String = config(key = "exe", submodule = "samtools")

  //  FREEC v5.7(Control-FREEC v2.7) : calling copy number alterations and LOH regions using deep-sequencing data
  override def versionRegex = """Control-FREEC v(.*) :[.*]+""".r
  override def defaultThreads = 4
  override def defaultCoreMemory = 4.0

  private var configFile: File = _

  override def beforeGraph {
    super.beforeGraph

    _gcProfile = gcProfile
    _sampleBins = sampleBins
    _cnvOutput = cnvOutput
    _bafOutput = bafOutput
    _ratioOutput = ratioOutput
    _ratioBedGraph = ratioBedGraph

    configFile = new File(outputPath, input.getName + ".freec_config.txt")
    output = cnvOutput
  }

  override def beforeCmd: Unit = {
    super.beforeCmd

    outputPath.mkdirs()

    logger.info("Creating FREEC config file: " + configFile.getAbsolutePath)
    createConfigFile
  }

  protected def createConfigFile = {
    val writer = new PrintWriter(configFile)

    // header
    writer.println("[general]")
    writer.println("BedGraphOutput=TRUE")
    writer.println("chrFiles=" + chrFiles)
    writer.println("chrLenFile=" + chrLenFile)
    writer.println("gemMappabilityFile=" + gemMappabilityFile)
    writer.println("maxThreads=" + nCoresRequest.getOrElse(defaultThreads))
    writer.println("outputDir=" + outputPath.getAbsolutePath)
    writer.println("ploidy=" + ploidy.getOrElse(2))
    writer.println("samtools=" + samtoolsExe)
    writer.println("telocentromeric=" + telocentromeric.getOrElse(50000))
    writer.println("window=" + window.getOrElse(10000))

    writer.println("[sample]")
    writer.println("mateFile=" + this.input + "")
    writer.println("inputFormat=pileup")
    // TODO: determine mateOrientation!
    // FR = Paired End Illumina
    // FF = SOLiD mate pairs
    // RF = Illumina mate-pairs
    // 0 = Single End
    writer.println("mateOrientation=FR")
    writer.println("[BAF]")
    snpFile.foreach(x => writer.println("SNPfile=" + x))

    writer.close()
  }

  def cmdLine = required(executable) +
    required("--conf", configFile)
}
