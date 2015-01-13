package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommand
import htsjdk.samtools.fastq.{ FastqRecord, AsyncFastqWriter, FastqReader, BasicFastqWriter }
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 1/13/15.
 */
object PrefixFastq extends ToolCommand {
  case class Args(input: File = null, output: File = null, seq: String = "CATG") extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "input") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(output = x)
    }
    opt[String]('s', "seq") maxOccurs (1) valueName ("<prefix seq>") action { (x, c) =>
      c.copy(seq = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val writer = new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.output), 3000)
    val reader = new FastqReader(cmdArgs.input)

    var counter = 0
    while (reader.hasNext) {
      val read = reader.next()

      val maxQuality = read.getBaseQualityString.max

      val readHeader = read.getReadHeader
      val readSeq = cmdArgs.seq + read.getReadString
      val baseQualityHeader = read.getBaseQualityHeader
      val baseQuality = Array.fill(cmdArgs.seq.size)(maxQuality).mkString + read.getBaseQualityString

      writer.write(new FastqRecord(readHeader, readSeq, baseQualityHeader, baseQuality))

      counter += 1
      if (counter % 1e6 == 0) logger.info(counter + " reads processed")
    }

    if (counter % 1e6 != 0) logger.info(counter + " reads processed")
    writer.close()
    reader.close()
    logger.info("Done")
  }
}
