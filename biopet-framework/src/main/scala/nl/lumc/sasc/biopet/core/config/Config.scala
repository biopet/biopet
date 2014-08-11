package nl.lumc.sasc.biopet.core.config

import java.io.File
import org.broadinstitute.gatk.queue.util.Logging
import argonaut._, Argonaut._
import scalaz._, Scalaz._

class Config(var map: Map[String, Any]) extends Logging {
  logger.debug("Init phase of config")
  def this() = {
    this(Map())
    loadDefaultConfig()
  }

  def loadDefaultConfig() {
    var globalFile: String = System.getenv("BIOPET_CONFIG")
    if (globalFile != null) {
      var file: File = new File(globalFile)
      if (file.exists()) {
        logger.info("Loading config file: " + file)
        loadConfigFile(file)
      } else logger.warn("BIOPET_CONFIG value found but file does not exist, no global config is loaded")
    } else logger.info("BIOPET_CONFIG value not found, no global config is loaded")
  }

  def loadConfigFile(configFile: File) {
    logger.debug("Jsonfile: " + configFile)
    val jsonText = scala.io.Source.fromFile(configFile).mkString
    val json = Parse.parseOption(jsonText)
    logger.debug(json)
    val configJson = jsonToMap(json.get)
    logger.debug("Contain: " + configJson)
    if (configJson == None) {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }

    if (map.isEmpty) map = configJson
    else map = Config.mergeMaps(configJson, map)
    logger.debug("New config: " + map)
  }

  private def jsonToMap(json: Json): Map[String, Any] = {
    var output: Map[String, Any] = Map()
    if (json.isObject) {
      for (key <- json.objectFieldsOrEmpty) {
        val value: Any = jsonToAny(json.field(key).get)
        output += (key -> value)
      }
    } else return null
    return output
  }

  private def jsonToAny(json: Json): Any = {
    if (json.isObject) return jsonToMap(json)
    else if (json.isArray) {
      var list: List[Any] = List()
      for (value <- json.objectValues.get) list ::= jsonToAny(value)
      return list
    } else if (json.isBool) return json.bool.get
    else if (json.isString) return json.string.get.toString
    else if (json.isNumber) {
      val num = json.number.get
      if (num.toString.contains(".")) return num.toDouble
      else return num.toLong
    } else throw new IllegalStateException("Config value type not supported, value: " + json)
  }

  def getMap(): Map[String, Any] = map

  var notFoundCache: List[ConfigValueIndex] = List()
  var foundCache: Map[ConfigValueIndex, ConfigValue] = Map()
  var defaultCache: Map[ConfigValueIndex, ConfigValue] = Map()

  def contains(s: String): Boolean = map.contains(s)
  def contains(requestedIndex: ConfigValueIndex, freeVar:Boolean): Boolean = contains(requestedIndex.module, requestedIndex.path, requestedIndex.key, freeVar)
  def contains(requestedIndex: ConfigValueIndex): Boolean = contains(requestedIndex.module, requestedIndex.path, requestedIndex.key, true)
  def contains(module: String, path: List[String], key: String, freeVar:Boolean = true): Boolean = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
    if (notFoundCache.contains(requestedIndex)) return false
    else if (foundCache.contains(requestedIndex)) return true
    else {
      var submodules = path.reverse
      while (!submodules.isEmpty) {
        var submodules2 = submodules
        while (!submodules2.isEmpty) {
          val p = getMapFromPath(submodules2 ::: module :: Nil)
          //logger.debug("p: " + p)
          if (p.contains(key)) {
            foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module, submodules2, key), p(key)))
            return true
          }
          if (freeVar) {
            val p2 = getMapFromPath(submodules2)
            //logger.debug("p2: " + p2)
            if (p2.contains(key)) {
              foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module, submodules2, key), p2(key)))
              return true
            }
          }
          submodules2 = submodules2.init
        }
        submodules = submodules.tail
      }
      val p = getMapFromPath(module :: Nil)
      if (p.contains(key)) { // Module is not nested
        foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module, Nil, key), p(key)))
        return true
      } else if (this.contains(key) && freeVar) { // Root value of json
        foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex("", Nil, key), get(key)))
        return true
      } else { // At this point key is not found on the path
        notFoundCache +:= requestedIndex
        return false
      }
    }
  }

  private def get(key: String): Any = map(key)
  private def get(key: String, default: Any): Any = if (contains(key)) get(key) else default

  def apply(module: String, path: List[String], key: String, default: Any = null, freeVar:Boolean = true): ConfigValue = {
    val requestedIndex = ConfigValueIndex(module, path, key)
    if (contains(requestedIndex, freeVar)) return foundCache(requestedIndex)
    else if (default != null) {
      defaultCache += (requestedIndex -> ConfigValue.apply(requestedIndex, null, default, true))
      return defaultCache(requestedIndex)
    } else {
      logger.error("Value in config could not be found but it seems required, index: " + requestedIndex)
      throw new IllegalStateException("Value in config could not be found but it seems required, index: " + requestedIndex)
    }
  }

  private def getMapFromPath(path: List[String]): Map[String, Any] = {
    var returnMap: Map[String, Any] = map
    for (m <- path) {
      if (!returnMap.contains(m)) return Map()
      else returnMap = Config.valueToMap(returnMap(m))
    }
    return returnMap
  }

  def getReport: String = {
    var output: StringBuilder = new StringBuilder
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

object Config {
  def valueToMap(input: Any): Map[String, Any] = {
    input match {
      case m: Map[_, _] => return m.asInstanceOf[Map[String, Any]]
      case _            => throw new IllegalStateException("Value '" + input + "' is not an Map")
    }
  }

  def mergeMaps(map1: Map[String, Any], map2: Map[String, Any]): Map[String, Any] = {
    var newMap: Map[String, Any] = Map()
    for (key <- map1.keySet.++(map2.keySet)) {
      if (map1.contains(key) && !map2.contains(key)) newMap += (key -> map1(key))
      else if (!map1.contains(key) && map2.contains(key)) newMap += (key -> map2(key))
      else if (map1.contains(key) && map2.contains(key)) {
        map1(key) match {
          case m1: Map[_, _] => {
            map2(key) match {
              case m2: Map[_, _] => newMap += (key -> mergeMaps(Config.valueToMap(m1), Config.valueToMap(m2)))
              case _             => newMap += (key -> map1(key))
            }
          }
          case _ => newMap += (key -> map1(key))
        }
      }
    }
    return newMap
  }

  def mergeConfigs(config1: Config, config2: Config): Config = new Config(mergeMaps(config1.getMap, config2.getMap))
}