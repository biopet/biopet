package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.annotation.Feature
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.io.Source

object AddGenesAndIntronsToGtf extends ToolCommand {
  case class Args(input: File = null, output: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    } text "Input gtf file. Mandatory"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    } text "Output gtf file. Mandatory"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
    : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val reader = Source.fromFile(cmdArgs.input)

    val genes = reader.getLines()
      .map(Feature.fromLine)
      .toTraversable
      .groupBy(_.attributes.get("gene_id"))

    val writer = new PrintWriter(cmdArgs.output)

    for ((geneN, features) <- genes) {
      geneN match {
        case Some(geneName) =>
          val (geneStart, geneEnd) = features.foldLeft((features.head.minPosition, features.head.maxPosition)) { case (a, b) =>
            (if (a._1 < b.minPosition) a._1 else b.minPosition,
            if (a._2 > b.maxPosition) a._2 else b.maxPosition)
          }
          val gene = Feature(features.head.contig, features.head.source, "gene", geneStart, geneEnd, None, features.head.strand, None, Map("gene_id" -> geneName))
          writer.println(gene.asGtfLine)
          val transcriptFeatures = features.groupBy(_.attributes.get("transcript_id"))

          for ((transcriptN, transFeatures) <- transcriptFeatures) {
            transcriptN match {
              case Some(transcriptName) =>
                val (transStart, transEnd) = transFeatures.foldLeft((transFeatures.head.minPosition, transFeatures.head.maxPosition)) { case (a, b) =>
                  (if (a._1 < b.minPosition) a._1 else b.minPosition,
                    if (a._2 > b.maxPosition) a._2 else b.maxPosition)
                }
                val transcript = Feature(features.head.contig, features.head.source, "gene", geneStart, geneEnd, None, features.head.strand, None, Map("gene_id" -> geneName, "transcript_id" -> transcriptName))
                writer.println(transcript.asGtfLine)

              case _ => transFeatures.foreach(f => writer.println(f.asGtfLine))
            }
            transFeatures.foreach(f => writer.println(f.asGtfLine))
          }
        case _ => features.foreach(f => writer.println(f.asGtfLine))
      }
    }

    writer.close()

    logger.info("Done")
  }

}
