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
package nl.lumc.sasc.biopet.tools

import java.io.{ File, IOException }
import scala.collection.JavaConversions._
import scala.collection.mutable.{ Map => MMap }

import htsjdk.tribble.TribbleException
import htsjdk.variant.variantcontext.{ VariantContextBuilder, VariantContext }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf._
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, ToolCommand }
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * This tool parses a VEP annotated VCF into a standard VCF file.
 * The VEP puts all its annotations for each variant in an CSQ string, where annotations per transcript are comma-separated
 * Annotations are then furthermore pipe-separated.
 * This tool has two modes:
 * 1) explode - explodes all transcripts such that each is on a unique line
 * 2) standard - parse as a standard VCF, where multiple transcripts occur in the same line
 * Created by ahbbollen on 10/27/14.
 */

class VEPNormalizer(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input VCF, may be indexed", shortName = "InputFile", required = true)
  var inputVCF: File = null

  @Output(doc = "Output VCF", shortName = "OutputFile", required = true)
  var outputVCF: File = null

  var mode: String = config("mode", default = "explode")
  var doNotRemove: Boolean = config("donotremove", default = false)

  memoryLimit = Some(1.0)
  override val defaultVmem = "4G"

  override def commandLine = super.commandLine +
    required("-I", inputVCF) +
    required("-O", outputVCF) +
    required("-m", mode) +
    conditional(doNotRemove, "--do-not-remove")
}

object VEPNormalizer extends ToolCommand {

  def main(args: Array[String]): Unit = {
    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val input = commandArgs.inputVCF
    val output = commandArgs.outputVCF

    logger.info(s"""Input VCF is $input""")
    logger.info(s"""Output VCF is $output""")

    val reader = try {
      new VCFFileReader(input, false)
    } catch {
      case e: TribbleException.MalformedFeatureFile =>
        logger.error("Malformed VCF file! Are you sure this isn't a VCFv3 file?")
        throw e
    }

    val header = reader.getFileHeader
    logger.debug("Checking for CSQ tag")
    csqCheck(header)
    logger.debug("CSQ tag OK")
    logger.debug("Checkion VCF version")
    versionCheck(header)
    logger.debug("VCF version OK")
    logger.debug("Parsing header")
    val new_infos = parseCsq(header)
    header.setWriteCommandLine(true)
    for (info <- new_infos) {
      val tmpheaderline = new VCFInfoHeaderLine(info, VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "A VEP annotation")
      header.addMetaDataLine(tmpheaderline)
    }
    logger.debug("Header parsing done")

    logger.debug("Writing header to file")
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setOutputFile(output).
      build())
    writer.writeHeader(header)
    logger.debug("Wrote header to file")

