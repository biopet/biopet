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

import java.io.{ File, PrintWriter }
import nl.lumc.sasc.biopet.utils.{ Logging, ConfigUtils }
import nl.lumc.sasc.biopet.utils.ConfigUtils._

/**
 * This class can store nested config values
 * @param _map Map with value for new config
 * @constructor Load config with existing map
 */
class Config(protected var _map: Map[String, Any],
             protected var _defaults: Map[String, Any] = Map()) extends Logging {
  logger.debug("Init phase of config")

  /** Default constructor */
  def this() = {
    this(Map())
    loadDefaultConfig()
  }

  def map = _map
  def defaults = _defaults

  /**
   * Loading a environmental variable as location of config files to merge into the config
   * @param valueName Name of value
   * @param default if true files are added to default instead of normal map
   */
  def loadConfigEnv(valueName: String, default: Boolean) {
    sys.env.get(valueName) match {
      case Some(globalFiles) =>
        for (globalFile <- globalFiles.split(":")) {
          val file: File = new File(globalFile)
          if (file.exists) {
            logger.debug("Loading config file: " + file)
            loadConfigFile(file, default)
          } else logger.warn(valueName + " value found but file '" + file + "' does not exist, no global config is loaded")
        }
      case _ => logger.debug(valueName + " value not found, no global config is loaded")
    }
  }

  /** Loading default value for biopet */
  def loadDefaultConfig() {
    loadConfigEnv("BIOPET_CONFIG", default = true)
  }

  /**
   * Merge a json file into the config
   * @param configFile Location of file
   */
  def loadConfigFile(configFile: File, default: Boolean = false) {
    val configMap = fileToConfigMap(configFile)
    if (default) {
      if (_defaults.isEmpty) _defaults = configMap
      else _defaults = mergeMaps(configMap, _defaults)
      logger.debug("New defaults: " + _defaults)
    } else {
      if (_map.isEmpty) _map = configMap
      else _map = mergeMaps(configMap, _map)
      logger.debug("New config: " + _map)
    }
  }

  /**
   * Add a single vallue to the config
   * @param key key of value
   * @param value value itself
   * @param path Path to value
   * @param default if true value is put in default map
   */
  def addValue(key: String, value: Any, path: List[String] = Nil, default: Boolean = false): Unit = {
    val valueMap = path.foldRight(Map(key -> value))((a, b) => Map(a -> b))
    if (default) _defaults = mergeMaps(valueMap, _defaults)
    else _map = mergeMaps(valueMap, _map)
  }

  protected[config] var notFoundCache: Set[ConfigValueIndex] = Set()
  protected[config] var fixedCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] var foundCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] var defaultCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] def clearCache(): Unit = {
    notFoundCache = Set()
    foundCache = Map()
    defaultCache = Map()
  }

  /**
   * Check if value exist in root of config
   * @deprecated
   * @param s key
   * @return True if exist
   */
  def contains(s: String): Boolean = _map.contains(s)

  /**
   * Checks if value exist in config
   * @param requestedIndex Index to value
   * @return True if exist
   */
  def contains(requestedIndex: ConfigValueIndex): Boolean = contains(requestedIndex, Map())

  /**
   * Checks if value exist in config
   * @param requestedIndex Index to value
   * @param fixedValues Fixed values
   * @return True if exist
   */
  def contains(requestedIndex: ConfigValueIndex, fixedValues: Map[String, Any]): Boolean =
    if (notFoundCache.contains(requestedIndex)) false
    else if (fixedCache.contains(requestedIndex)) true
    else if (foundCache.contains(requestedIndex)) true
    else {
      val fixedValue = Config.getValueFromMap(fixedValues, requestedIndex)
      if (fixedValue.isDefined) {
        fixedCache += (requestedIndex -> fixedValue.get)
        true
      } else {
        val value = Config.getValueFromMap(_map, requestedIndex)
        if (value.isDefined && value.get.value != None) {
          foundCache += (requestedIndex -> value.get)
          true
        } else {
          notFoundCache += requestedIndex
          false
        }
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
  def contains(module: String, path: List[String],
               key: String,
               freeVar: Boolean = true,
               fixedValues: Map[String, Any] = Map()): Boolean = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
    contains(requestedIndex, fixedValues)
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
  def apply(module: String,
            path: List[String],
            key: String,
            default: Any = null,
            freeVar: Boolean = true,
            fixedValues: Map[String, Any] = Map()): ConfigValue = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
    if (contains(requestedIndex, fixedValues)) {
      val fixedValue = fixedCache.get(requestedIndex)
      if (fixedValue.isDefined) {
        val userValue = Config.getValueFromMap(_map, requestedIndex)
        if (userValue.isDefined)
          logger.warn(s"Ignoring user-supplied value ${requestedIndex.key} at path ${requestedIndex.path} because it is a fixed value.")
      }

      fixedValue.getOrElse(foundCache(requestedIndex))
    } else if (default != null) {
      defaultCache += (requestedIndex -> ConfigValue(requestedIndex, null, default, freeVar))
      defaultCache(requestedIndex)
    } else ConfigValue(requestedIndex, null, null, freeVar)
  }

  def writeReport(directory: File): Unit = {
    directory.mkdirs()

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
      val file = new File(directory, name + ".json")
      val writer = new PrintWriter(file)
      writer.write(ConfigUtils.mapToJson(map).spaces2)
      writer.close()
    }

    // Positions where values are found
    val found = convertIndexValuesToMap(foundCache.filter(!_._2.default).toList.map(x => (x._2.foundIndex, x._2.value)))
    val fixed = convertIndexValuesToMap(fixedCache.filter(!_._2.default).toList.map(x => (x._2.foundIndex, x._2.value)))
    val unused = uniqueKeys(map, found)

    def reportUnused(map: Map[String, Any], path: List[String] = Nil): Unit = {
      map.foreach {
        case (key, value: Map[_, _]) => reportUnused(value.asInstanceOf[Map[String, Any]], path :+ key)
        case (key, value) => logger.warn(s"config key in user config is never used, key: $key" +
          (if (path.nonEmpty) s", path: ${path.mkString(" -> ")}" else ""))
      }
    }

    reportUnused(unused)

    // Positions where to start searching
    val effectiveFound = convertIndexValuesToMap(foundCache.filter(!_._2.default).toList.map(x => (x._2.requestIndex, x._2.value)), Some(false))
    val effectiveFixed = convertIndexValuesToMap(fixedCache.filter(!_._2.default).toList.map(x => (x._2.requestIndex, x._2.value)), Some(false))
    val effectiveDefaultFound = convertIndexValuesToMap(defaultCache.filter(_._2.default).toList.map(x => (x._2.requestIndex, x._2.value)), Some(false))
    val notFound = convertIndexValuesToMap(notFoundCache.toList.map((_, None)), Some(false))

    // Merged maps
    val fullEffective = ConfigUtils.mergeMaps(effectiveFound, effectiveDefaultFound)
    val fullEffectiveWithNotFound = ConfigUtils.mergeMaps(fullEffective, notFound)

    writeMapToJsonFile(map, "input")
    writeMapToJsonFile(unused, "unused")
    writeMapToJsonFile(_defaults, "defaults")
    writeMapToJsonFile(found, "found")
    writeMapToJsonFile(fixed, "fixed")
    writeMapToJsonFile(effectiveFound, "effective.found")
    writeMapToJsonFile(effectiveFixed, "effective.fixed")
    writeMapToJsonFile(effectiveDefaultFound, "effective.defaults")
    writeMapToJsonFile(notFound, "not.found")
    writeMapToJsonFile(fullEffective, "effective.full")
    writeMapToJsonFile(fullEffectiveWithNotFound, "effective.full.notfound")
  }

  override def toString: String = map.toString()
}

object Config extends Logging {
  val global = new Config

  /**
   * Merge 2 config objects
   * @param config1 prio over config 2
   * @param config2 Low prio map
   * @return Merged config
   */
  def mergeConfigs(config1: Config, config2: Config): Config = new Config(mergeMaps(config1._map, config2._map))

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
      if (p.isDefined) p
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

    tailSearch(startIndex.path)
  }
}