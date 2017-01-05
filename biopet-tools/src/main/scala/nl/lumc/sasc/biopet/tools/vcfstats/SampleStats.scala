package nl.lumc.sasc.biopet.tools.vcfstats

import scala.collection.mutable

/**
 * class to store all sample relative stats
 *
 * @param genotypeStats Stores all genotype relative stats
 * @param sampleToSample Stores sample to sample compare stats
 */
case class SampleStats(genotypeStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map(),
                       sampleToSample: mutable.Map[String, SampleToSampleStats] = mutable.Map()) {
  /** Add an other class */
  def +=(other: SampleStats): Unit = {
    for ((key, value) <- other.sampleToSample) {
      if (this.sampleToSample.contains(key)) this.sampleToSample(key) += value
      else this.sampleToSample(key) = value
    }
    for ((chr, chrMap) <- other.genotypeStats; (field, fieldMap) <- chrMap) {
      if (!this.genotypeStats.contains(chr)) genotypeStats += (chr -> mutable.Map[String, mutable.Map[Any, Int]]())
      val thisField = this.genotypeStats(chr).get(field)
      if (thisField.isDefined) Stats.mergeStatsMap(thisField.get, fieldMap)
      else this.genotypeStats(chr) += field -> fieldMap
    }
  }
}
