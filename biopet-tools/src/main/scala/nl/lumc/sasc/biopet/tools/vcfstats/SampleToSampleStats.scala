package nl.lumc.sasc.biopet.tools.vcfstats

/**
  * Class to store sample to sample compare stats
  * @param genotypeOverlap Number of genotypes match with other sample
  * @param alleleOverlap Number of alleles also found in other sample
  */
case class SampleToSampleStats(var genotypeOverlap: Int = 0,
                               var alleleOverlap: Int = 0) {
  /** Add an other class */
  def +=(other: SampleToSampleStats) {
    this.genotypeOverlap += other.genotypeOverlap
    this.alleleOverlap += other.alleleOverlap
  }
}
