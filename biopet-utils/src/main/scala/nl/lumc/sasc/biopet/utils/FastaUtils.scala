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
  def clearCache() = dictCache = Map()

  /**
   * This method will get a dict from the fasta file. If it's already in the cache file will not opened again.
   *
   * @param fastaFile Fasta file
   * @return sequence dict
   */
  def getCachedDict(fastaFile: File): SAMSequenceDictionary = {
    if (!dictCache.contains(fastaFile)) dictCache += fastaFile -> getDictFromFasta(fastaFile)
    dictCache(fastaFile)
  }
}
