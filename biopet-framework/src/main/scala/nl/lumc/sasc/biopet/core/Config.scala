package nl.lumc.sasc.biopet.core

import scala.util.parsing.json._
import java.io.File
import org.broadinstitute.sting.queue.util.Logging

class Config(var map: Map[String,Any]) extends Logging {
  def this() = {
    this(Map())
    logger.info("Init phase of config")
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
  
  def contains(s:String) : Boolean = map.contains(s)
  
  def loadConfigFile(configFile:File) {
    var returnMap: Map[String,Any] = Map()
    var configJson = JSON.parseFull(scala.io.Source.fromFile(configFile).mkString)
    
    if (configJson == None) {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }
    
    this.logger.debug("Jsonfile: " + configFile)
    this.logger.debug("Contain: " + configJson)
    configJson.get match {
      case m:Map[_,_] => {
          returnMap = Config.valueToMap(configJson.get)
          map = Config.mergeMaps(returnMap, map)
      }
      case null => logger.warn("Config " + configFile + " wrong format")
    }
    this.logger.debug("config: " + map)
  }
  
  def getMap() : Map[String,Any] = map
  
  def get(key:String) : Any = map(key)
  def get(key:String, default:Any) : Any = if (contains(key)) get(key) else default
  
  def getAsString(key:String) : String = map(key).toString
  def getAsString(key:String, default:String) : String = if (contains(key)) getAsString(key) else default
  
  def getAsInt(key:String) : Int = {
    map(key) match {
      case i:Double => return i.toInt
      case i:Int => return i
      case i:String => {
        logger.warn("Value '" + key + "' is a string insteadof int in json file, trying auto convert")
        return i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + key + "' is not an int")
    }
  }
  def getAsInt(s:String, default:Int) : Int = if (contains(s)) getAsInt(s) else default
  
  def getAsDouble(key:String) : Double = {
    map(key) match {
      case d:Double => return d
      case d:Int => return d.toDouble
      case d:String => {
        logger.warn("Value '" + key + "' is a string insteadof int in json file, trying auto convert")
        return d.toDouble
      }
      case _ => throw new IllegalStateException("Value '" + key + "' is not an int")
    }
  }
  def getAsDouble(key:String, default:Double) : Double = if (contains(key)) getAsDouble(key) else default
  
  def getAsBoolean(key:String) : Boolean = {
    map(key) match {
      case b:Boolean => b
      case b:String => {
        logger.warn("Value '" + key + "' is a string insteadof boolean in json file, trying auto convert")
        return b.contains("true")
      }
      case b:Int => {
        logger.warn("Value '" + key + "' is a int insteadof boolean in json file, trying auto convert")
        (b > 0)
      }
      case _ => throw new IllegalStateException("Value '" + key + "' is not an boolean")
    }
  }
  def getAsBoolean(key:String, default:Boolean) : Boolean = if (contains(key)) getAsBoolean(key) else default
  
  def getAsList(key:String) : List[Any] = {
    map(key) match {
      case l:List[_] => return l
      case s:String => return List(s)
      case _ => throw new IllegalStateException("Value '" + key + "' is not an List")
    }
  }
  def getAsList(key:String, default:List[Any]) : List[Any] = if (contains(key)) getAsList(key) else default
  def getAsListOfStrings(key:String) : List[String] = {
    var l: List[String] = Nil
    for (v <- getAsList(key)) l :+= v.toString
    return l
  }
  def getAsListOfStrings(key:String, default:List[String]) : List[String] = if (contains(key)) getAsListOfStrings(key) else default
  
  def getAsMap(key:String) : Map[String,Any] = {
    map(key) match {
      case m:Map[_,_] => return Config.valueToMap(m)
      case _ => throw new IllegalStateException("Value '" + key + "' is not an Map")
    }
  }
  def getAsMap(key:String, default:Map[String,Any]) : Map[String,Any] = if (contains(key)) getAsMap(key) else default
  
  def getAsConfig(key:String, default:Map[String,Any]) : Config = if (contains(key)) new Config(getAsMap(key)) else new Config(default)
  def getAsConfig(key:String, default:Config) : Config = if (contains(key)) Config.mergeConfigs(getAsConfig(key), default) else default
  def getAsConfig(key:String, default:Config, subDefault:String) : Config = {
    if (contains(key)) Config.mergeConfigs(getAsConfig(key), default.getAsConfig(subDefault))
    else default
  }
  def getAsConfig(s:String) : Config = if (contains(s)) new Config(getAsMap(s)) else new Config(Map())
  
  def getThreads(default:Int) : Int = {
    val maxThreads = this.getAsInt("maxthreads", 8)
    val threads = this.getAsInt("threads", default)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
  
  override def toString() : String = map.toString
}

object Config {
  def valueToMap(input:Any) : Map[String,Any] = {
    var ouputMap: Map[String,Any] = Map()
    input match {
      case m:Map[_, _] => {
        for ((k,v) <- m) {
          k match {
            case s:String => ouputMap += (s -> v)
            case _ => throw new IllegalStateException("Key of map '" + m + "' is not an String")
          }
        }
      }
      case _ => throw new IllegalStateException("Value '" + input + "' is not an Map")
    }
    return ouputMap
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