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

import java.io.{ PrintWriter, File }
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.ConfigUtils._

import scala.reflect.io.Directory

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
      if (value.isDefined && value.get.value != None) {
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
    if (contains(requestedIndex)) foundCache(requestedIndex)
    else if (default != null) {
      defaultCache += (requestedIndex -> ConfigValue(requestedIndex, null, default, freeVar))
      defaultCache(requestedIndex)
    } else ConfigValue(requestedIndex, null, null, freeVar)
  }

  def writeReport(id: String, directory: String): Unit = {

    def convertIndexValuesToMap(input: List[(ConfigValueIndex, Any)], forceFreeVar: Option[Boolean] = None): Map[String, Any] = {
      input.foldLeft(Map[String, Any]())(
        (a: Map[String, Any], x: (ConfigValueIndex, Any)) => {
          val v = {
            if (forceFreeVar.getOrElse(x._1.freeVar)) Map(x._1.key -> x._2)
            else Map(x._1.module -> Map(x._1.key -> x._2))
          }
          val newMap = x._1.path.foldRight(v)((p, map) => Map(p -> map))
          ConfigUtils.mergeMaps(a, newMap)
        })
    }

    def writeMapToJsonFile(map: Map[String, Any], name: String): Unit = {
      val file = new File(directory + "/" + id + "." + name + ".json")
      file.getParentFile.mkdirs()
      val writer = new PrintWriter(file)
      writer.write(ConfigUtils.mapToJson(map).spaces2)
      writer.close()
    }

    // Positions where values are found
    val found = convertIndexValuesToMap(foundCache.filter(!_._2.default).toList.map(x => (x._2.foundIndex, x._2.value)))

    // Positions where to start searching
    val effectiveFound = convertIndexValuesToMap(foundCache.filter(!_._2.default).toList.map(x => (x._2.requestIndex, x._2.value)), Some(false))
    val effectiveDefaultFound = convertIndexValuesToMap(defaultCache.filter(_._2.default).toList.map(x => (x._2.requestIndex, x._2.value)), Some(false))
    val notFound = convertIndexValuesToMap(notFoundCache.map((_, None)), Some(false))

    // Merged maps
    val fullEffective = ConfigUtils.mergeMaps(effectiveFound, effectiveDefaultFound)
    val fullEffectiveWithNotFound = ConfigUtils.mergeMaps(fullEffective, notFound)

    writeMapToJsonFile(this.map, "input")
    writeMapToJsonFile(found, "found")
    writeMapToJsonFile(effectiveFound, "effective.found")
    writeMapToJsonFile(effectiveDefaultFound, "effective.defaults")
    writeMapToJsonFile(notFound, "not.found")
    writeMapToJsonFile(fullEffective, "effective.full")
    writeMapToJsonFile(fullEffectiveWithNotFound, "effective.full.notfound")
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
   * @param startIndex Config index
   * @return Value
   */
  def getValueFromMap(map: Map[String, Any], startIndex: ConfigValueIndex): Option[ConfigValue] = {
    def getFromPath(path: List[String]): Option[ConfigValue] = {
      val p = getValueFromPath(map, path ::: startIndex.module :: startIndex.key :: Nil)
      if (p.isDefined) Option(ConfigValue(startIndex, ConfigValueIndex(startIndex.module, path, startIndex.key, freeVar = false), p.get))
      else if (startIndex.freeVar) {
        val p = getValueFromPath(map, path ::: startIndex.key :: Nil)
        if (p.isDefined) Option(ConfigValue(startIndex, ConfigValueIndex(startIndex.module, path, startIndex.key, freeVar = true), p.get))
        else None
      } else None
    }

    def tailSearch(path: List[String]): Option[ConfigValue] = {
      val p = getFromPath(path)
      if (p != None) p
      else if (path == Nil) None
      else {
        val p = initSearch(path)
        if (p.isDefined) p
        else tailSearch(path.tail)
      }
    }

    def initSearch(path: List[String], tail: List[String] = Nil): Option[ConfigValue] = {
      val p = getFromPath(path)
      if (p.isDefined) p
      else if (path == Nil) None
      else {
        val p = skipNested(path, tail)
        if (p.isDefined) p
        else initSearch(path.init, path.last :: tail)
      }
    }

    def skipNested(path: List[String], tail: List[String] = Nil): Option[ConfigValue] = {
      val p = getFromPath(path ::: tail)
      if (p.isDefined) p
      else if (tail == Nil) None
      else skipNested(path, tail.tail)
    }

    return tailSearch(startIndex.path)
  }
}