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
package nl.lumc.sasc.biopet.utils.config

/**
  * General case class used as index config values. This stores the path to the value, the module, name of the value and if freeVar is allowed
  * @param module Module where this value is belonging to
  * @param path Path to value
  * @param key Name of value
  * @param freeVar Default true, if set false value must exist in module
  */
case class ConfigValueIndex(module: String,
                            path: List[String],
                            key: String,
                            freeVar: Boolean = true) {
  override def toString: String =
    "Module = " + module + ",  path = " + path + ",  key = " + key + ",  freeVar = " + freeVar
}
