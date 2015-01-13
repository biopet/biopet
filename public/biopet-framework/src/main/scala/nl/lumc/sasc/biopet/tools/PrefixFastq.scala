package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, ToolCommand }
import htsjdk.samtools.fastq.{ FastqRecord, AsyncFastqWriter, FastqReader, BasicFastqWriter }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 1/13/15.
 */
class PrefixFastq(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var inputFastq: File = _

  @Output(doc = "Output fastq", shortName = "o", required = true)
  var outputFastq: File = _

  @Argument(doc = "Prefix seq", required = true)
  var prefixSeq: String = _

  override def commandLine = super.commandLine +
    required("-i", inputFastq) +
    required("-o", outputFastq) +
    optional("-s", prefixSeq)
}

object PrefixFastq extends ToolCommand {
  def apply(root: Configurable, input: File, outputDir: String): PrefixFastq = {
    val prefixFastq = new PrefixFastq(root)
    prefixFastq.inputFastq = input
    prefixFastq.outputFastq = new File(outputDir, input.getName + ".prefix.fastq")
    return prefixFastq
  }

  case class Args(input: File = null, output: File = null, seq: String = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "input") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(output = x)
    }
    opt[String]('s', "seq") required () maxOccurs (1) valueName ("<prefix seq>") action { (x, c) =>
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
