package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintStream, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * This tool will find all pairs above a cutoff in a data table
 *
 * Created by pjvan_thof on 21-9-16.
 */
object FindOverlapMatch extends ToolCommand {

  case class Args(inputMetrics: File = null,
                  outputFile: Option[File] = None,
                  cutoff: Double = 0.0,
                  filterSameNames: Boolean = true) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputMetrics = x)
    } text "Input should be a table where the first row and column have the ID's, those can be different"
    opt[File]('o', "output") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "default to stdout"
    opt[Double]('c', "cutoff") required () unbounded () valueName "<value>" action { (x, c) =>
      c.copy(cutoff = x)
    } text "minimum value to report it as pair"
    opt[Unit]("use_same_names") unbounded () valueName "<value>" action { (x, c) =>
      c.copy(filterSameNames = false)
    } text "Do not compare samples with the same name"

  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val reader = Source.fromFile(cmdArgs.inputMetrics)

    val data = reader.getLines().map(_.split("\t")).toArray

    val samplesColumnHeader = data.head.zipWithIndex.tail
    val samplesRowHeader = data.map(_.head).zipWithIndex.tail

    var overlap = 0
    var multiOverlap = 0
    var noOverlap = 0

    val writer = cmdArgs.outputFile match {
      case Some(file) => new PrintStream(file)
      case _ => sys.process.stdout
    }

    for (i1 <- samplesColumnHeader) {
      val buffer = ListBuffer[(String, Double)]()
      for (i2 <- samplesRowHeader) {
        val value = data(i1._2)(i2._2).toDouble
        if (value >= cmdArgs.cutoff && (!cmdArgs.filterSameNames || i1._2 != i2._2)) {
          buffer.+=((i2._1, value))
        }
      }
      if (buffer.nonEmpty) overlap += 1
      else noOverlap += 1
      if (buffer.size > 1) multiOverlap += 1

      writer.println(s"${i1._1}\t${buffer.mkString("\t")}")
    }
    logger.info(s"$overlap found")
    logger.info(s"no $noOverlap found")
    logger.info(s"multi $multiOverlap found")
    writer.close()
  }
}
