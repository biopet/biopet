/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import scala.io.Source

import org.broadinstitute.gatk.utils.commandline.{ Input }


import argonaut._, Argonaut._
import scalaz._, Scalaz._

class Sickle(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Sickle(root) {
  @Input(doc = "qualityType file", required = false)
  var qualityTypeFile: File = _

  override def beforeCmd {
    super.beforeCmd
    qualityType = getQualityTypeFromFile
  }
  
  def getQualityTypeFromFile: String = {
    if (qualityType == null && qualityTypeFile != null) {
      if (qualityTypeFile.exists()) {
        for (line <- Source.fromFile(qualityTypeFile).getLines) {
          var s: String = line.substring(0, line.lastIndexOf("\t"))
          return s
        }
      } else logger.warn("File : " + qualityTypeFile + " does not exist")
    }
    return null
  }

  def getSummary: Json = {
    return jNull
  }
}

object Sickle {
  def mergeSummarys(jsons: List[Json]): Json = {
    return jNull
  }
}