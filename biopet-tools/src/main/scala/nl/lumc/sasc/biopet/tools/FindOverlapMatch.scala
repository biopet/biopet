package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * Created by pjvan_thof on 21-9-16.
 */
object FindOverlapMatch extends ToolCommand {

  case class Args(inputVcf: File = null,
                  cutoff: Double = 0.0) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputVcf = x)
    }
    opt[Double]('c', "cutoff") required () unbounded () valueName "<value>" action { (x, c) =>
      c.copy(cutoff = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val reader = Source.fromFile(cmdargs.inputVcf)

    val data = reader.getLines().map(_.split("\t")).toArray

    val samples = data.head.zipWithIndex.tail

    var overlap = 0
    var multiOverlap = 0
    var noOverlap = 0

    for (i1 <- samples) {
      val buffer = ListBuffer[(String, Double)]()
      for (i2 <- samples ) {
        val value = data(i1._2)(i2._2).toDouble
        if (value >= cmdargs.cutoff && i1._2 != i2._2) {
          buffer.+=((i2._1, value))
        }
      }
      if (buffer.nonEmpty) overlap += 1
      else noOverlap += 1
      if (buffer.size > 1) multiOverlap += 1

      println(s"${i1._1}\t${buffer.mkString("\t")}")
    }
    logger.info(s"$overlap found")
    logger.info(s"no $noOverlap found")
    logger.info(s"multi $multiOverlap found")
  }
}
