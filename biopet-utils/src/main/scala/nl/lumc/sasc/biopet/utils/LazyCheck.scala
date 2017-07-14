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

/**
  * Created by pjvan_thof on 1/25/16.
  */
class LazyCheck[T](function: => T) {
  private var _isSet = false
  def isSet: Boolean = _isSet
  lazy val value: T = {
    val cache = function
    _isSet = true
    cache
  }
  def apply(): T = value
  def get: T = value
}
