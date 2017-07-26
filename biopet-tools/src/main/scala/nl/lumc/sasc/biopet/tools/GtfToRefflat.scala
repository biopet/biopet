package nl.lumc.sasc.biopet.tools

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, FastaUtils, ToolCommand}
import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.annotation.{Exon, Feature, Gene, Transcript}

import scala.collection.mutable
import scala.io.Source

/**
  * Created by pjvanthof on 16/05/2017.
  */
object GtfToRefflat extends ToolCommand {

  case class Args(refFlat: File = null, gtfFile: File = null, referenceFasta: Option[File] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('r', "refFlat") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(refFlat = x)
    } text "Input refFlat file. Mandatory"
    opt[File]('g', "gtfFile") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(gtfFile = x)
    } text "Output gtf file. Mandatory"
    opt[File]('R', "referenceFasta") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = Some(x))
    } text "Reference file"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")
    gtfToRefflat(cmdArgs.gtfFile, cmdArgs.refFlat, cmdArgs.referenceFasta)
    logger.info("Done")
  }

  def gtfToRefflat(gtfFile: File, refflatFile: File, referenceFasta: Option[File] = None): Unit = {
    val reader = Source.fromFile(gtfFile)

    val featureBuffer: mutable.Map[String, Int] = mutable.Map()

    val genesBuffer: mutable.Map[String, Gene] = mutable.Map()
    val transcriptsBuffer: mutable.Map[String, List[Transcript]] = mutable.Map()
    val exonBuffer: mutable.Map[(String, String), List[Exon]] = mutable.Map()
    val codingBuffer: mutable.Map[(String, String), (Option[Int], Option[Int])] = mutable.Map()

    val referenceDict = referenceFasta.map(file => FastaUtils.getCachedDict(file))

    def updateCodingRegion(geneId: String,
                           transcriptId: String,
                           start: Option[Int] = None,
                           end: Option[Int] = None): Unit = {
      genesBuffer.get(geneId).flatMap(_.transcripts.find(_.name == transcriptId)) match {
        case Some(transcriptOld) =>
          val geneOld = genesBuffer(geneId)
          genesBuffer(geneId) = geneOld.copy(transcripts = updateTranscriptCodingRegion(
            transcriptOld,
            start,
            end) :: geneOld.transcripts.filter(_.name != transcriptOld.name))
        case _ =>
          transcriptsBuffer.get(geneId).flatMap(_.find(_.name == transcriptId)) match {
            case Some(transcriptOld) =>
              transcriptsBuffer(geneId) = updateTranscriptCodingRegion(transcriptOld, start, end) ::
                transcriptsBuffer(geneId).filter(_.name != transcriptId)
            case _ =>
              val old = codingBuffer.getOrElse((geneId, transcriptId), (None, None))
              codingBuffer((geneId, transcriptId)) =
                (start.map(Some(_)).getOrElse(old._1), end.map(Some(_)).getOrElse(old._2))
          }
      }
    }

    def updateTranscriptCodingRegion(transcript: Transcript,
                                     start: Option[Int] = None,
                                     end: Option[Int] = None): Transcript = {
      val newStart: Option[Int] = start match {
        case Some(x) =>
          transcript.codingStart match {
            case Some(y) if y < x => Some(y)
            case _ => Some(x)
          }
        case _ => transcript.codingStart
      }

      val newEnd: Option[Int] = end match {
        case Some(x) =>
          transcript.codingEnd match {
            case Some(y) if y > x => Some(y)
            case _ => Some(x)
          }
        case _ => transcript.codingEnd
      }

      transcript.copy(codingStart = newStart, codingEnd = newEnd)
    }

    logger.info("Reading gtf file")
    reader
      .getLines()
      .filter(!_.startsWith("#"))
      .foreach { line =>
        val feature = Feature.fromLine(line)
        referenceDict.foreach(
          dict =>
            require(dict.getSequence(feature.contig) != null,
                    s"Contig '${feature.contig}' does not exist on reference"))
        featureBuffer += feature.feature -> (featureBuffer.getOrElse(feature.feature, 0) + 1)
        lazy val geneId = feature.attributes("gene_id")
        lazy val transcriptId = feature.attributes("transcript_id")
        feature.feature match {
          case "gene" =>
            genesBuffer += geneId -> Gene(geneId,
                                          feature.contig,
                                          feature.minPosition,
                                          feature.maxPosition,
                                          feature.strand.get,
                                          transcriptsBuffer.remove(geneId).getOrElse(Nil))
          case "transcript" =>
            val coding = codingBuffer.remove((geneId, transcriptId)).getOrElse((None, None))
            val transcript = Transcript(transcriptId,
                                        feature.minPosition,
                                        feature.maxPosition,
                                        coding._1,
                                        coding._2,
                                        exonBuffer.remove(geneId, transcriptId).getOrElse(Nil))
            if (genesBuffer.contains(geneId)) {
              val oldGene = genesBuffer(geneId)
              genesBuffer(geneId) = oldGene.copy(transcripts = transcript :: oldGene.transcripts)
            } else
              transcriptsBuffer(geneId) = transcript :: transcriptsBuffer.getOrElse(geneId, Nil)
          case "exon" =>
            val exon = Exon(feature.minPosition, feature.maxPosition)
            genesBuffer.get(geneId).flatMap(_.transcripts.find(_.name == transcriptId)) match {
              case Some(transcriptOld) =>
                val geneOld = genesBuffer(geneId)
                genesBuffer(geneId) = geneOld.copy(
                  transcripts = transcriptOld
                    .copy(exons = exon :: transcriptOld.exons) :: geneOld.transcripts.filter(
                    _.name != transcriptOld.name))
              case _ =>
                transcriptsBuffer.get(geneId).flatMap(_.find(_.name == transcriptId)) match {
                  case Some(transcriptOld) =>
                    transcriptsBuffer(geneId) = transcriptOld.copy(
                      exons = exon :: transcriptOld.exons) ::
                      transcriptsBuffer(geneId).filter(_.name != transcriptId)
                  case _ =>
                    exonBuffer((geneId, transcriptId)) = exon :: exonBuffer.getOrElse(
                      (geneId, transcriptId),
                      Nil)
                }
            }
          case "stop_codon" if !feature.strand.contains(false) =>
            updateCodingRegion(geneId, transcriptId, end = Some(feature.end))
          case "stop_codon" =>
            updateCodingRegion(geneId, transcriptId, start = Some(feature.start - 1))
          case "start_codon" if !feature.strand.contains(false) =>
            updateCodingRegion(geneId, transcriptId, start = Some(feature.start - 1))
          case "start_codon" => updateCodingRegion(geneId, transcriptId, end = Some(feature.end))
          case "CDS" =>
            updateCodingRegion(geneId,
                               transcriptId,
                               start = Some(feature.start - 1),
                               end = Some(feature.end))
          case _ =>
        }
      }

    featureBuffer.foreach { case (k, c) => logger.info(s"$k\t$c") }

    val writer = new PrintWriter(refflatFile)

    for {
      (_, genes) <- genesBuffer.values.toList.groupBy(_.contig).toList.sortBy(_._1)
      gene <- genes.sortBy(_.start)
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
        transcript.codingStart
          .getOrElse(transcript.transcriptionEnd)
          .toString, //TODO: check if this is correct
        transcript.codingEnd
          .getOrElse(transcript.transcriptionEnd)
          .toString, //TODO: check if this is correct
        transcript.exons.length.toString,
        exons.map(_.start - 1).mkString("", ",", ","), //TODO: check if this is correct
        exons.map(_.end).mkString("", ",", ",")
      )
      writer.println(values.mkString("\t"))
    }

    writer.close()
  }
}
