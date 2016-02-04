package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.FastqReader
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._

/**
 * Created by sajvanderzeeuw on 2-2-16.
 */
object CheckFastqPairs extends ToolCommand {
  /**
   * Args for commandline program
   * @param input input first fastq file (R1) (can be zipped)
   * @param input2 input second fastq file (R2) (can be zipped)
   * @param output output fastq file (can be zipped)
   */

  case class Args(input: File = null, input2: Option[File] = None, output: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "fastq1") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('j', "fastq2") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(input2 = Some(x))
    }
    opt[File]('o', "output") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val readFq1 = new FastqReader(cmdArgs.input)
    val readFq2 = cmdArgs.input2.map(new FastqReader(_))

    var counter = 0
    var countline = 0

    for (recordR1 <- readFq1.iterator()) {
      if (readFq2.map(_.hasNext) == Some(false))
        throw new IllegalStateException("R2 has less reads then R1")

      //Getting R2 record, None if it's single end
      val recordR2 = readFq2.map(_.next())

      recordR2 match {
        case Some(recordR2) => // Paired End
          val readHeader = recordR1.getReadHeader
          val readHeader2 = recordR2.getReadHeader
          val id1 = readHeader.takeWhile(_ != ' ')
          val id2 = readHeader2.takeWhile(_ != ' ')

          if (counter % 1e5 == 0) logger.info(counter + " reads processed")

          if (id1 == id2){

          } else if (id1.stripSuffix("/1") == id2.stripSuffix("/2")) {

          } else if (id1.stripSuffix(".1") == id2.stripSuffix(".2")) {

          } else
            throw new IllegalStateException(s"sequenceHeaders does not match at line '${(counter*4)-3}'. R1: '$readHeader', R2: '$readHeader2'")
        case _ => // Single end
      }
      counter += 1
    }

    if (readFq2.map(_.hasNext) == Some(true))
      throw new IllegalStateException("R2 has more reads then R1")

    logger.info("Done processing the Fastq file(s) no errors found")
    readFq1.close()
    readFq2.foreach(_.close())
  }

}