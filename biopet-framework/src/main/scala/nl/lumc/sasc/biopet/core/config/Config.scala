package nl.lumc.sasc.biopet.core.config

import nl.lumc.sasc.biopet.core._
import scala.util.parsing.json._
import java.io.File
import org.broadinstitute.sting.queue.util.Logging

class Config(var map: Map[String,Any]) extends Logging {
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
      }
      else logger.warn("BIOPET_CONFIG value found but file does not exist, no global config is loaded")
    } else logger.info("BIOPET_CONFIG value not found, no global config is loaded")
  }
  
  def loadConfigFile(configFile:File) {
    var configJson = JSON.parseFull(scala.io.Source.fromFile(configFile).mkString)
    
    if (configJson == None) {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }
    
    this.logger.debug("Jsonfile: " + configFile)
    this.logger.debug("Contain: " + configJson)
    configJson.get match {
      case m:Map[_,_] => {
          logger.debug(m)
          if (map.isEmpty) map = m.asInstanceOf[Map[String,Any]]
          else map = Config.mergeMaps(m.asInstanceOf[Map[String,Any]], map)
      }
      case null => logger.warn("Config " + configFile + " wrong format")
    }
    this.logger.debug("config: " + map)
  }
  
  def getMap() : Map[String,Any] = map
  
  var notFoundCache: List[ConfigValueIndex] = List()
  var foundCache: Map[ConfigValueIndex,ConfigValue] = Map()
  
  def contains(s:String) : Boolean = map.contains(s)
  def contains(requestedIndex:ConfigValueIndex) : Boolean = contains(requestedIndex.module, requestedIndex.path, requestedIndex.key)
  def contains(module:String, path: List[String], key:String) : Boolean = {
    val requestedIndex = ConfigValueIndex(module,path,key)
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
            foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module,submodules2,key), p(key)))
            return true
          }

          val p2 = getMapFromPath(submodules2)
          //logger.debug("p2: " + p2)
          if (p2.contains(key)) {
            foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module,submodules2,key), p2(key)))
            return true
          }
          submodules2 = submodules2.init
        }
        submodules = submodules.tail
      }
      val p = getMapFromPath(module :: Nil)
      if (p.contains(key)) { // Module is not nested
        foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex(module,Nil,key), p(key)))
        return true
      } else if (this.contains(key)) { // Root value of json
        foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, ConfigValueIndex("",Nil,key), get(key)))
        return true
      } else { // At this point key is not found on the path
        notFoundCache +:= requestedIndex
        return false
      }
    }
  }
  
  private def get(key:String) : Any = map(key)
  private def get(key:String, default:Any) : Any = if (contains(key)) get(key) else default
  
  def apply(module:String, path: List[String], key:String, default:Any) : ConfigValue = {
    val requestedIndex = ConfigValueIndex(module,path,key)
    if (contains(requestedIndex)) return foundCache(requestedIndex)
    else {
      foundCache += (requestedIndex -> ConfigValue.apply(requestedIndex, null, default))
      return foundCache(requestedIndex)
    }
  }
  
  def apply(module:String, path: List[String], key:String) : ConfigValue = {
    val requestedIndex = ConfigValueIndex(module,path,key)
    if (contains(requestedIndex)) return foundCache(requestedIndex)
    else {
      logger.error("Value in config could not be found but it seems required, inde: " + requestedIndex)
      throw new IllegalStateException("Value in config could not be found but it seems required, index: " + requestedIndex)
    }
  }
  
  private def getMapFromPath(path: List[String]) : Map[String,Any] = {
    var returnMap: Map[String,Any] = map
    for (m <- path) {
      if (!returnMap.contains(m)) return Map()
      else returnMap = Config.valueToMap(returnMap(m))
    }
    return returnMap
  }
  
  private def check(module:String, path: List[String], key:String) {
    
  }
    
