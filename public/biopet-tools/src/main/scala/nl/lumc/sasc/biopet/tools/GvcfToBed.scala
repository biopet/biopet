package nl.lumc.sasc.biopet.tools

import java.io.{ File, PrintWriter }

import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.BedRecord

import scala.collection.JavaConversions._

/**
 * Created by ahbbollen on 13-10-15.
 * Create bed track from genome quality values in (g)VCF
 */
object GvcfToBed extends ToolCommand {

  case class Args(inputVcf: File = null,
                  outputBed: File = null,
                  sample: Option[String] = None,
                  minGenomeQuality: Int = 0,
                  inverse: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputVcf = x)
    } text "Input vcf file"
    opt[File]('O', "outputBed") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputBed = x)
    } text "Output bed file"
    opt[String]('S', "sample") unbounded () maxOccurs 1 valueName "<sample>" action { (x, c) =>
      c.copy(sample = Some(x))
    } text "Sample to consider. Will take first sample on alphabetical order by default"
    opt[Int]("minGenomeQuality") unbounded () maxOccurs 1 valueName "<int>" action { (x, c) =>
      c.copy(minGenomeQuality = x)
    } text "Minimum genome quality to consider"
    opt[Boolean]("invert") unbounded () maxOccurs 1 valueName "<boolean>" action { (x, c) =>
      c.copy(inverse = x)
    } text "Invert filter (i.e. emit only records NOT passing filter)"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    logger.debug("Opening reader")
    val reader = new VCFFileReader(cmdArgs.inputVcf, false)
    logger.debug("Opening writer")
    val writer = new PrintWriter(cmdArgs.outputBed)

    var counter = 0

    logger.info("Start")
    for (r <- reader) {
      if (counter % 100000 == 0) {
        logger.info(s"Processed $counter records")
      }
      counter += 1
      if (!hasMinGenomeQuality(r, cmdArgs.sample, cmdArgs.minGenomeQuality) && cmdArgs.inverse) {
        writer.println(createBedRecord(r).toString)
      } else if (hasMinGenomeQuality(r, cmdArgs.sample, cmdArgs.minGenomeQuality) && !cmdArgs.inverse) {
        writer.println(createBedRecord(r).toString)
      }
    }

    logger.debug("Closing writer")
    writer.close()
    logger.debug("Closing reader")
    reader.close()

    logger.info("Finished!")
  }

  /**
   * Check whether record has minimum genome qality
   * @param record variant context
   * @param sample Option[String] with sample name
   * @param minGQ minimum genome quality value
   * @return
   */
  def hasMinGenomeQuality(record: VariantContext, sample: Option[String], minGQ: Int): Boolean = {
    sample foreach { x => if (!record.getSampleNamesOrderedByName.contains(x))
      throw new IllegalArgumentException("Sample does not exist")}
    val gt = record.getGenotype(sample.getOrElse(record.getSampleNamesOrderedByName.head))
    gt.hasGQ && gt.getGQ >= minGQ
  }

  /**
   * Create bed record from variantcontext
   * @param record variant context
   * @return BedRecord
   */
  def createBedRecord(record: VariantContext): BedRecord = {
    new BedRecord(record.getContig, record.getStart, record.getEnd)
  }

}
