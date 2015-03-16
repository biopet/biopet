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

import java.io.{ BufferedWriter, File, FileWriter }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.reflect.io.Path

class FreeC(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Pileup file", required = true)
  var input: File = null

  var outputPath: File = null

  @Output(doc = "Output", shortName = "out")
  protected var output: File = _

  @Output(doc = "FreeC GC_profile")
  def gcprofile: File = {
    new File(outputPath, "GC_profile.cnp")
  }

  @Output(doc = "FreeC Read numbers per bin")
  def samplebins: File = {
    new File(outputPath, input.getName + "_sample.cpn")
  }

  executable = config("exe", default = "freec")

  var chrFiles: String = config("chrFiles")
  var chrLenFile: String = config("chrLenFile")
  var gemMappabilityFile: String = config("gemMappabilityFile")

  var ploidy: Option[Int] = config("ploidy", default = 2)
  var telocentromeric: Option[Int] = config("telocentromeric", default = 50000)
  // Default of 10k bins
  var window: Option[Int] = config("window", default = 10000)
  var snpfile: String = config("SNPfile")

  var samtools_exe: String = config(key = "exe", submodule = "samtools")

  //  FREEC v5.7(Control-FREEC v2.7) : calling copy number alterations and LOH regions using deep-sequencing data
  override val versionRegex = """Control-FREEC v(.*) :[.*]+""".r
  override val defaultThreads = 4
  override val defaultVmem = "4G"
  private var config_file: File = _

  /*
  * Output file from FreeC
  * */
  @Output()
  def CNVoutput: File = {
    new File(outputPath, input.getName + "_CNVs")
  }

  /*
  * Output file from FreeC
  * */
  @Output()
  def BAFoutput: File = {
    new File(outputPath, input.getName + "_BAF.txt")
  }

  /*
  * Output file from FreeC
  * */
  @Output()
  def RatioOutput: File = {
    new File(outputPath, input.getName + "_ratio.txt")
  }

  /*
  * Output file from FreeC
  * */
  @Output()
  def RatioBedGraph: File = {
    new File(outputPath, input.getName + "_ratio.BedGraph")
  }

  override def beforeGraph {
    super.beforeGraph
    config_file = new File(outputPath, input.getName + ".freec_config.txt")
    output = CNVoutput
  }

  override def freezeFieldValues(): Unit = {
    super.freezeFieldValues()
    logger.info("Creating directory for FREEC: " + outputPath.getAbsolutePath)
    outputPath.mkdirs()
    logger.info("Creating FREEC config file: " + config_file.getAbsolutePath)
    createConfigFile
  }

  /*
   *
   *
   */

  def createConfigFile = {
    val writer = new BufferedWriter(new FileWriter(config_file))

    // header
    writer.write("[general]\n")
    writer.write("BedGraphOutput=TRUE\n")
    writer.write("chrFiles=" + this.chrFiles + "\n")
    writer.write("chrLenFile=" + this.chrLenFile + "\n")
    writer.write("gemMappabilityFile=" + this.gemMappabilityFile + "\n")
    writer.write("maxThreads=" + this.nCoresRequest.getOrElse(defaultThreads) + "\n")
    writer.write("outputDir=" + this.outputPath.getAbsolutePath + "/\n")
    writer.write("ploidy=" + this.ploidy.getOrElse(2) + "\n")
    writer.write("samtools=" + this.samtools_exe + "\n")
    writer.write("telocentromeric=" + this.telocentromeric.getOrElse(50000) + "\n")
    writer.write("window=" + this.window.getOrElse(10000) + "\n")

    writer.write("[sample]\n")
    writer.write("mateFile=" + this.input + "\n")
    writer.write("inputFormat=pileup\n")
    // TODO: determine mateOrientation!
    // FR = Paired End Illumina
    // FF = SOLiD mate pairs
    // RF = Illumina mate-pairs
    // 0 = Single End
    writer.write("mateOrientation=FR\n")
    writer.write("[BAF]\n")
    writer.write("SNPfile=" + this.snpfile + "\n")

    writer.close()
  }

  def cmdLine = required(executable) +
    required("--conf", config_file)
}
