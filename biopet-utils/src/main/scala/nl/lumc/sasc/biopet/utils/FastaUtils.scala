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
import htsjdk.samtools.reference.{FastaSequenceFile, IndexedFastaSequenceFile}

import scala.collection.JavaConversions._

import scala.io.Source

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
  def clearCache(): Unit = {
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

  /** This method returns the fraction of GC for a given region */
  def getSequenceGc(fastaFile: File, contig: String, start: Long, end: Long): Double = {
    getSequenceGc(new IndexedFastaSequenceFile(fastaFile), contig, start, end)
  }

  /** This method returns the fraction of GC for a given region */
  def getSequenceGc(referenceFile: IndexedFastaSequenceFile,
                    contig: String,
                    start: Long,
                    end: Long): Double = {
    require(referenceFile.isIndexed)
    val sequence = referenceFile.getSubsequenceAt(contig, start, end)
    val gcCount = sequence.getBaseString.toLowerCase.count(c => c == 'c' || c == 'g')
    val atCount = sequence.getBaseString.toLowerCase.count(c => c == 'a' || c == 't')
    val gc = gcCount.toDouble / (gcCount + atCount)
    gc
  }

  def readContigMap(file: File): Map[String, Set[String]] = {
    val reader = Source.fromFile(file)
    val map = reader
      .getLines()
      .filter(!_.startsWith("#"))
      .map { line =>
        val columns = line.split("\t")
        val refContig = columns(0)
        val alternativeNames = columns(1).split(";").toSet
        refContig -> alternativeNames
      }
      .toMap
    reader.close()
    map
  }

  def readContigMapReverse(file: File): Map[String, String] = {
    readContigMap(file).flatMap(x => x._2.map(y => y -> x._1))
  }

  def rebuildContigMap(contigMap: File, referenceFasta: File): Map[String, Set[String]] = {
    rebuildContigMap(contigMap, getDictFromFasta(referenceFasta))
  }

  def rebuildContigMap(contigMap: File, dict: SAMSequenceDictionary): Map[String, Set[String]] = {
    val map = readContigMap(contigMap)
    (for (contig <- dict.getSequences) yield {
      val name = contig.getSequenceName
      val set = map
        .filter(x => x._1 == name || x._2.contains(name))
        .flatMap(x => x._2 + x._1)
        .filter(_ != name)
        .toSet
      name -> set
    }).toMap
  }
}
