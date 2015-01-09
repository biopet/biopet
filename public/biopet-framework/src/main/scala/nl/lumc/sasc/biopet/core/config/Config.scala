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

import java.io.File
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.utils.ConfigUtils._

/**
 * This class can store nested config values
 * @param map Map with value for new config
 * @constructor Load config with existing map
 */
class Config(var map: Map[String, Any]) extends Logging {
  logger.debug("Init phase of config")

  /**
   * Default constructor
   */
  def this() = {
    this(Map())
    loadDefaultConfig()
  }

  /**
   * Loading a environmental variable as location of config files to merge into the config
   * @param valueName Name of value
   */
  def loadConfigEnv(valueName: String) {
    val globalFiles = sys.env.get(valueName).getOrElse("").split(":")
    if (globalFiles.isEmpty) logger.info(valueName + " value not found, no global config is loaded")
    for (globalFile <- globalFiles) {
      val file: File = new File(globalFile)
      if (file.exists()) {
        logger.info("Loading config file: " + file)
        loadConfigFile(file)
      } else logger.warn(valueName + " value found but file does not exist, no global config is loaded")
    }
  }

  /**
   * Loading default value for biopet
   */
  def loadDefaultConfig() {
    loadConfigEnv("BIOPET_CONFIG")
  }

  /**
   * Merge a json file into the config
   * @param configFile Location of file
   */
  def loadConfigFile(configFile: File) {
    val configMap = fileToConfigMap(configFile)

    if (map.isEmpty) map = configMap
    else map = mergeMaps(configMap, map)
    logger.debug("New config: " + map)
  }

  protected[config] var notFoundCache: List[ConfigValueIndex] = List()
  protected[config] var foundCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] var defaultCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] def clearCache: Unit = {
    notFoundCache = List()
    foundCache = Map()
    defaultCache = Map()
  }

  /**
   * Check if value exist in root of config
   * @deprecated
   * @param s key
   * @return True if exist
   */
  def contains(s: String): Boolean = map.contains(s)

  /**
   * Checks if value exist in config
   * @param requestedIndex Index to value
   * @return True if exist
   */
  def contains(requestedIndex: ConfigValueIndex): Boolean =
    if (notFoundCache.contains(requestedIndex)) return false
    else if (foundCache.contains(requestedIndex)) return true
    else {
      val value = Config.getValueFromMap(map, requestedIndex)
      if (value.isDefined) {
        foundCache += (requestedIndex -> value.get)
        return true
      } else {
        notFoundCache +:= requestedIndex
        return false
      }
    }

  /**
   * Checks if value exist in config
   * @param module Name of module
   * @param path Path to start searching
   * @param key Name of value
   * @param freeVar Default true, if set false value must exist in module
   * @return True if exist
   */
  def contains(module: String, path: List[String], key: String, freeVar: Boolean = true): Boolean = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
    contains(requestedIndex)
  }

  /**
   * Find value in config
   * @param module Name of module
   * @param path Path to start searching
   * @param key Name of value
   * @param default Default value when no value is found
   * @param freeVar Default true, if set false value must exist in module
   * @return Config value
   */
  protected[config] def apply(module: String, path: List[String], key: String, default: Any = null, freeVar: Boolean = true): ConfigValue = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
    if (contains(requestedIndex)) return foundCache(requestedIndex)
    else if (default != null) {
      defaultCache += (requestedIndex -> ConfigValue(requestedIndex, null, default, freeVar))
      return defaultCache(requestedIndex)
    } else throw new IllegalStateException("Value in config could not be found but it seems required, index: " + requestedIndex)
  }

  //TODO: New version of report is needed
  /**
   * Makes report for all used values
   * @return Config report
   */
  def getReport: String = {
    val output: StringBuilder = new StringBuilder
    output.append("Config report, sorted on module:\n")
    var modules: Map[String, StringBuilder] = Map()
    for ((key, value) <- foundCache) {
      val module = key.module
      if (!modules.contains(module)) modules += (module -> new StringBuilder)
      modules(module).append("Found: " + value.toString + "\n")
    }
    for ((key, value) <- defaultCache) {
      val module = key.module
      if (!modules.contains(module)) modules += (module -> new StringBuilder)
      modules(module).append("Default used: " + value.toString + "\n")
    }
    for (value <- notFoundCache) {
      val module = value.module
      if (!modules.contains(module)) modules += (module -> new StringBuilder)
      if (!defaultCache.contains(value)) modules(module).append("Not Found: " + value.toString + "\n")
    }
    for ((key, value) <- modules) {
      output.append("Config options for module: " + key + "\n")
      output.append(value.toString)
      output.append("\n")
    }
    return output.toString
  }

  override def toString(): String = map.toString
}

object Config extends Logging {
  val global = new Config

  /**
   * Merge 2 config objects
   * @param config1 prio over config 2
   * @param config2
   * @return Merged config
   */
  def mergeConfigs(config1: Config, config2: Config): Config = new Config(mergeMaps(config1.map, config2.map))

  /**
   * Search for value in index position in a map
   * @param map Map to search in
   * @param index Config index
   * @return Value
   */
  def getValueFromMap(map: Map[String, Any], index: ConfigValueIndex): Option[ConfigValue] = {
    var submodules = index.path
    while (!submodules.isEmpty) {
      var submodules2 = submodules
      while (!submodules2.isEmpty) {
        val p = getMapFromPath(map, submodules2 ::: index.module :: Nil) getOrElse Map()
        if (p.contains(index.key)) {
          return Option(ConfigValue(index, ConfigValueIndex(index.module, submodules2, index.key, freeVar = false), p(index.key)))
        }
        if (index.freeVar) {
          val p2 = getMapFromPath(map, submodules2) getOrElse Map()
          if (p2.contains(index.key)) {
            return Option(ConfigValue(index, ConfigValueIndex(index.module, submodules2, index.key, freeVar = true), p2(index.key)))
          }
        }
        submodules2 = submodules2.init
      }
      submodules = submodules.tail
    }
    val p = getMapFromPath(map, index.module :: Nil) getOrElse Map()
    if (p.contains(index.key)) { // Module is not nested
      return Option(ConfigValue(index, ConfigValueIndex(index.module, Nil, index.key, freeVar = false), p(index.key)))
    } else if (map.contains(index.key) && index.freeVar) { // Root value of json
      return Option(ConfigValue(index, ConfigValueIndex("", Nil, index.key, freeVar = true), map(index.key)))
    } else { // At this point key is not found on the path
      return None
    }
  }
}