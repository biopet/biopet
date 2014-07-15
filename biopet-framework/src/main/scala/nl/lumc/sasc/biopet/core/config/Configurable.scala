package nl.lumc.sasc.biopet.core.config

import java.io.File
import org.broadinstitute.gatk.queue.util.Logging

trait Configurable extends Logging {
  val root: Configurable
  val globalConfig: Config = if (root != null) root.globalConfig else new Config()
  val configPath: List[String] = if (root != null) root.configFullPath else List()
  protected val configName = getClass.getSimpleName.toLowerCase
  protected val configFullPath = configName :: configPath
  
  def config(key:String) = globalConfig(configName, configPath, key)
  def config(key:String, default:Any) = globalConfig(configName, configPath, key, default)
  def config(key:String, default:Any, module:String) = globalConfig(module, configName :: configPath, key, default)
  
  def configContains(key:String) = globalConfig.contains(configName, configPath, key)
  def configContains(key:String, module:String) = globalConfig.contains(module, configName :: configPath, key)
  
  implicit def configValue2file(value:ConfigValue) = new File(Configurable.any2string(value.value))
  implicit def configValue2string(value:ConfigValue) = Configurable.any2string(value.value)
  implicit def configValue2long(value:ConfigValue) = Configurable.any2long(value.value)
  implicit def configValue2int(value:ConfigValue) = Configurable.any2int(value.value)
  implicit def configValue2optionInt(value:ConfigValue) = Option(Configurable.any2int(value.value))
  implicit def configValue2double(value:ConfigValue) = Configurable.any2double(value.value)
  implicit def configValue2optionDouble(value:ConfigValue) = Option(Configurable.any2double(value.value))
  implicit def configValue2boolean(value:ConfigValue) = Configurable.any2boolean(value.value)
  implicit def configValue2list(value:ConfigValue) = Configurable.any2list(value.value)
  implicit def configValue2stringList(value:ConfigValue) = Configurable.any2stringList(value.value)
  implicit def configValue2stringSet(value:ConfigValue) = Configurable.any2stringList(value.value).toSet
  implicit def configValue2map(value:ConfigValue) = Configurable.any2map(value.value)
}

object Configurable extends Logging {
  def any2string(any:Any) : String = {
    if (any == null) return null
    any match {
      case s:String => return s
      case _ => return any.toString
    }
  }
  
  def any2int(any:Any) : Int = {
    any match {
      case i:Double => return i.toInt
      case i:Int => return i
      case i:String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }
  
  def any2long(any:Any) : Long = {
    any match {
      case l:Double => return l.toLong
      case l:Int => return l.toLong
      case l:Long => return l
      case l:String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return l.toLong
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }
  
  def any2double(any:Any) : Double = {
    any match {
      case d:Double => return d
      case d:Int => return d.toDouble
      case d:String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return d.toInt
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }
  
  def any2boolean(any:Any) : Boolean = {
    any match {
      case b:Boolean => return b
      case b:String => {
        logger.warn("Value '" + any + "' is a string insteadof boolean in json file, trying auto convert")
        return b.contains("true")
      }
      case b:Int => {
        logger.warn("Value '" + any + "' is a int insteadof boolean in json file, trying auto convert")
        return (b > 0)
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an boolean")
    }
  }
  
  def any2list(any:Any) : List[Any] = {
    if (any == null) return null
    any match {
      case l:List[_] => return l
      case s:String => return List(s)
      case _ => throw new IllegalStateException("Value '" + any + "' is not an List")
    }
  }
  
  def any2stringList(any:Any) : List[String] = {
    if (any == null) return null
    var l: List[String] = Nil
      for (v <- any2list(any)) l :+= v.toString
      return l
  }
  
  def any2map(any:Any) : Map[String,Any] = {
    if (any == null) return null
    any match {
      case m:Map[_, _] => return m.asInstanceOf[Map[String,Any]]
      case _ => throw new IllegalStateException("Value '" + any + "' is not an Map")
    }
  }
}