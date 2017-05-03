package nl.lumc.sasc.biopet.tools.refflatstats

/**
  * Created by pjvanthof on 01/05/2017.
  */
case class GeneStats(name: String,
                     contig: String,
                     start: Int,
                     end: Int,
                     totalGc: Double,
                     exonGc: Double,
                     intronGc: Option[Double],
                     exonLength: Int,
                     transcripts: Array[TranscriptStats]) {
  def length: Int = end - start
}
