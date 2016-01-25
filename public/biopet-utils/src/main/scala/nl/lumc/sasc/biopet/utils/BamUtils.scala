package nl.lumc.sasc.biopet.utils

import java.io.File

import htsjdk.samtools.{SamReader, SamReaderFactory}

import scala.collection.JavaConversions._

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
   * @return
   */
  def sampleBamMap(bamFiles: List[File]): Map[String, File] = {
    val temp = bamFiles.map { file =>
      val inputSam = SamReaderFactory.makeDefault.open(file)
      val samples = inputSam.getFileHeader.getReadGroups.map(_.getSample).distinct
      if (samples.size == 1) samples.head -> file
      else throw new IllegalArgumentException("Bam contains multiple sample IDs: " + file)
    }
    if (temp.map(_._1).distinct.size != temp.size) throw new IllegalArgumentException("Samples has been found twice")
    temp.toMap
  }

  /**
    * Estimate the insertsize for each bam file and return Map[<sampleName>, <insertSize>]
    *
    * @param bamFiles input bam files
    * @return
    */
  def sampleBamInsertSize(bamFiles: List[File]): Map[File, Float] = bamFiles.map { file =>

    val inputSam: SamReader = SamReaderFactory.makeDefault.open(file)

    val baminsertsizes = inputSam.getFileHeader.getSequenceDictionary.getSequences.map {
      contig =>
        val insertsizes: Iterator[Int] = for {
          read <- inputSam.query( contig.getSequenceName, 1, contig.getSequenceLength, true) //.toStream.slice(0, 100).toList
          insertsize = read.getInferredInsertSize
          paired = read.getReadPairedFlag
          bothMapped = (read.getReadUnmappedFlag == false) && (read.getMateUnmappedFlag == false)
          if paired && bothMapped
        } yield {
          insertsize
        }
        val contigInsertSize = insertsizes.foldLeft((0.0,0))((t, r) => (t._1 + r, t._2 +1))
        contigInsertSize._1 / contigInsertSize._2
    }.foldLeft((0.0,0))((t, r) => (t._1 + r, t._2 +1))

    file -> baminsertsizes._1 / baminsertsizes._2
  }

}
