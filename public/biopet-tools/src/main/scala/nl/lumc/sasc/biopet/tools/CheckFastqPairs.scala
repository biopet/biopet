package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.{FastqRecord, FastqReader}
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
   */

  case class Args(input: File = null, input2: Option[File] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "fastq1") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('j', "fastq2") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(input2 = Some(x))
    }
  }

  def main(args: Array[String]): Unit = {
    //Start analyses of fastq files
    logger.info("Start")

    //parse all possible options into OptParser
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    //read in fastq file 1 and if present fastq file 2
    val readFq1 = new FastqReader(cmdArgs.input)
    val readFq2 = cmdArgs.input2.map(new FastqReader(_))

    //define a counter to track the number of objects passing through the for loop

    var counter = 0

    try {
      //Iterate over the fastq file check for the length of both files if not correct, exit the tool and print error message
      for (recordR1 <- readFq1.iterator()) {
        counter += 1
        if (readFq2.map(_.hasNext) == Some(false))
          throw new IllegalStateException("R2 has less reads then R1")

        //Getting R2 record, None if it's single end
        val recordR2 = readFq2.map(_.next())

        validFastqRecord(recordR1)

        //Here we check if the readnames of both files are concordant, and if the sequence content are correct DNA/RNA sequences
        recordR2 match {
          case Some(recordR2) => // Paired End
            validFastqRecord(recordR2)
            checkMate(recordR1, recordR2)
          case _ => // Single end
        }
        if (counter % 1e5 == 0) logger.info(counter + " reads processed")
      }

      //if R2 is longer then R1 print an error code and exit the tool
      if (readFq2.map(_.hasNext) == Some(true))
        throw new IllegalStateException("R2 has more reads then R1")

      logger.info(s"Done processing ${counter} fastq records, no errors found")
    } catch {
      case e:IllegalStateException =>
        logger.error(s"Error found at readnumber: $counter, linenumber ${(counter*4)-3}")
        logger.error(e.getMessage)
    }

    //close both iterators
    readFq1.close()
    readFq2.foreach(_.close())
  }

  val allowedBases = """([actgnACTGN+]+)""".r

  /**
   *
   * @param record
   * @throws IllegalStateException
   */
  def validFastqRecord(record: FastqRecord): Unit = {
    record.getReadString match {
      case allowedBases(m) =>
      case _ => throw new IllegalStateException(s"Non IUPAC symbols identified")
    }
    if (record.getReadString.size != record.getBaseQualityString.size)
      throw new IllegalStateException(s"Sequence length do not match quality length")
  }

  /**
   *
   * @param r1
   * @param r2
   * @throws IllegalStateException
   */
  def checkMate(r1: FastqRecord, r2: FastqRecord): Unit = {
    val id1 = r1.getReadHeader.takeWhile(_ != ' ')
    val id2 = r2.getReadHeader.takeWhile(_ != ' ')
    if (!(id1 == id2 ||
      id1.stripSuffix("/1") == id2.stripSuffix("/2") ||
      id1.stripSuffix(".1") == id2.stripSuffix(".2")))
      throw new IllegalStateException(s"sequenceHeaders does not match. R1: '${r1.getReadHeader}', R2: '${r2.getReadHeader}'")
  }

  //TODO: check duplicate read ideas in both R1 and R2
}