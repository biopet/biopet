package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Trait for class to let them accept into a Summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
trait Summarizable extends Configurable {

  /**
   * Must return files to store into summary
   * @return
   */
  def summaryFiles: Map[String, File]

  /**
   * Must returns stats to store into summary
   * @return
   */
  def summaryStats: Map[String, Any]

  /**
   * This function is used to merge
   * @param v1
   * @param v2
   * @param key
   * @return
   */
  def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    throw new IllegalStateException("Merge can not have same key by default")
  }
}
