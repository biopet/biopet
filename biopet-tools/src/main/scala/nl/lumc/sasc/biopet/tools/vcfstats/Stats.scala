package nl.lumc.sasc.biopet.tools.vcfstats

import scala.collection.mutable

/**
  * General stats class to store vcf stats
  *
  * @param generalStats Stores are general stats
  * @param samplesStats Stores all sample/genotype specific stats
  */
case class Stats(generalStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map(),
                 samplesStats: mutable.Map[String, SampleStats] = mutable.Map()) {
  /** Add an other class */
  def +=(other: Stats): Stats = {
    for ((key, value) <- other.samplesStats) {
      if (this.samplesStats.contains(key)) this.samplesStats(key) += value
      else this.samplesStats(key) = value
    }
    for ((chr, chrMap) <- other.generalStats; (field, fieldMap) <- chrMap) {
      if (!this.generalStats.contains(chr)) generalStats += (chr -> mutable.Map[String, mutable.Map[Any, Int]]())
      val thisField = this.generalStats(chr).get(field)
      if (thisField.isDefined) Stats.mergeStatsMap(thisField.get, fieldMap)
      else this.generalStats(chr) += field -> fieldMap
    }
    this
  }
}

object Stats {
  /** Merge m2 into m1 */
  def mergeStatsMap(m1: mutable.Map[Any, Int], m2: mutable.Map[Any, Int]): Unit = {
    for (key <- m2.keySet)
      m1(key) = m1.getOrElse(key, 0) + m2(key)
  }

  /** Merge m2 into m1 */
  def mergeNestedStatsMap(m1: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]],
                          m2: Map[String, Map[String, Map[Any, Int]]]): Unit = {
    for ((chr, chrMap) <- m2; (field, fieldMap) <- chrMap) {
      if (m1.contains(chr)) {
        if (m1(chr).contains(field)) {
          for ((key, value) <- fieldMap) {
            if (m1(chr)(field).contains(key)) m1(chr)(field)(key) += value
            else m1(chr)(field)(key) = value
          }
        } else m1(chr)(field) = mutable.Map(fieldMap.toList: _*)
      } else m1(chr) = mutable.Map(field -> mutable.Map(fieldMap.toList: _*))
    }
  }
}