    normalize(reader, writer, new_infos, commandArgs.mode, commandArgs.removeCSQ)
    writer.close()
    logger.debug("Closed writer")
    reader.close()
    logger.debug("Closed reader")
    logger.info("Done. Goodbye")
  }

  /**
   * Normalizer
   * @param reader input VCF VCFFileReader
   * @param writer output VCF AsyncVariantContextWriter
   * @param newInfos array of string containing names of new info fields
   * @param mode normalizer mode (explode or standard)
   * @param removeCsq remove csq tag (Boolean)
   * @return
   */
  def normalize(reader: VCFFileReader, writer: AsyncVariantContextWriter,
                newInfos: Array[String], mode: String, removeCsq: Boolean) = {
    logger.info(s"""You have selected mode $mode""")
    logger.info("Start processing records")

    for (record <- reader) {
      mode match {
        case "explode"  => explodeTranscripts(record, newInfos, removeCsq).foreach(vc => writer.add(vc))
        case "standard" => writer.add(standardTranscripts(record, newInfos, removeCsq))
        case _          => throw new IllegalArgumentException("Something odd happened!")
      }
    }
  }

  /**
   * Checks whether header has a CSQ tag
   * @param header VCF header
   */
  def csqCheck(header: VCFHeader) = {
    if (!header.hasInfoLine("CSQ")) {
      //logger.error("No CSQ info tag found! Is this file VEP-annotated?")
      throw new IllegalArgumentException("No CSQ info tag found! Is this file VEP-annotated?")
    }
  }

  /**
   * Checks whether version of input VCF is at least 4.0
   * VEP is known to cause issues below 4.0
   * Throws exception if not
   * @param header VCFHeader of input VCF
   */
  def versionCheck(header: VCFHeader) = {
    var format = ""
    //HACK: getMetaDataLine does not work for fileformat
    for (line <- header.getMetaDataInInputOrder) {
      if (line.getKey == "fileformat" || line.getKey == "format") {
        format = line.getValue
      }
    }
    val version = VCFHeaderVersion.toHeaderVersion(format)
    if (!version.isAtLeastAsRecentAs(VCFHeaderVersion.VCF4_0)) {
      throw new IllegalArgumentException(s"""version $version is not supported""")
    }
  }

  /**
   * Parses the CSQ tag in the header
   * @param header the VCF header
   * @return list of strings with new info fields
   */
  def parseCsq(header: VCFHeader): Array[String] = {
    header.getInfoHeaderLine("CSQ").getDescription.
      split(':')(1).trim.split('|').map("VEP_" + _)
  }

  /**
   * Explode a single VEP-annotated record to multiple normal records
   * Based on the number of annotated transcripts in the CSQ tag
   * @param record the record as a VariantContext object
   * @param csq_infos An array with names of new info tags
   * @return An array with the new records
   */
  def explodeTranscripts(record: VariantContext, csq_infos: Array[String], remove_CSQ: Boolean): Array[VariantContext] = {
    val csq = record.getAttributeAsString("CSQ", "unknown")
    val attributes = if (remove_CSQ) record.getAttributes.toMap - "CSQ" else record.getAttributes.toMap

    csq.
      stripPrefix("[").
      stripSuffix("]").
      split(",").
      map(x => attributes ++ csq_infos.zip(x.split("""\|""", -1))).
      map(x => {
        if (remove_CSQ) new VariantContextBuilder(record)
          .attributes(x)
          .make()
        else new VariantContextBuilder(record).attributes(x).make()
      })
  }

  def standardTranscripts(record: VariantContext, csqInfos: Array[String], removeCsq: Boolean): VariantContext = {
    val csq = record.getAttributeAsString("CSQ", "unknown")
    val attributes = if (removeCsq) record.getAttributes.toMap - "CSQ" else record.getAttributes.toMap

    val newAttrs = attributes ++ csqInfos.zip(csq.
      stripPrefix("[").
      stripSuffix("]").
      split(",").
      // This makes a list of lists with each annotation for every transcript in a top-level list element
      foldLeft(List.fill(csqInfos.length) { List.empty[String] })(
        (acc, x) => {
          val broken = x.split("""\|""", -1)
          acc.zip(broken).map(x => x._2 :: x._1)
        }
      ).
        map(x => x.mkString(",")))

    new VariantContextBuilder(record).attributes(newAttrs).make()
  }

  case class Args(inputVCF: File = null,
                  outputVCF: File = null,
                  mode: String = null,
                  removeCSQ: Boolean = true) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(s"""|$commandName - Parse VEP-annotated VCF to standard VCF format """)

    opt[File]('I', "InputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(inputVCF = x)
    } validate {
      x => if (x.exists) success else failure("Input VCF not found")
    } text "Input VCF file"

    opt[File]('O', "OutputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(outputVCF = x)
    } validate {
      x =>
        if (!x.getName.endsWith(".vcf") && (!x.getName.endsWith(".vcf.gz")) && (!x.getName.endsWith(".bcf")))
          failure("Unsupported output file type") else success
    } text "Output VCF file"

    opt[String]('m', "mode") required () valueName "<mode>" action { (x, c) =>
      c.copy(mode = x)
    } validate {
      x => if (x == "explode") success else if (x == "standard") success else failure("Unsupported mode")
    } text "Mode"

    opt[Unit]("do-not-remove") action { (x, c) =>
      c.copy(removeCSQ = false)
    } text "Do not remove CSQ tag"
  }
}

