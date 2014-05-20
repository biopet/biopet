package nl.lumc.sasc.biopet.core

import scala.util.parsing.json._
import java.io.File
import org.broadinstitute.sting.queue.util.Logging

class Config(private var map: Map[String,Any]) extends Logging {
  def this() = {
    this(Map())
    logger.info("Init phase of config")
    loadDefaultConfig()
  }
  
  def loadDefaultConfig() {
    var globalFile: String = System.getenv("QUEUE_CONFIG")
    if (globalFile != null) {
      var file: File = new File(globalFile)
      if (file.exists()) loadConfigFile(file)
      else logger.warn("QUEUE_CONFIG value  found but file does not exist, no glogal config is loaded")
    } else logger.warn("QUEUE_CONFIG value not found, no glogal config is loaded")
  }
  
  def contains(s:String) : Boolean = map.contains(s)
  
  def loadConfigFile(configFile:File) {
    var returnMap: Map[String,Any] = Map()
    var configJson = JSON.parseFull(scala.io.Source.fromFile(configFile).mkString)
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
  
  def get(s:String) : Any = map(s)
  def get(s:String, default:Any) : Any = if (contains(s)) get(s) else default
  
  def getAsString(s:String) : String = map(s).toString
  def getAsString(s:String, default:String) : String = if (contains(s)) getAsString(s) else default
  
  def getAsInt(s:String) : Int = {
    map(s) match {
      case i:Double => return i.toInt
      case i:Int => return i
      case i:String => {
        logger.warn("Value '" + s + "' is a string insteadof int in json file, trying auto convert")
        return i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + s + "' is not an int")
    }
  }
  def getAsInt(s:String, default:Int) : Int = if (contains(s)) getAsInt(s) else default
  
  def getAsDouble(s:String) : Double = {
    map(s) match {
      case d:Double => return d
      case d:Int => return d.toDouble
      case d:String => {
        logger.warn("Value '" + s + "' is a string insteadof int in json file, trying auto convert")
        return d.toDouble
      }
      case _ => throw new IllegalStateException("Value '" + s + "' is not an int")
    }
  }
  def getAsDouble(s:String, default:Double) : Double = if (contains(s)) getAsDouble(s) else default
  
  def getAsBoolean(s:String) : Boolean = {
    map(s) match {
      case b:Boolean => b
      case b:String => {
        logger.warn("Value '" + s + "' is a string insteadof boolean in json file, trying auto convert")
        return b.contains("true")
      }
      case b:Int => {
        logger.warn("Value '" + s + "' is a int insteadof boolean in json file, trying auto convert")
        (b > 0)
      }
      case _ => throw new IllegalStateException("Value '" + s + "' is not an boolean")
    }
  }
  def getAsBoolean(s:String, default:Boolean) : Boolean = if (contains(s)) getAsBoolean(s) else default
  
  def getAsList(s:String) : List[Any] = {
    map(s) match {
      case l:List[_] => return l
      case _ => throw new IllegalStateException("Value '" + s + "' is not an List")
    }
  }
  def getAsList(s:String, default:List[Any]) : List[Any] = if (contains(s)) getAsList(s) else default
  def getAsListOfStrings(s:String) : List[String] = {
    var l: List[String] = Nil
    for (v <- getAsList(s)) l :+= v.toString
    return l
  }
  def getAsListOfStrings(s:String, default:List[String]) : List[String] = if (contains(s)) getAsListOfStrings(s) else default
  
  def getAsMap(s:String) : Map[String,Any] = {
    map(s) match {
      case m:Map[_,_] => return Config.valueToMap(m)
      case _ => throw new IllegalStateException("Value '" + s + "' is not an Map")
    }
  }
  def getAsMap(s:String, default:Map[String,Any]) : Map[String,Any] = if (contains(s)) getAsMap(s) else default
  
  def getAsConfig(s:String, default:Map[String,Any]) : Config = if (contains(s)) new Config(getAsMap(s)) else new Config(default)
  def getAsConfig(s:String, default:Config) : Config = if (contains(s)) Config.mergeConfigs(getAsConfig(s), default) else default
  def getAsConfig(s:String, default:Config, subDefault:String) : Config = {
    if (contains(s)) Config.mergeConfigs(getAsConfig(s), default.getAsConfig(subDefault))
    else default
  }
  def getAsConfig(s:String) : Config = if (contains(s)) new Config(getAsMap(s)) else new Config(Map())
  
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