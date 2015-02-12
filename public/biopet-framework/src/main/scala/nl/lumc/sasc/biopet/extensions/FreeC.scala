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
package nl.lumc.sasc.biopet.extensions

import java.io.{ FileWriter, BufferedWriter, File, PrintWriter }

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable


class FreeC(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Bam file", required = false)
  var bamFile: File = _

  @Output(doc = "Output", shortName = "out")
  protected var output: File = _

  executable = config("exe", default = "freec")

  var chrFiles: String = config("chrFiles")
  var chrLenFile: String = config("chrLenFile")
  var gemMappabilityFile: String = config("gemMappabilityFile")

  var ploidy: Option[Int] = config("ploidy", default=2)
  var telocentromeric: Option[Int] = config("telocentromeric", default=50000)
  // Default of 10k bins
  var window: Option[Int] = config("window", default=10000)


  //  FREEC v5.7(Control-FREEC v2.7) : calling copy number alterations and LOH regions using deep-sequencing data
  override val versionRegex = """FREEC v(.*)\(""".r
  override def versionCommand = executable + " --version"
  override val defaultThreads = 4
  private var config_file: File = _

  override def afterGraph {
    this.checkExecutable
    config_file = new File(output.getParent + File.separator + output.getName + ".freec_config.txt"  )
    this.output = new File(this.bamFile.getCanonicalPath + "_CNVs")
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
    writer.write("chrFiles="+ this.chrFiles +"\n")
    writer.write("chrLenFile="+config("chrLenFile")+"\n")
    writer.write("gemMappabilityFile="+config("gemMappabilityFile")+"\n")
    writer.write("maxThreads="+ this.nCoresRequest +"\n")
    writer.write("outputDir="+ this.output.getParent +"/\n")
    writer.write("ploidy="+ this.ploidy +"\n")
    writer.write("samtools="+ config(key="exe", submodule="samtools" ) +"\n")
    writer.write("telocentromeric="+ this.telocentromeric +"\n")
    writer.write("window="+ this.window +"\n")

    writer.write("[sample]")
    writer.write("mateFile="+this.bamFile+"\n")
    writer.write("inputFormat=bam\n")
    // TODO: determine mateOrientation!
    // FR = Paired End Illumina
    // FF = SOLiD mate pairs
    // RF = Illumina mate-pairs
    // 0 = Single End
    writer.write("mateOrientation=FR\n")

    writer.close()
  }

  def cmdLine = required(executable) +
    required("--conf", config_file)
}
