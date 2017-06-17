package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.utils.{BamUtils, ToolCommand}
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 17/06/2017.
  */
object MultiCoverage extends ToolCommand {
  case class Args(bedFile: File = null, bamFiles: List[File] = Nil, outputFile: File = null)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
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
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val bamFiles = BamUtils.sampleBamMap(cmdargs.bamFiles)

    val futures = for (region <- BedRecordList.fromFile(cmdargs.bedFile).allRecords)
      yield
        Future {
          val samInterval = region.toSamInterval
          val counts = bamFiles.map {
            case (sampleName, bamFile) =>
              val samReader = SamReaderFactory.makeDefault.open(bamFile)
              val count = sampleName -> samReader
                .queryOverlapping(samInterval.getContig, samInterval.getStart, samInterval.getEnd)
                .foldLeft(0L) {
                  case (bases, samRecord) =>
                    val start = (samInterval.getStart :: samRecord.getAlignmentStart :: Nil).max
                    val end = (samInterval.getEnd :: samRecord.getAlignmentEnd + 1 :: Nil).min
                    bases + (end - start)
                }
              samReader.close()
              count
          }
          region -> counts
        }

    val writer = new PrintWriter(cmdargs.outputFile)
    val samples = bamFiles.keys.toList
    writer.println(s"#contig\tstart\tend\t${samples.mkString("\t")}")
    for (future <- futures) {
      val (region, counts) = Await.result(future, Duration.Inf)
      writer.println(
        s"${region.chr}\t${region.start}\t${region.end}\t${samples.map(counts).mkString("\t")}")
    }
    writer.close()

  }
}
