package nl.lumc.sasc.biopet.tools

import java.io.{ File, IOException }
import scala.collection.JavaConversions._
import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, ToolCommand }
import collection.mutable.{ Map => MMap }
import collection.JavaConverters._
import htsjdk.variant.vcf._
import htsjdk.variant.variantcontext.{ VariantContextBuilder, VariantContext }
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

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
  var inputVCF: File = _

  @Output(doc = "Output VCF", shortName = "OutputFile", required = true)
  var outputVCF: File = _

  var mode: String = config("mode", default = "explode")

  override def commandLine = super.commandLine +
    required("-I", inputVCF) +
    required("-O", outputVCF) +
    required("-m", mode)
}

object VEPNormalizer extends ToolCommand {

  def main(args: Array[String]): Unit = {
    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val input = commandArgs.inputVCF
    val output = commandArgs.outputVCF

    if (commandArgs.mode == "explode") {
      logger.info("You have selected explode mode")
      logger.info(s"""Input VCF is $input""")
      logger.info(s"""Output VCF is $output""")
      explode(commandArgs.inputVCF, commandArgs.outputVCF)
    } else if (commandArgs.mode == "standard") {
      logger.info("You have selected standard mode")
      logger.info(s"""Input VCF is $input""")
      logger.info(s"""Output VCF is $output""")
      standard(commandArgs.inputVCF, commandArgs.outputVCF)
    } else {
      // this should be impossible, but should nevertheless be checked
      logger.error("impossibru!", new IllegalArgumentException)
    }
  }

  /**
   * Wrapper for mode explode
   * @param input input VCF file
   * @param output output VCF file
   */
  def explode(input: File, output: File) = {
    var reader: VCFFileReader = null
    // this can give a codec error if malformed VCF
    //
    try {
      reader = new VCFFileReader(input, false)
    } catch {
      case e: Exception =>
        logger.error("Malformed VCF file! VCFv3 not supported!")
        throw e
    }

    val header = reader.getFileHeader
    logger.debug("Checking for CSQ tag")
    csqCheck(header)
    logger.debug("CSQ tag OK")
    logger.debug("Checkion VCF version")
    versionCheck(header)
    logger.debug("VCF version OK")
    val seqDict = header.getSequenceDictionary
    logger.debug("Parsing header")
    val new_infos = parseCsq(header)
    header.setWriteCommandLine(true)
    for (info <- new_infos) {
      val tmpheaderline = new VCFInfoHeaderLine(info, VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "A VEP annotation")
      header.addMetaDataLine(tmpheaderline)
    }
    logger.debug("Header parsing done")

    logger.debug("Writing header to file")
    val writerBuilder = new VariantContextWriterBuilder()
    writerBuilder.
      setOutputFile(output).
      setOutputFileType(VariantContextWriterBuilder.OutputType.BLOCK_COMPRESSED_VCF).
      setReferenceDictionary(seqDict)
    val writer = writerBuilder.build()
    writer.writeHeader(header)
    logger.debug("Wrote header to file")

    logger.info("Start processing records")
    var nprocessed_records: Int = 0
    var nwritten_records: Int = 0
    for ((record, i) <- reader.iterator().zipWithIndex) {
      nprocessed_records += 1
      if (i % 1000 == 0) {
        logger.info(s"""Read $i records \t Wrote $nwritten_records records""")
      }
      val new_records = explodeTranscripts(record, new_infos)
      for (vc <- new_records) {
        writer.add(vc)
      }
      nwritten_records += new_records.length
    }
    logger.info(s"""Processed $nprocessed_records records""")
    logger.info("Done. Goodbye")
    writer.close()
    logger.debug("Closed writer")
    reader.close()
    logger.debug("Closed reader")
  }

