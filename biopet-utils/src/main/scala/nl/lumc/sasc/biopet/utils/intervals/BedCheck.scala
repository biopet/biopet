package nl.lumc.sasc.biopet.utils.intervals

import java.io.File
import scala.collection.mutable.Set

import nl.lumc.sasc.biopet.utils.Logging

/**
  * Created by pjvanthof on 14/05/16.
  */
object BedCheck {
  private val cache: Set[(File, File)] = Set()

  def checkBedFileToReference(bedFile: File, reference: File, biopetError: Boolean = false, ignoreCache: Boolean = false): Unit = {
    if (ignoreCache || !cache.contains((bedFile, reference))) {
      cache.add((bedFile, reference))
      val bedrecords = BedRecordList.fromFile(bedFile)
      if (biopetError) {
        try {
          bedrecords.validateContigs(reference)
        } catch {
          case e: IllegalArgumentException => Logging.addError(e.getMessage + s", Bedfile: $bedFile")
        }
      } else bedrecords.validateContigs(reference)
    }
  }
}
