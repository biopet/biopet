package nl.lumc.sasc.biopet.tools

import nl.lumc.sasc.biopet.utils.ToolCommand
import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.annotation.{Exon, Feature, Gene, Transcript}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by pjvanthof on 16/05/2017.
  */
object GtfToRefflat extends ToolCommand {

  case class Args(refFlat: File = null, gtfFile: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('r', "refFlat") required () valueName "<file>" action { (x, c) =>
      c.copy(refFlat = x)
    } text "Input refFlat file. Mandatory"
    opt[File]('g', "gtfFile") required () valueName "<file>" action { (x, c) =>
      c.copy(gtfFile = x)
    } text "Output gtf file. Mandatory"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")
    gtfToRefflat(cmdArgs.gtfFile, cmdArgs.refFlat)
    logger.info("Done")
  }

  def gtfToRefflat(gtfFile: File, refflatFile: File): Unit = {
    val reader = Source.fromFile(gtfFile)

    val geneBuffer: mutable.Map[String, Gene] = mutable.Map()
    val transcriptBuffer: mutable.Map[String, mutable.Map[String, Transcript]] = mutable.Map()
    val exonBuffer: mutable.Map[(String, String), ListBuffer[Exon]] = mutable.Map()

    val featureBuffer: mutable.Map[String, Int] = mutable.Map()

    reader
      .getLines()
      .filter(!_.startsWith("#"))
      .map(Feature.fromLine)
      .foreach { feature =>
        featureBuffer += feature.feature -> (featureBuffer.getOrElse(feature.feature, 0) + 1)
        val geneId = feature.attributes.get("gene_id") match {
          case Some(id) => id
          case _ => throw new IllegalArgumentException(s"Feature should have a gene_id, $feature")
        }

        def transcriptId = feature.attributes.get("transcript_id") match {
          case Some(id) => id
          case _ =>
            throw new IllegalArgumentException(s"Feature should have a transcript_id, $feature")
        }

        feature.feature match {
          case "gene" =>
            if (geneBuffer.contains(geneId))
              throw new IllegalArgumentException(s"Gene '$geneId' is found twice in $gtfFile")
            geneBuffer += geneId -> Gene(geneId,
                                         feature.contig,
                                         feature.start,
                                         feature.end,
                                         feature.strand.getOrElse(true),
                                         transcriptBuffer.remove(geneId).toList.flatMap(_.values))
          case "transcript" =>
            val id = transcriptId
            val exons = exonBuffer.remove((geneId, id)).getOrElse(Nil).toList
            val transcript =
              Transcript(id, feature.start, feature.end, feature.start, feature.end, exons)
            if (geneBuffer.contains(geneId)) {
              if (geneBuffer(geneId).transcripts.exists(_.name == id))
                throw new IllegalArgumentException(s"Transcript '$id' is found twice in $gtfFile")
              val gene = geneBuffer(geneId)
              geneBuffer(geneId) = gene.copy(transcripts = gene.transcripts ::: transcript :: Nil)
            } else { // Gene does not exist yet
              if (!transcriptBuffer.contains(geneId)) transcriptBuffer += geneId -> mutable.Map()
              if (transcriptBuffer(geneId).contains(id))
                throw new IllegalArgumentException(s"Transcript '$id' is found twice in $gtfFile")
              transcriptBuffer(geneId) += id -> transcript
            }
          case "exon" =>
            val id = transcriptId
            val exon = Exon(feature.start, feature.end)
            (geneBuffer.get(geneId).flatMap(_.transcripts.find(_.name == id)),
             transcriptBuffer.get(geneId).flatMap(_.get(id))) match {
              case (Some(transcript), _) =>
                val gene = geneBuffer(geneId)
                geneBuffer(geneId) = gene.copy(
                  transcripts = transcript
                    .copy(exons = exon :: transcript.exons) :: gene.transcripts.filter(
                    _.name != transcript.name))
              case (None, Some(transcript)) =>
                transcriptBuffer(geneId)(id) = transcript.copy(exons = exon :: transcript.exons)
              case _ =>
                if (!exonBuffer.contains((geneId, id)))
                  exonBuffer += (geneId, id) -> ListBuffer(exon)
                else exonBuffer(geneId, id) += exon
            }
          case _ =>
        }
      }
    reader.close()

    featureBuffer.foreach { case (k, c) => logger.info(s"$k\t$c") }

    val writer = new PrintWriter(refflatFile)

    for {
      gene <- geneBuffer.values
      transcript <- gene.transcripts
    } {
      val exons = transcript.exons.sortBy(_.start)
      val values = List(
        gene.name,
        transcript.name,
        gene.contig,
        if (gene.strand) "+" else "-",
        (transcript.transcriptionStart - 1).toString, //TODO: check if this is correct
        transcript.transcriptionEnd.toString,
        (transcript.codingStart - 1).toString, //TODO: check if this is correct
        transcript.codingEnd.toString, //TODO: check if this is correct
        transcript.exons.length.toString,
        exons.map(_.start - 1).mkString("", ",", ","), //TODO: check if this is correct
        exons.map(_.end).mkString("", ",", ",")
      )
      writer.println(values.mkString("\t"))
    }

    writer.close()
  }

}
