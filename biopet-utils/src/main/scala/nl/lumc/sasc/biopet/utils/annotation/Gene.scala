package nl.lumc.sasc.biopet.utils.annotation

/**
  * Created by pjvanthof on 16/05/2017.
  */
case class Gene(name: String,
                contig: String,
                start: Int,
                end: Int,
                strand: Boolean,
                transcripts: List[Transcript])

case class Transcript(name: String,
                      transcriptionStart: Int,
                      transcriptionEnd: Int,
                      codingStart: Int,
                      codingEnd: Int,
                      exons: List[Exon])

case class Exon(start: Int, end: Int)
