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

    //Iterate over the fastq file check for the length of both files if not correct, exit the tool and print error message
    for (recordR1 <- readFq1.iterator()) {
      if (readFq2.map(_.hasNext) == Some(false))
        throw new IllegalStateException("R2 has less reads then R1")

      //Getting R2 record, None if it's single end
      val recordR2 = readFq2.map(_.next())

      //Here we check if the readnames of both files are concordant, and if the sequence content are correct DNA/RNA sequences
      recordR2 match {
        case Some(recordR2) => // Paired End
          val readHeader = recordR1.getReadHeader
          val readHeader2 = recordR2.getReadHeader
          val readSeq = recordR1.getReadString
          val readSeq2 = recordR2.getReadString
          val id1 = readHeader.takeWhile(_ != ' ')
          val id2 = readHeader2.takeWhile(_ != ' ')

          if (counter % 1e5 == 0) logger.info(counter + " reads processed")


          val allowedBases = """([actgnACTGN+]+)""".r

          val validBases: Boolean = readSeq match {
            case allowedBases(m) => true
            case _ => throw new IllegalStateException(s"Non IUPAC symbols identified '${(counter*4)-3}'")
          }

          val validBases2: Boolean = readSeq2 match {
            case allowedBases(m) => true
            case _ => throw new IllegalStateException(s"Non IUPAC symbols identified '${(counter*4)-3}'")
          }

          if (id1 == id2){

          } else if (id1.stripSuffix("/1") == id2.stripSuffix("/2")) {

          } else if (id1.stripSuffix(".1") == id2.stripSuffix(".2")) {

          } else
            throw new IllegalStateException(s"sequenceHeaders does not match at line '${(counter*4)-3}'. R1: '$readHeader', R2: '$readHeader2'")
        case _ => // Single end
      }
      counter += 1
    }

    //if R2 is longer then R1 print an error code and exit the tool
    if (readFq2.map(_.hasNext) == Some(true))
      throw new IllegalStateException("R2 has more reads then R1")

    logger.info("Done processing the Fastq file(s) no errors found")
    logger.info("total reads processed: " + counter)
    //close both iterators
    readFq1.close()
    readFq2.foreach(_.close())
  }

}