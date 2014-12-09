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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core.config

class ConfigValueIndex(val module: String, val path: List[String], val key: String, val freeVar: Boolean = true) {
  override def toString = "Module = " + module + ",  path = " + path + ",  key = " + key + ",  freeVar = " + freeVar
}

object ConfigValueIndex {
  private var cache: Map[(String, List[String], String), ConfigValueIndex] = Map()

  def apply(module: String, path: List[String], key: String, freeVar: Boolean = true): ConfigValueIndex = {
    if (!cache.contains(module, path, key)) cache += ((module, path, key) -> new ConfigValueIndex(module, path, key, freeVar))
    return cache(module, path, key)
  }
}