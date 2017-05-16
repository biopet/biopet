package nl.lumc.sasc.biopet.utils.annotation

import java.io.File

import picard.annotation.Gene

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 15/05/2017.
  */
object GtfToRefflat {
  def readFile(gtfFile: File) = {
    val reader = Source.fromFile(gtfFile)

    val geneBuffer: mutable.Map[String, Gene] = mutable.Map()
    val transcriptBuffer: mutable.Map[String, mutable.Map[String, Gene#Transcript]] = mutable.Map()
    val exonBuffer: mutable.Map[(String, String), ListBuffer[Gene#Transcript#Exon]] = mutable.Map()

    var count = 0

    reader.getLines()
      .filter(_.startsWith("#"))
      .map(Feature.fromLine)
      .filter(f => f.feature == "gene" || f.feature == "transcript" || f.feature == "exon")
      .foreach { feature =>
        val geneId = feature.attributes.get("gene_id") match {
          case Some(id) => id
          case _ => throw new IllegalArgumentException(s"Feature should have a gene_id, $feature")
        }
        def transcriptId = feature.attributes.get("transcript_id") match {
          case Some(id) => id
          case _ => throw new IllegalArgumentException(s"Feature should have a transcript_id, $feature")
        }
        feature.feature match {
          case "gene" =>
            if (geneBuffer.contains(geneId)) throw new IllegalArgumentException(s"Gene '$geneId' is found twice in $gtfFile")
            geneBuffer += geneId -> new Gene(feature.contig, feature.start, feature.end, !feature.strand.getOrElse(true), geneId)
            val transcripts = transcriptBuffer.remove(geneId).flatMap(_.values)
            transcripts.foreach { t =>
              val newT = geneBuffer(geneId).addTranscript(t.name, t.transcriptionStart, t.transcriptionEnd, t.codingStart, t.codingEnd, t.exons.length)
              t.exons.foreach(e => newT.addExon(e.start, e.end))
            }
          case "transcript" =>
            val id = transcriptId
            val exons = exonBuffer.remove((geneId, id)).getOrElse(Nil).toArray
            if (geneBuffer.contains(geneId)) {
              if (geneBuffer(geneId).exists(_.name == id)) throw new IllegalArgumentException(s"Transcript '$id' is found twice in $gtfFile")
              geneBuffer(geneId).addTranscript(id, feature.start, feature.end, feature.start, feature.end, exons.length)
              geneBuffer(geneId).find(_.name == id).foreach(t => exons.foreach(e => t.addExon(e.start, e.end)))
            } else { // Gene does not exist yet
              if (!transcriptBuffer.contains(geneId)) transcriptBuffer += geneId -> mutable.Map()
              if (transcriptBuffer(geneId).contains(id)) throw new IllegalArgumentException(s"Transcript '$id' is found twice in $gtfFile")
              transcriptBuffer(geneId) += id -> new Gene#Transcript(id, feature.start, feature.end, feature.start, feature.end, exons.length)
              exons.foreach(e => transcriptBuffer(geneId)(id).addExon(e.start, e.end))
            }
          case "exon" =>
            val id = transcriptId
            geneBuffer.get(geneId).flatMap(_.find(_.name == id)) match {
              case Some(t) => t.addExon(feature.start, feature.end)
              case _ =>
                val exon = new Gene#Transcript#Exon(feature.start, feature.end)
                if (!exonBuffer.contains((geneId, id))) exonBuffer += (geneId, id) -> ListBuffer(exon)
                else exonBuffer(geneId, id) += exon
            }
          case _ => //TODO: count not used features
        }
        count += 1
      }

    reader.close()

    for (line <- reader.getLines().filter(_.startsWith("#"))) yield {
      val feature = Feature.fromLine(line)
    }
  }
}
