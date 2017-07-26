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

import htsjdk.samtools.{SAMSequenceDictionary, SamReader, SamReaderFactory}
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.parallel.immutable

/**
  * Created by pjvan_thof on 11/19/15.
  */
object BamUtils {

  /**
    * This method will convert a list of bam files to a Map[<sampleName>, <bamFile>]
    *
    * Each sample may only be once in the list
    *
    * @throws IllegalArgumentException
    * @param bamFiles input bam files
    * @return Map of sample bam files
    */
  def sampleBamMap(bamFiles: List[File]): Map[String, File] = {
    val temp = bamFiles.map { file =>
      val inputSam = SamReaderFactory.makeDefault.open(file)
      val samples = inputSam.getFileHeader.getReadGroups.map(_.getSample).distinct
      if (samples.size == 1) samples.head -> file
      else if (samples.size > 1)
        throw new IllegalArgumentException("Bam contains multiple sample IDs: " + file)
      else
        throw new IllegalArgumentException(
          "Bam does not contain sample ID or have no readgroups defined: " + file)
    }
    if (temp.map(_._1).distinct.size != temp.size)
      throw new IllegalArgumentException("Samples has been found twice")
    temp.toMap
  }

  /**
    * Estimate the insertsize of fragments within the given contig.
    * Uses the properly paired reads according to flags set by the aligner
    *
    * @param inputBam input bam file
    * @param contig contig to scan for
    * @param end position to stop scanning
    * @return Int with insertsize for this contig
    */
  def contigInsertSize(inputBam: File,
                       contig: String,
                       start: Int,
                       end: Int,
                       samplingSize: Int = 10000,
                       binSize: Int = 1000000): Option[Int] = {

    // create a bedList to devide the contig into multiple pieces
    val insertSizesOnAllFragments = BedRecordList
      .fromList(Seq(BedRecord(contig, start, end)))
      .scatter(binSize)
      .flatten
      .par
      .flatMap({ bedRecord =>
        // for each scatter, open the bamfile for this specific region-query
        val inputSam: SamReader = SamReaderFactory.makeDefault.open(inputBam)
        val samIterator = inputSam.query(bedRecord.chr, bedRecord.start, bedRecord.end, true)

        val counts: mutable.Map[Int, Int] = mutable.Map()

        for (_ <- 0 until samplingSize if samIterator.hasNext) {
          val rec = samIterator.next()
          val isPaired = rec.getReadPairedFlag
          val minQ10 = rec.getMappingQuality >= 10
          val pairOnSameContig = rec.getContig == rec.getMateReferenceName

          if (isPaired && minQ10 && pairOnSameContig) {
            val insertSize = rec.getInferredInsertSize.abs
            counts(insertSize) = counts.getOrElse(insertSize, 0) + 1
          }
        }

        counts.keys.size match {
          case 1 => Some(counts.keys.head)
          case 0 => None
          case _ =>
            Some(counts.foldLeft(0)((old, observation) => {
              observation match {
                case (insertSize: Int, observations: Int) =>
                  (old + (insertSize * observations)) / (observations + 1)
                case _ => 0
              }
            }))
        }
      })

    insertSizesOnAllFragments.size match {
      case 1 => Some(insertSizesOnAllFragments.head)
      case 0 => None
      case _ =>
        Some(insertSizesOnAllFragments.foldLeft(0)((old, observation) => {
          (old + observation) / 2
        }))

    }
  }

  /**
    * Estimate the insertsize for one single bamfile and return the insertsize
    *
    * @param bamFile bamfile to estimate average insertsize from
    * @return
    */
  def sampleBamInsertSize(bamFile: File, samplingSize: Int = 10000, binSize: Int = 1000000): Int = {
    val inputSam: SamReader = SamReaderFactory.makeDefault.open(bamFile)
    val bamInsertSizes = inputSam.getFileHeader.getSequenceDictionary.getSequences.par
      .map({ contig =>
        BamUtils.contigInsertSize(bamFile,
                                  contig.getSequenceName,
                                  1,
                                  contig.getSequenceLength,
                                  samplingSize,
                                  binSize)
      })
      .toList
    val counts = bamInsertSizes.flatten

    // avoid division by zero
    if (counts.nonEmpty) counts.sum / counts.size
    else 0
  }

  /**
    * Estimate the insertsize for each bam file and return Map[<sampleBamFile>, <insertSize>]
    *
    * @param bamFiles input bam files
    * @return
    */
  def sampleBamsInsertSize(bamFiles: List[File],
                           samplingSize: Int = 10000,
                           binSize: Int = 1000000): immutable.ParMap[File, Int] =
    bamFiles.par.map { bamFile =>
      bamFile -> sampleBamInsertSize(bamFile, samplingSize, binSize)
    }.toMap

  /** This class will add functionality to [[SAMSequenceDictionary]] */
  implicit class SamDictCheck(samDicts: SAMSequenceDictionary) {

    /**
      * This method will check if all contig and sizes are the same without looking at the order of the contigs
      *
      * @throws AssertionError
      * @param that Dict to compare to
      * @param ignoreOrder When true the order of the contig does not matter
      */
    def assertSameDictionary(that: SAMSequenceDictionary, ignoreOrder: Boolean): Unit = {
      if (ignoreOrder) {
        assert(samDicts.getReferenceLength == that.getReferenceLength)
        val thisContigNames =
          samDicts.getSequences.map(x => (x.getSequenceName, x.getSequenceLength)).sorted.toSet
        assert(
          thisContigNames == that.getSequences
            .map(x => (x.getSequenceName, x.getSequenceLength))
            .sorted
            .toSet)
      } else samDicts.assertSameDictionary(that)
    }
  }
}
