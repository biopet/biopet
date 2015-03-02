package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Trait for class to let them accept into a Summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
trait Summarizable {

  /** Must return files to store into summary */
  def summaryFiles: Map[String, File]

  /** Must returns stats to store into summary */
  def summaryStats: Map[String, Any]

  /**
   * This function is used to merge value that are found at the same path in the map. Default there will throw a exception at conflicting values.
   * @param v1 Value of new map
   * @param v2 Value of old map
   * @param key Key of value
   * @return
   */
  def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    throw new IllegalStateException("Merge can not have same key by default")
  }
}
