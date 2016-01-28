package nl.lumc.sasc.biopet.utils

import java.io.File

import htsjdk.samtools.{ SamReader, SamReaderFactory }

import scala.collection.JavaConversions._
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
   * Estimate the insertsize of fragments within the given contig.
   * Uses the properly paired reads according to flags set by the aligner
   *
   * @param inputBam input bam file
   * @param contig contig to scan for
   * @param end postion to stop scanning
   * @return Int with insertsize for this contig
   */
  def contigInsertSize(inputBam: File, contig: String, end: Int): Option[Int] = {
    val inputSam: SamReader = SamReaderFactory.makeDefault.open(inputBam)
    val samIterator = inputSam.query(contig, 1, end, true)
    val insertsizes: List[Int] = (for {
      read <- samIterator.toStream.takeWhile(rec => {
        rec.getReadPairedFlag && ((rec.getReadUnmappedFlag == false) && (rec.getMateUnmappedFlag == false) && rec.getProperPairFlag)
      }).take(100000)
    } yield {
      read.getInferredInsertSize.asInstanceOf[Int].abs
    })(collection.breakOut)
    val cti = insertsizes.foldLeft((0.0, 0))((t, r) => (t._1 + r, t._2 + 1))

    samIterator.close()
    inputSam.close()
    val ret = if (cti._2 == 0) None else Some((cti._1 / cti._2).toInt)
    ret
  }

  /**
   * Estimate the insertsize for each bam file and return Map[<sampleName>, <insertSize>]
   *
   * @param bamFiles input bam files
   * @return
   */
  def sampleBamInsertSize(bamFiles: List[File]): immutable.ParMap[File, Int] = bamFiles.par.map { bamFile =>
    val inputSam: SamReader = SamReaderFactory.makeDefault.open(bamFile)
    val baminsertsizes = inputSam.getFileHeader.getSequenceDictionary.getSequences.par.map({
      contig => BamUtils.contigInsertSize(bamFile, contig.getSequenceName, contig.getSequenceLength)
    }).toList
    val sum = baminsertsizes.flatMap(x => x).reduceLeft(_ + _)
    val n = baminsertsizes.flatMap(x => x).size
    bamFile -> (sum / n).toInt
  }.toMap

}
