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
package nl.lumc.sasc.biopet.core.summary

import java.io.File

/**
 * Trait for class to let them accept into a Summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
trait Summarizable {

  /** Must return files to store into summary */
  def summaryFiles: Map[String, File]

  /** Must returns stats to store into summary */
  def summaryStats: Any

  /** Can be used to add additional Summarizable, this is executed at the start of WriteSummary*/
  def addToQscriptSummary(qscript: SummaryQScript) {}

  /**
   * This function is used to merge value that are found at the same path in the map. Default there will throw a exception at conflicting values.
   * @param v1 Value of new map
   * @param v2 Value of old map
   * @param key Key of value
   * @return combined value
   */
  def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    throw new IllegalStateException("Merge can not have same key by default")
  }

  def summaryDeps: List[File] = Nil
}
