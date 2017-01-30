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
package nl.lumc.sasc.biopet.utils

import java.io.File

import htsjdk.samtools.SAMSequenceDictionary
import htsjdk.samtools.reference.FastaSequenceFile

/**
 * Created by pjvan_thof on 25-10-16.
 */
object FastaUtils {
  /**
   * This method will get a dict from the fasta file. This will not use the cache
   *
   * @param fastaFile Fasta file
   * @return sequence dict
   */
  def getDictFromFasta(fastaFile: File): SAMSequenceDictionary = {
    val referenceFile = new FastaSequenceFile(fastaFile, true)
    val dict = referenceFile.getSequenceDictionary
    referenceFile.close()
    dictCache += fastaFile -> dict
    dict
  }

  private var dictCache: Map[File, SAMSequenceDictionary] = Map()

  /** This will clear the dict cache */
  def clearCache() = {
    dictCache = Map()
  }

  /**
   * This method will get a dict from the fasta file. If it's already in the cache file will not opened again.
   *
   * @param fastaFile Fasta file
   * @return sequence dict
   */
  def getCachedDict(fastaFile: File): SAMSequenceDictionary = {
    if (!dictCache.contains(fastaFile)) getDictFromFasta(fastaFile)
    else dictCache(fastaFile)
  }
}
