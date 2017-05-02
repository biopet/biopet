package nl.lumc.sasc.biopet.tools.refflatstats

/**
  * Created by pjvanthof on 01/05/2017.
  */
case class TranscriptStats(name: String,
                           totalGc: Double,
                           exonGc: Double,
                           intronGc: Option[Double],
                           exons: Array[RegionStats],
                           introns: Array[RegionStats]) {}
