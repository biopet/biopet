package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Created by pjvan_thof on 2/14/15.
 */
trait Summarizable extends Configurable {

  var summaryModule = configName

  def summaryFiles: Map[String, File]

  def summaryStats: Map[String, Any]

  /**
   * This function is used to merge
   * @param v1
   * @param v2
   * @param key
   * @return
   */
  def resolveSummaryConflict(v1: Any, v2: Any, key: String) = v1
}
