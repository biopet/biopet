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

import java.io.{ File, PrintWriter }

import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{ VcfUtils, ToolCommand }
import nl.lumc.sasc.biopet.utils.intervals.BedRecord

import scala.collection.JavaConversions._

/**
 * Created by ahbbollen on 13-10-15.
 * Create bed track from genome quality values in (g)VCF
 */
object GvcfToBed extends ToolCommand {

  case class Args(inputVcf: File = null,
                  outputBed: File = null,
                  invertedOutputBed: Option[File] = None,
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
    opt[File]("invertedOutputBed") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(invertedOutputBed = Some(x))
    } text "Output bed file"
    opt[String]('S', "sample") unbounded () maxOccurs 1 valueName "<sample>" action { (x, c) =>
      c.copy(sample = Some(x))
    } text "Sample to consider. Will take first sample on alphabetical order by default"
    opt[Int]("minGenomeQuality") unbounded () maxOccurs 1 valueName "<int>" action { (x, c) =>
      c.copy(minGenomeQuality = x)
    } text "Minimum genome quality to consider"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.debug("Opening reader")
    val reader = new VCFFileReader(cmdArgs.inputVcf, false)
    logger.debug("Opening writer")
    val writer = new PrintWriter(cmdArgs.outputBed)
    val invertedWriter = cmdArgs.invertedOutputBed.collect {
      case file =>
        logger.debug("Opening inverted writer")
        new PrintWriter(file)
    }

    val sample = cmdArgs.sample.getOrElse(reader.getFileHeader.getSampleNamesInOrder.head)

    val it = reader.iterator()
    val firstRecord = it.next()
    var contig = firstRecord.getContig
    var start = firstRecord.getStart
    var end = firstRecord.getEnd
    var pass = VcfUtils.hasMinGenomeQuality(firstRecord, sample, cmdArgs.minGenomeQuality)

    def writeResetCachedRecord(newRecord: VariantContext): Unit = {
      writeCachedRecord()
      contig = newRecord.getContig
      start = newRecord.getStart
      end = newRecord.getEnd
      pass = VcfUtils.hasMinGenomeQuality(newRecord, sample, cmdArgs.minGenomeQuality)
    }

    def writeCachedRecord(): Unit = {
      if (pass) writer.println(new BedRecord(contig, start - 1, end))
      else invertedWriter.foreach(_.println(new BedRecord(contig, start - 1, end)))
    }

    var counter = 1
    logger.info("Start")
    for (r <- it) {
      if (contig == r.getContig) {
        val p = VcfUtils.hasMinGenomeQuality(r, sample, cmdArgs.minGenomeQuality)
        if (p != pass || r.getStart > (end + 1)) writeResetCachedRecord(r)
        else end = r.getEnd
      } else writeResetCachedRecord(r)

      counter += 1
      if (counter % 100000 == 0) {
        logger.info(s"Processed $counter records")
      }
    }
    writeCachedRecord()

    logger.info(s"Processed $counter records")

    logger.debug("Closing writer")
    writer.close()
    invertedWriter.foreach { w =>
      logger.debug("Closing inverted writer")
      w.close()
    }
    logger.debug("Closing reader")
    reader.close()

    logger.info("Finished!")
  }
}
