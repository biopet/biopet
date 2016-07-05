package nl.lumc.sasc.biopet.tools.bamstats

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats() {

  var totalReads = 0L

  def +(other: Stats): Stats = {
    this.totalReads += other.totalReads
    this
  }
}
