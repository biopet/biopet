package nl.lumc.sasc.biopet.utils

import java.io.File

import htsjdk.samtools.SamReaderFactory

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
      else if (samples.size > 1) throw new IllegalArgumentException("Bam contains multiple sample IDs: " + file)
      else throw new IllegalArgumentException("Bam does not contain sample ID or have no readgroups defined: " + file)
    }
    if (temp.map(_._1).distinct.size != temp.size) throw new IllegalArgumentException("Samples has been found twice")
    temp.toMap
  }
}
