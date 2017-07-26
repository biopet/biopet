package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, BamUtils, ToolCommand}
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 17/06/2017.
  */
object MultiCoverage extends ToolCommand {
  case class Args(bedFile: File = null,
                  bamFiles: List[File] = Nil,
                  outputFile: File = null,
                  mean: Boolean = false)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('L', "bedFile") required () maxOccurs 1 unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(bedFile = x)
    } text "input bedfile"
    opt[File]('b', "bamFile") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(bamFiles = x :: c.bamFiles)
    } text "input bam files"
    opt[File]('o', "output") required () maxOccurs 1 unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(outputFile = x)
    } text "output file"
    opt[Unit]("mean") unbounded () valueName "<file>" action { (_, c) =>
      c.copy(mean = true)
    } text "By default total bases is outputed, enable this option make the output relative to region length"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val bamFiles = BamUtils.sampleBamMap(cmdargs.bamFiles)

    val futures = for (region <- BedRecordList.fromFile(cmdargs.bedFile).allRecords)
      yield
        Future {
          val samInterval = region.toSamInterval
          val counts = bamFiles.map {
            case (sampleName, bamFile) =>
              val samReader = SamReaderFactory.makeDefault.open(bamFile)
              val count = samReader
                .queryOverlapping(samInterval.getContig, samInterval.getStart, samInterval.getEnd)
                .foldLeft(0L) {
                  case (bases, samRecord) =>
                    val start = (samInterval.getStart :: samRecord.getAlignmentStart :: Nil).max
                    val end = (samInterval.getEnd :: samRecord.getAlignmentEnd + 1 :: Nil).min
                    val length = end - start
                    bases + (if (length < 0) 0 else length)
                }
              samReader.close()
              if (cmdargs.mean && region.length > 0) sampleName -> (count.toDouble / region.length)
              else if (cmdargs.mean) sampleName -> 0.0
              else sampleName -> count
          }
          region -> counts
        }

    logger.info("Reading bam files")

    var count = 0
    val writer = new PrintWriter(cmdargs.outputFile)
    val samples = bamFiles.keys.toList
    writer.println(s"#contig\tstart\tend\t${samples.mkString("\t")}")
    for (future <- futures) {
      val (region, counts) = Await.result(future, Duration.Inf)
      writer.println(
        s"${region.chr}\t${region.start}\t${region.end}\t${samples.map(counts).mkString("\t")}")
      count += 1
      if (count % 1000 == 0) logger.info(s"$count regions done")
    }
    logger.info(s"$count regions done")
    writer.close()

    logger.info("Done")
  }
}
