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
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.tribble.TribbleException
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.variantcontext.{VariantContext, VariantContextBuilder}
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._

/**
  * This tool parses a VEP annotated VCF into a standard VCF file.
  * The VEP puts all its annotations for each variant in an CSQ string, where annotations per transcript are comma-separated
  * Annotations are then furthermore pipe-separated.
  * This tool has two modes:
  * 1) explode - explodes all transcripts such that each is on a unique line
  * 2) standard - parse as a standard VCF, where multiple transcripts occur in the same line
  * Created by ahbbollen on 10/27/14.
  */
object VepNormalizer extends ToolCommand {

  def main(args: Array[String]): Unit = {
    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

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
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(output)
        .setReferenceDictionary(header.getSequenceDictionary)
        build ())

    if (reader.iterator().hasNext) {
      logger.debug("Checking for CSQ tag")
      csqCheck(header)
      logger.debug("CSQ tag OK")
      logger.debug("Checkion VCF version")
      versionCheck(header)
      logger.debug("VCF version OK")
      logger.debug("Parsing header")
      val newInfos = parseCsq(header)
      header.setWriteCommandLine(true)

      for (info <- newInfos) {
        val tmpheaderline = new VCFInfoHeaderLine(info,
                                                  VCFHeaderLineCount.UNBOUNDED,
                                                  VCFHeaderLineType.String,
                                                  "A VEP annotation")
        header.addMetaDataLine(tmpheaderline)
      }
      logger.debug("Header parsing done")

      logger.debug("Writing header to file")

      writer.writeHeader(header)
      logger.debug("Wrote header to file")

      normalize(reader, writer, newInfos, commandArgs.mode, commandArgs.removeCSQ)
    } else {
      logger.debug("No variants found, skipping normalize step")
      writer.writeHeader(header)
    }
    writer.close()
    logger.debug("Closed writer")
    reader.close()
    logger.debug("Closed reader")
    logger.info("Done. Goodbye")
  }

  /**
    * Normalizer
    *
    * @param reader input VCF VCFFileReader
    * @param writer output VCF AsyncVariantContextWriter
    * @param newInfos array of string containing names of new info fields
    * @param mode normalizer mode (explode or standard)
    * @param removeCsq remove csq tag (Boolean)
    * @return
    */
  def normalize(reader: VCFFileReader,
                writer: AsyncVariantContextWriter,
                newInfos: Array[String],
                mode: String,
                removeCsq: Boolean): Unit = {
    logger.info(s"""You have selected mode $mode""")
    logger.info("Start processing records")

    var counter = 0
    for (record <- reader) {
      mode match {
        case "explode" =>
          explodeTranscripts(record, newInfos, removeCsq).foreach(vc => writer.add(vc))
        case "standard" => writer.add(standardTranscripts(record, newInfos, removeCsq))
        case _ => throw new IllegalArgumentException("Something odd happened!")
      }
      counter += 1
      if (counter % 100000 == 0) logger.info(counter + " variants processed")
    }
    logger.info("done: " + counter + " variants processed")
  }

  /**
    * Checks whether header has a CSQ tag
    *
    * @param header VCF header
    */
  def csqCheck(header: VCFHeader): Unit = {
    if (!header.hasInfoLine("CSQ")) {
      //logger.error("No CSQ info tag found! Is this file VEP-annotated?")
      throw new IllegalArgumentException("No CSQ info tag found! Is this file VEP-annotated?")
    }
  }

  /**
    * Checks whether version of input VCF is at least 4.0
    * VEP is known to cause issues below 4.0
    * Throws exception if not
    *
    * @param header VCFHeader of input VCF
    */
  def versionCheck(header: VCFHeader): Unit = {
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
    *
    * @param header the VCF header
    * @return list of strings with new info fields
    */
  def parseCsq(header: VCFHeader): Array[String] = {
    header.getInfoHeaderLine("CSQ").getDescription.split(':')(1).trim.split('|').map("VEP_" + _)
  }

  /**
    * Explode a single VEP-annotated record to multiple normal records
    * Based on the number of annotated transcripts in the CSQ tag
    *
    * @param record the record as a VariantContext object
    * @param csqInfos An array with names of new info tags
    * @return An array with the new records
    */
  def explodeTranscripts(record: VariantContext,
                         csqInfos: Array[String],
                         removeCsq: Boolean): Array[VariantContext] = {
    for (transcript <- parseCsq(record)) yield {
      (for (fieldId <- csqInfos.indices if transcript.isDefinedAt(fieldId);
            value = transcript(fieldId) if value.nonEmpty) yield csqInfos(fieldId) -> value)
        .filterNot(_._2.isEmpty)
        .foldLeft(createBuilder(record, removeCsq))((builder, attribute) =>
          builder.attribute(attribute._1, attribute._2))
        .make()
    }
  }

  def standardTranscripts(record: VariantContext,
                          csqInfos: Array[String],
                          removeCsq: Boolean): VariantContext = {
    val attribs = parseCsq(record)

    (for (fieldId <- csqInfos.indices)
      yield
        csqInfos(fieldId) -> {
          for (transcript <- attribs if transcript.isDefinedAt(fieldId);
               value = transcript(fieldId) if value.nonEmpty) yield value
        })
      .filter(_._2.nonEmpty)
      .foldLeft(createBuilder(record, removeCsq))((builder, attribute) =>
        builder.attribute(attribute._1, attribute._2))
      .make()
  }

  protected def createBuilder(record: VariantContext, removeCsq: Boolean): VariantContextBuilder = {
    if (removeCsq) new VariantContextBuilder(record).rmAttribute("CSQ")
    else new VariantContextBuilder(record)
  }

  protected def parseCsq(record: VariantContext): Array[Array[String]] = {
    record
      .getAttributeAsString("CSQ", "unknown")
      .stripPrefix("[")
      .stripSuffix("]")
      .split(",")
      .map(_.split("""\|""").map(_.trim))
  }

  case class Args(inputVCF: File = null,
                  outputVCF: File = null,
                  mode: String = null,
                  removeCSQ: Boolean = true)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(s"""|$commandName - Parse VEP-annotated VCF to standard VCF format """)

    opt[File]('I', "InputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(inputVCF = x)
    } validate { x =>
      if (x.exists) success else failure("Input VCF not found")
    } text "Input VCF file. Required."
    opt[File]('O', "OutputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(outputVCF = x)
    } validate { x =>
      if (!x.getName.endsWith(".vcf") && (!x.getName.endsWith(".vcf.gz")) && (!x.getName.endsWith(
            ".bcf")))
        failure("Unsupported output file type")
      else success
    } text "Output VCF file. Required."

    opt[String]('m', "mode") required () valueName "<mode>" action { (x, c) =>
      c.copy(mode = x)
    } validate { x =>
      if (x == "explode") success
      else if (x == "standard") success
      else failure("Unsupported mode")
    } text "Mode. Can choose between <standard> (generates standard vcf) and <explode> (generates new record for each transcript). Required."

    opt[Unit]("do-not-remove") action { (_, c) =>
      c.copy(removeCSQ = false)
    } text "Do not remove CSQ tag. Optional"
  }
}