//  def getAsString(key:String) : String = map(key).toString
//  def getAsString(key:String, default:String) : String = if (contains(key)) getAsString(key) else default
//  
//  def getAsInt(key:String) : Int = {
//    map(key) match {
//      case i:Double => return i.toInt
//      case i:Int => return i
//      case i:String => {
//        logger.warn("Value '" + key + "' is a string insteadof int in json file, trying auto convert")
//        return i.toInt
//      }
//      case _ => throw new IllegalStateException("Value '" + key + "' is not an int")
//    }
//  }
//  def getAsInt(s:String, default:Int) : Int = if (contains(s)) getAsInt(s) else default
//  
//  def getAsDouble(key:String) : Double = {
//    map(key) match {
//      case d:Double => return d
//      case d:Int => return d.toDouble
//      case d:String => {
//        logger.warn("Value '" + key + "' is a string insteadof int in json file, trying auto convert")
//        return d.toDouble
//      }
//      case _ => throw new IllegalStateException("Value '" + key + "' is not an int")
//    }
//  }
//  def getAsDouble(key:String, default:Double) : Double = if (contains(key)) getAsDouble(key) else default
//  
//  def getAsBoolean(key:String) : Boolean = {
//    map(key) match {
//      case b:Boolean => b
//      case b:String => {
//        logger.warn("Value '" + key + "' is a string insteadof boolean in json file, trying auto convert")
//        return b.contains("true")
//      }
//      case b:Int => {
//        logger.warn("Value '" + key + "' is a int insteadof boolean in json file, trying auto convert")
//        (b > 0)
//      }
//      case _ => throw new IllegalStateException("Value '" + key + "' is not an boolean")
//    }
//  }
//  def getAsBoolean(key:String, default:Boolean) : Boolean = if (contains(key)) getAsBoolean(key) else default
//  
//  def getAsList(key:String) : List[Any] = {
//    map(key) match {
//      case l:List[_] => return l
//      case s:String => return List(s)
//      case _ => throw new IllegalStateException("Value '" + key + "' is not an List")
//    }
//  }
//  def getAsList(key:String, default:List[Any]) : List[Any] = if (contains(key)) getAsList(key) else default
//  def getAsListOfStrings(key:String) : List[String] = {
//    var l: List[String] = Nil
//    for (v <- getAsList(key)) l :+= v.toString
//    return l
//  }
//  def getAsListOfStrings(key:String, default:List[String]) : List[String] = if (contains(key)) getAsListOfStrings(key) else default
//  
//  def getAsMap(key:String) : Map[String,Any] = Config.valueToMap(map(key))
//  def getAsMap(key:String, default:Map[String,Any]) : Map[String,Any] = if (contains(key)) getAsMap(key) else default
//  
//  def getAsConfig(key:String, default:Map[String,Any]) : Config = if (contains(key)) new Config(getAsMap(key)) else new Config(default)
//  def getAsConfig(key:String, default:Config) : Config = if (contains(key)) Config.mergeConfigs(getAsConfig(key), default) else default
//  def getAsConfig(key:String, default:Config, subDefault:String) : Config = {
//    if (contains(key)) Config.mergeConfigs(getAsConfig(key), default.getAsConfig(subDefault))
//    else default
//  }
//  def getAsConfig(s:String) : Config = if (contains(s)) new Config(getAsMap(s)) else new Config(Map())
//  
//  def getThreads(default:Int) : Int = {
//    val maxThreads = this.getAsInt("maxthreads", 8)
//    val threads = this.getAsInt("threads", default)
//    if (maxThreads > threads) return threads
//    else return maxThreads
//  }
  
  override def toString() : String = map.toString
}

object Config {  
  def valueToMap(input:Any) : Map[String,Any] = {
    input match {
      case m:Map[_, _] => return m.asInstanceOf[Map[String,Any]]
      case _ => throw new IllegalStateException("Value '" + input + "' is not an Map")
    }
  }
  
  def mergeMaps(map1:Map[String,Any],map2:Map[String,Any]) : Map[String,Any] = {
    var newMap: Map[String,Any] = Map()
    for (key <- map1.keySet.++(map2.keySet)) {
      if (map1.contains(key) && !map2.contains(key)) newMap += (key -> map1(key))
      else if (!map1.contains(key) && map2.contains(key)) newMap += (key -> map2(key))
      else if (map1.contains(key) && map2.contains(key)) {
        map1(key) match { 
          case m1:Map[_,_] => {
            map2(key) match {
              case m2:Map[_,_] => newMap += (key -> mergeMaps(Config.valueToMap(m1),Config.valueToMap(m2)))
              case _ => newMap += (key -> map1(key))
            }
          }
          case _ => newMap += (key -> map1(key))
        }
      }
    }
    return newMap
  }
  
  def mergeConfigs(config1:Config,config2:Config) : Config = new Config(mergeMaps(config1.getMap, config2.getMap))
}