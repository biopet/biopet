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
package nl.lumc.sasc.biopet.extensions.pindel
import java.io.{File, PrintWriter}

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{BamUtils, ToolCommand}
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

class PindelConfig(val parent: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName
  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output Config file")
  var output: File = _

  @Argument(doc = "Insertsize")
  var insertSize: Int = 0

  var sampleName: String = _

  override def cmdLine =
    super.cmdLine +
      required("-i", input) +
      required("-n", sampleName) + { if (insertSize == 0) "" else s" -s $insertSize " } +
      required("-o", output)
}

object PindelConfig extends ToolCommand {
  case class Args(inputBam: File = null,
                  sampleLabel: Option[String] = None,
                  insertSize: Option[Int] = None,
                  output: Option[File] = None)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "inputbam") required () valueName "<bamfile/path>" action { (x, c) =>
      c.copy(inputBam = x)
    } text "Please specify the input bam file"
    opt[String]('n', "samplelabel") valueName "<sample label>" action { (x, c) =>
      c.copy(sampleLabel = Some(x))
    } text "Sample label is missing"
    opt[Int]('s', "insertsize") valueName "<insertsize>" action { (x, c) =>
      c.copy(insertSize = Some(x))
    } text "Insertsize is missing"
    opt[File]('o', "output") valueName "<output>" action { (x, c) =>
      c.copy(output = Some(x))
    } text "Output path is missing"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val input: File = commandArgs.inputBam
    val output: File =
      commandArgs.output.getOrElse(new File(input.getAbsoluteFile + ".pindel.cfg"))
    val insertSize: Int = commandArgs.insertSize.getOrElse(BamUtils.sampleBamInsertSize(input))

    val bamReader = SamReaderFactory.makeDefault().open(input)
    val writer = new PrintWriter(output)
    for (sample <- BamUtils.sampleBamMap(List(input))) {
      writer.write("%s\t%d\t%s\n".format(sample._2.getAbsoluteFile, insertSize, sample._1))
    }
    bamReader.close()
    writer.close()

    // the logic here is to pull the libraries stored in the bam file and output this to a pindel config file.
    // see: http://gmt.genome.wustl.edu/packages/pindel/quick-start.html
    // this is called bam-configuration file

    // sampleLabel can be given from the commandline or read from the bam header

    /**
    * filename<tab>avg insert size<tab>sample_label or name for reporting
    * tumor_sample_1222.bam<tab>250<tab>TUMOR_1222
    * somatic_sample_1222.bam<tab>250<tab>HEALTHY_1222
    */

  }
}
