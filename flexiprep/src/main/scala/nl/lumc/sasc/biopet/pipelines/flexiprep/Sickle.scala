package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.config.Configurable

import argonaut._, Argonaut._
import scalaz._, Scalaz._

class Sickle(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Sickle(root) {
  def getSummary: Json = {
    return jNull
  }
}

object Sickle {
  def mergeSummarys(jsons: List[Json]): Json = {
    return jNull
  }
}