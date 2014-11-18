package nl.lumc.sasc.biopet.core.config

import java.io.File
import nl.lumc.sasc.biopet.core.Logging
import argonaut._, Argonaut._
import scalaz._, Scalaz._

class Config(var map: Map[String, Any]) extends Logging {
  logger.debug("Init phase of config")
  def this() = {
    this(Map())
    loadDefaultConfig()
  }

  def loadConfigEnv(valueName: String) {
    var globalFiles = System.getenv(valueName).split(":")
    for (globalFile <- globalFiles) {
      var file: File = new File(globalFile)
      if (file.exists()) {
        logger.info("Loading config file: " + file)
        loadConfigFile(file)
      } else logger.warn(valueName + " value found but file does not exist, no global config is loaded")
    }
    if (globalFiles.isEmpty) logger.info(valueName + " value not found, no global config is loaded")
  }

  def loadDefaultConfig() {
    loadConfigEnv("BIOPET_CONFIG")
  }

  def loadConfigFile(configFile: File) {
    logger.debug("Jsonfile: " + configFile)
    val jsonText = scala.io.Source.fromFile(configFile).mkString
    val json = Parse.parseOption(jsonText)
    if (json == None) {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }
    logger.debug(json)
    val configJson = Config.jsonToMap(json.get)
    logger.debug("Contain: " + configJson)

    if (map.isEmpty) map = configJson
    else map = Config.mergeMaps(configJson, map)
    logger.debug("New config: " + map)
  }

  protected[config] var notFoundCache: List[ConfigValueIndex] = List()
  protected[config] var foundCache: Map[ConfigValueIndex, ConfigValue] = Map()
  protected[config] var defaultCache: Map[ConfigValueIndex, ConfigValue] = Map()

  def contains(s: String): Boolean = map.contains(s)
  def contains(requestedIndex: ConfigValueIndex, freeVar: Boolean): Boolean = contains(requestedIndex.module, requestedIndex.path, requestedIndex.key, freeVar)
  def contains(requestedIndex: ConfigValueIndex): Boolean = contains(requestedIndex.module, requestedIndex.path, requestedIndex.key, true)
  def contains(module: String, path: List[String], key: String, freeVar: Boolean = true): Boolean = {
    val requestedIndex = ConfigValueIndex(module, path, key, freeVar)
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
  }

  protected[config] def apply(module: String, path: List[String], key: String, default: Any = null, freeVar: Boolean = true): ConfigValue = {
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

object Config extends Logging {
  val global = new Config

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

  def mergeConfigs(config1: Config, config2: Config): Config = new Config(mergeMaps(config1.map, config2.map))

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
      for (value <- json.array.get) list ::= jsonToAny(value)
      return list
    } else if (json.isBool) return json.bool.get
    else if (json.isString) return json.string.get.toString
    else if (json.isNumber) {
      val num = json.number.get
      if (num.toString.contains(".")) return num.toDouble
      else return num.toLong
    } else throw new IllegalStateException("Config value type not supported, value: " + json)
  }

  private def getMapFromPath(map: Map[String, Any], path: List[String]): Map[String, Any] = {
    var returnMap: Map[String, Any] = map
    for (m <- path) {
      if (!returnMap.contains(m)) return Map()
      else returnMap = Config.valueToMap(returnMap(m))
    }
    return returnMap
  }

  def getValueFromMap(map: Map[String, Any], index: ConfigValueIndex): Option[ConfigValue] = {
    var submodules = index.path.reverse
    while (!submodules.isEmpty) {
      var submodules2 = submodules
      while (!submodules2.isEmpty) {
        val p = getMapFromPath(map, submodules2 ::: index.module :: Nil)
        if (p.contains(index.key)) {
          return Option(ConfigValue(index, ConfigValueIndex(index.module, submodules2, index.key), p(index.key)))
        }
        if (index.freeVar) {
          val p2 = getMapFromPath(map, submodules2)
          if (p2.contains(index.key)) {
            return Option(ConfigValue(index, ConfigValueIndex(index.module, submodules2, index.key), p2(index.key)))
          }
        }
        submodules2 = submodules2.init
      }
      submodules = submodules.tail
    }
    val p = getMapFromPath(map, index.module :: Nil)
    if (p.contains(index.key)) { // Module is not nested
      return Option(ConfigValue(index, ConfigValueIndex(index.module, Nil, index.key), p(index.key)))
    } else if (map.contains(index.key) && index.freeVar) { // Root value of json
      return Option(ConfigValue(index, ConfigValueIndex("", Nil, index.key), map(index.key)))
    } else { // At this point key is not found on the path
      return None
    }
  }

  def mapToJson(map: Map[String, Any]): Json = {
    map.foldLeft(jEmptyObject)((acc, kv) => (kv._1 := {
      kv._2 match {
        case m: Map[_, _] => mapToJson(m.map(m => m._1.toString -> anyToJson(m._2)))
        case _            => anyToJson(kv._2)
      }
    }) ->: acc)
  }

  def anyToJson(any: Any): Json = {
    any match {
      case j: Json      => j
      case m: Map[_, _] => mapToJson(m.map(m => m._1.toString -> anyToJson(m._2)))
      case l: List[_]   => Json.array(l.map(anyToJson(_)): _*)
      case n: Int       => Json.jNumberOrString(n)
      case n: Double    => Json.jNumberOrString(n)
      case n: Long      => Json.jNumberOrString(n)
      case n: Short     => Json.jNumberOrString(n)
      case n: Float     => Json.jNumberOrString(n)
      case n: Byte      => Json.jNumberOrString(n)
      case _            => jString(any.toString)
    }
  }
}