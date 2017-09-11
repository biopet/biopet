/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools.vcfstats

import scala.collection.mutable

/**
  * class to store all sample relative stats
  *
  * @param genotypeStats Stores all genotype relative stats
  * @param sampleToSample Stores sample to sample compare stats
  */
case class SampleStats(genotypeStats: mutable.Map[String, mutable.Map[Any, Int]] = mutable.Map(),
                       sampleToSample: Array[SampleToSampleStats] = Array()) {

  /** Add an other class */
  def +=(other: SampleStats): Unit = {
    require(other.sampleToSample.size == this.sampleToSample.size)
    val zipped = this.sampleToSample.zip(other.sampleToSample).zipWithIndex
    for (((s1, s2), i) <- zipped) {
      s1 += s2
    }
    for ((field, fieldMap) <- other.genotypeStats) {
      val thisField = this.genotypeStats.get(field)
      if (thisField.isDefined) Stats.mergeStatsMap(thisField.get, fieldMap)
      else this.genotypeStats += field -> fieldMap
    }
  }
}
