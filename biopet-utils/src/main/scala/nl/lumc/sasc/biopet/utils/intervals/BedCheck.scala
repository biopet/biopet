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
package nl.lumc.sasc.biopet.utils.intervals

import java.io.File

import nl.lumc.sasc.biopet.utils.Logging

import scala.collection.mutable

/**
  * Created by pjvanthof on 14/05/16.
  */
object BedCheck {
  private val cache: mutable.Set[(File, File)] = mutable.Set()

  def checkBedFileToReference(bedFile: File,
                              reference: File,
                              biopetError: Boolean = false,
                              ignoreCache: Boolean = false): Unit = {
    if (ignoreCache || !cache.contains((bedFile, reference))) {
      cache.add((bedFile, reference))
      val bedrecords = BedRecordList.fromFile(bedFile)
      if (biopetError) {
        try {
          bedrecords.validateContigs(reference)
        } catch {
          case e: IllegalArgumentException =>
            Logging.addError(e.getMessage + s", Bedfile: $bedFile")
        }
      } else bedrecords.validateContigs(reference)
    }
  }
}
