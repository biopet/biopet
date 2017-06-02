package nl.lumc.sasc.biopet.tools

import nl.lumc.sasc.biopet.utils.{FastaUtils, ToolCommand}
import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.annotation.{Exon, Feature, Gene, Transcript}

import scala.collection.mutable
import scala.io.Source

/**
  * Created by pjvanthof on 16/05/2017.
  */
object GtfToRefflat extends ToolCommand {

  case class Args(refFlat: File = null, gtfFile: File = null, referenceFasta: Option[File] = None)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('r', "refFlat") required () valueName "<file>" action { (x, c) =>
      c.copy(refFlat = x)
    } text "Input refFlat file. Mandatory"
    opt[File]('g', "gtfFile") required () valueName "<file>" action { (x, c) =>
      c.copy(gtfFile = x)
    } text "Output gtf file. Mandatory"
    opt[File]('R', "referenceFasta") valueName "<file>" action { (x, c) =>
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

    val referenceDict = referenceFasta.map(file => FastaUtils.getCachedDict(file))

    val genesFeatures: mutable.Map[Option[String], List[Feature]] = mutable.Map()
    logger.info("Reading gtf file")
    reader
      .getLines()
      .filter(!_.startsWith("#"))
      .foreach { line =>
        val feature = Feature.fromLine(line)
        referenceDict.foreach(dict =>
          require(dict.getSequence(feature.contig) != null,
                  s"Contig '${feature.contig}' does not exist on reference"))
        featureBuffer += feature.feature -> (featureBuffer.getOrElse(feature.feature, 0) + 1)
        val geneId = feature.attributes.get("gene_id")
        genesFeatures(geneId) = feature :: genesFeatures.getOrElse(geneId, Nil)
      }

      val genes = genesFeatures.map {
        case (geneId, gtfFeatures) =>
          val gtfGene = gtfFeatures.find(_.feature == "gene").get
          val gtfTranscripts = gtfFeatures.groupBy(_.attributes.get("transcript_id"))
          val transcripts =
            for ((transcriptId, features) <- gtfTranscripts if transcriptId.isDefined) yield {
              val groupedFeatures = features.groupBy(_.feature)
              val exons = groupedFeatures
                .getOrElse("exon", Nil)
                .sortBy(_.start)
                .map(x => Exon(x.start, x.end))
              val gtfTranscript = groupedFeatures("transcript").head
              val cdsFeatures =
                groupedFeatures.get("CDS").map(_.flatMap(x => List(x.start, x.end)))
              val startFeatures =
                groupedFeatures.get("start_codon").map(_.flatMap(x => List(x.start, x.end)))
              val stopFeatures =
                groupedFeatures.get("stop_codon").map(_.flatMap(x => List(x.start, x.end)))
              val codingLocations = startFeatures
                .getOrElse(cdsFeatures.getOrElse(Nil)) ::: stopFeatures.getOrElse(
                cdsFeatures.getOrElse(Nil))
              val codingStart =
                if (codingLocations.isEmpty) None
                else Some(codingLocations.min - 1)
              val codingEnd =
                if (codingLocations.isEmpty) None
                else Some(codingLocations.max)
              Transcript(transcriptId.get,
                         gtfTranscript.start,
                         gtfTranscript.end,
                         codingStart.getOrElse(gtfTranscript.end),
                         codingEnd.getOrElse(gtfTranscript.end),
                         exons)
            }
          Gene(geneId.get,
               gtfGene.contig,
               gtfGene.start,
               gtfGene.end,
               gtfGene.strand.getOrElse(true),
               transcripts.toList)
      }

    featureBuffer.foreach { case (k, c) => logger.info(s"$k\t$c") }

    val writer = new PrintWriter(refflatFile)

    for {
      gene <- genes
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
        transcript.codingStart.toString, //TODO: check if this is correct
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