  /**
   * Wrapper for mode standard
   * @param input input VCF file
   * @param output output VCF file
   */
  def standard(input: File, output: File) = {
    val reader: VCFFileReader = try {
      new VCFFileReader(input, false)
    } catch {
      case e: Exception =>
        logger.error("Malformed VCF file! VCFv3 not supported!")
        throw e
    }

    val header = reader.getFileHeader
    logger.debug("Checking for CSQ tag")
    csqCheck(header)
    logger.debug("CSQ tag OK")
    logger.debug("Checkion VCF version")
    versionCheck(header)
    logger.debug("VCF version OK")
    val seqDict = header.getSequenceDictionary
    logger.debug("Parsing header")
    val new_infos = parseCsq(header)
    header.setWriteCommandLine(true)
    for (info <- new_infos) {
      val tmpheaderline = new VCFInfoHeaderLine(info, VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "A VEP annotation")
      header.addMetaDataLine(tmpheaderline)
    }
    logger.debug("Header parsing done")

    logger.debug("Writing header to file")
    val writerBuilder = new VariantContextWriterBuilder()
    writerBuilder.
      setOutputFile(output).
      setOutputFileType(VariantContextWriterBuilder.OutputType.VCF).
      setReferenceDictionary(seqDict)
    val writer = writerBuilder.build()
    writer.writeHeader(header)
    logger.debug("Wrote header to file")

    logger.info("Start processing records")
    var nprocessed_records: Int = 0
    var nwritten_records: Int = 0
    for ((record, i) <- reader.iterator().zipWithIndex) {
      nprocessed_records += 1
      if (i % 1000 == 0) {
        logger.info(s"""Read $i records \t Wrote $nwritten_records records""")
      }
      writer.add(standardTranscripts(record, new_infos))
      nwritten_records += 1
    }
    logger.info(s"""Processed $nprocessed_records records""")
    logger.info("Done. Goodbye")
    writer.close()
    logger.debug("Closed writer")
    reader.close()
    logger.debug("Closed reader")
  }

  /**
   * Checks whether header has a CSQ tag
   * @param header VCF header
   */
  def csqCheck(header: VCFHeader) = {
    if (!header.hasInfoLine("CSQ")) {
      logger.error("No CSQ info tag found! Is this file VEP-annotated?")
      throw new VEPException("")
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
      logger.error(s"""version $version is not supported""")
      throw new VEPException("")
    }
  }

  /**
   * Parses the CSQ tag in the header
   * @param header the VCF header
   * @return list of strings with new info fields
   */
  def parseCsq(header: VCFHeader): Array[String] = {
    val csq = header.getInfoHeaderLine("CSQ").getDescription
    val items = csq.split(':')(1).trim.split('|')
    items
  }

  /**
   * Explode a single VEP-annotated record to multiple normal records
   * Based on the number of annotated transcripts in the CSQ tag
   * @param record the record as a VariantContext object
   * @param csq_infos An array with names of new info tags
   * @return An array with the new records
   */
  def explodeTranscripts(record: VariantContext, csq_infos: Array[String]): Array[VariantContext] = {
    val csq = record.getAttributeAsString("CSQ", "unknown")
    val attributes = record.getAttributes.toMap

    csq.
      stripPrefix("[").
      stripSuffix("]").
      split(",").
      map(x => attributes ++ csq_infos.zip(x.split("""\|""", -1))).
      map(x => new VariantContextBuilder(record).attributes(x).make())
  }

  def standardTranscripts(record: VariantContext, csqInfos: Array[String]): VariantContext = {
    val csq = record.getAttributeAsString("CSQ", "unknown")
    val attributes = record.getAttributes.toMap

    val new_attrs = attributes ++ csqInfos.zip(csq.
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

    new VariantContextBuilder(record).attributes(new_attrs).make()
  }
  /**
   * This one-line class defines a new VEP-specific exception
   * @param msg The error message
   */
  class VEPException(msg: String) extends RuntimeException(msg)

  /**
   * This one-line class defines a version exception
   * @param msg The error message
   */
  class VCFVersionException(msg: String) extends RuntimeException(msg)

  case class Args(inputVCF: File = null,
                  outputVCF: File = null,
                  mode: String = null) extends AbstractArgs

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
      x => if (x.exists) success else success
    } text "Output VCF file"

    opt[String]('m', "mode") required () valueName "<mode>" action { (x, c) =>
      c.copy(mode = x)
    } validate {
      x => if (x == "explode") success else if (x == "standard") success else failure("Unsupported mode")
    } text "Mode"
  }
}

