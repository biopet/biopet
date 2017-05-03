package nl.lumc.sasc.biopet.tools.refflatstats

/**
  * Created by pjvanthof on 01/05/2017.
  */
case class RegionStats(start: Int, end: Int, gc: Double) {
  def length = end - start + 1
}
