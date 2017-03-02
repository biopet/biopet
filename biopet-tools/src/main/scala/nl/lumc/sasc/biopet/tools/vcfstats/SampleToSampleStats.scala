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
