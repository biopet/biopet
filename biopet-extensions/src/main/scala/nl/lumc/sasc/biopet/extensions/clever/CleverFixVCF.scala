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
package nl.lumc.sasc.biopet.extensions.clever

/**
  * Created by wyleung on 4-4-16.
  */
import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

import scala.io.Source

class CleverFixVCF(val parent: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName
  @Input(doc = "Input Clever VCF")
  var input: File = _

  @Output(doc = "Output fixed VCF")
  var output: File = _

  @Argument(doc = "Samplename")
  var sampleName: String = _

  override def defaultCoreMemory = 4.0

  override def cmdLine: String =
    super.cmdLine +
      required("-i", input) +
      required("-o", output) +
      required("-s", sampleName)
}

object CleverFixVCF extends ToolCommand {
  case class Args(inputVCF: File = null, sampleLabel: String = "", outputVCF: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('i', "inputvcf") required () valueName "<vcffile/path>" action { (x, c) =>
      c.copy(inputVCF = x)
    } text "Please specify the input Clever VCF file"
    opt[String]('s', "samplelabel") valueName "<sample label>" action { (x, c) =>
      c.copy(sampleLabel = x)
    } text "Sample label is missing"
    opt[File]('o', "outputvcf") valueName "<output>" action { (x, c) =>
      c.copy(outputVCF = x)
    } text "Output path is missing"
  }

  def replaceHeaderLine(inHeaderLine: String,
                        toCheckFor: String,
                        replacement: String,
                        extraHeader: String): String = {
    if (inHeaderLine == toCheckFor) {
      extraHeader + "\n" + replacement + "\n"
    } else {
      // We have to deal with matching records
      // these don't start with #

      if (inHeaderLine.startsWith("#")) {
        inHeaderLine + "\n"
      } else {
        // this should be a record
        // Ensure the REF field is at least an N
        val cols = inHeaderLine.split("\t")
        cols(3) = "N"
        cols.mkString("\t") + "\n"
      }
    }
  }

  val extraHeader =
    """##INFO=<ID=NS,Number=1,Type=Integer,Description="Number of Samples With Data">
##INFO=<ID=DP,Number=1,Type=Integer,Description="Total Depth">
##INFO=<ID=AF,Number=A,Type=Float,Description="Allele Frequency">
##INFO=<ID=IMPRECISE,Number=0,Type=Flag,Description="Imprecise structural variation">
##INFO=<ID=NOVEL,Number=0,Type=Flag,Description="Indicates a novel structural variation">
##INFO=<ID=SVEND,Number=1,Type=Integer,Description="End position of the variant described in this record">
##INFO=<ID=END,Number=1,Type=Integer,Description="End position of the variant described in this record">
##INFO=<ID=SVTYPE,Number=1,Type=String,Description="Type of structural variant">
##INFO=<ID=SVLEN,Number=.,Type=Integer,Description="Difference in length between REF and ALT alleles">
##INFO=<ID=CIPOS,Number=2,Type=Integer,Description="Confidence interval around POS for imprecise variants">
##INFO=<ID=CIEND,Number=2,Type=Integer,Description="Confidence interval around END for imprecise variants">
##INFO=<ID=HOMLEN,Number=.,Type=Integer,Description="Length of base pair identical micro-homology at event breakpoints">
##INFO=<ID=HOMSEQ,Number=.,Type=String,Description="Sequence of base pair identical micro-homology at event breakpoints">
##INFO=<ID=BKPTID,Number=.,Type=String,Description="ID of the assembled alternate allele in the assembly file">
##INFO=<ID=MEINFO,Number=4,Type=String,Description="Mobile element info of the form NAME,START,END,POLARITY">
##INFO=<ID=METRANS,Number=4,Type=String,Description="Mobile element transduction info of the form CHR,START,END,POLARITY">
##INFO=<ID=DGVID,Number=1,Type=String,Description="ID of this element in Database of Genomic Variation">
##INFO=<ID=DBVARID,Number=1,Type=String,Description="ID of this element in DBVAR">
##INFO=<ID=DBRIPID,Number=1,Type=String,Description="ID of this element in DBRIP">
##INFO=<ID=MATEID,Number=.,Type=String,Description="ID of mate breakends">
##INFO=<ID=PARID,Number=1,Type=String,Description="ID of partner breakend">
##INFO=<ID=EVENT,Number=1,Type=String,Description="ID of event associated to breakend">
##INFO=<ID=BPWINDOW,Number=2,Type=Integer,Description="Window of breakpoints">
##INFO=<ID=CILEN,Number=2,Type=Integer,Description="Confidence interval around the inserted material between breakends">
##INFO=<ID=DP,Number=1,Type=Integer,Description="Read Depth of segment containing breakend">
##INFO=<ID=DPADJ,Number=.,Type=Integer,Description="Read Depth of adjacency">
##INFO=<ID=CN,Number=1,Type=Integer,Description="Copy number of segment containing breakend">
##INFO=<ID=CNADJ,Number=.,Type=Integer,Description="Copy number of adjacency">
##INFO=<ID=ESUPPORT,Number=1,Type=Float,Description="Support of event, see into clever python script for more: scripts/postprocess-predictions">
##INFO=<ID=CICN,Number=2,Type=Integer,Description="Confidence interval around copy number for the segment">
##INFO=<ID=CICNADJ,Number=.,Type=Integer,Description="Confidence interval around copy number for the adjacency">
##FORMAT=<ID=CN,Number=1,Type=Integer,Description="Copy number genotype for imprecise events">
##FORMAT=<ID=CNQ,Number=1,Type=Float,Description="Copy number genotype quality for imprecise events">
##FORMAT=<ID=CNL,Number=.,Type=Float,Description="Copy number genotype likelihood for imprecise events">
##FORMAT=<ID=NQ,Number=1,Type=Integer,Description="Phred style probability score that the variant is novel">
##FORMAT=<ID=HAP,Number=1,Type=Integer,Description="Unique haplotype identifier">
##FORMAT=<ID=AHAP,Number=1,Type=Integer,Description="Unique identifier of ancestral haplotype">
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##FORMAT=<ID=GQ,Number=1,Type=Integer,Description="Genotype Quality">
##FORMAT=<ID=DP,Number=1,Type=Integer,Description="Read Depth">"""

  val vcfColHeader = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tdefault"

  val vcfColReplacementHeader = s"#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t"

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val input: File = commandArgs.inputVCF
    val output: File = commandArgs.outputVCF

    val inputVCF = Source.fromFile(input)
    val writer = new PrintWriter(output)
    inputVCF
      .getLines()
      .foreach(
        x =>
          writer.write(
            replaceHeaderLine(x,
                              vcfColHeader,
                              vcfColReplacementHeader + commandArgs.sampleLabel,
                              extraHeader)))
    writer.close()
    inputVCF.close()
  }
}
