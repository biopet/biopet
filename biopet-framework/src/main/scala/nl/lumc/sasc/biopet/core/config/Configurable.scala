package nl.lumc.sasc.biopet.core.config

import java.io.File
import org.broadinstitute.gatk.queue.util.Logging

trait Configurable extends Logging {
  val root: Configurable
  val globalConfig: Config = if (root != null) root.globalConfig else new Config()
  def configPath: List[String] = if (root != null) root.configFullPath else List()
  protected lazy val configName = getClass.getSimpleName.toLowerCase
  protected lazy val configFullPath = configName :: configPath
  var defaults: scala.collection.mutable.Map[String,Any] = if (root != null) scala.collection.mutable.Map(root.defaults.toArray:_*)
                                                          else scala.collection.mutable.Map()
  
  val config = new ConfigFuntions
  
  protected class ConfigFuntions {
    def apply(key: String, default: Any = null, submodule: String = null, required: Boolean = false, freeVar:Boolean = true): ConfigValue = {
      val m = if (submodule != null) submodule else configName
      val p = if (submodule != null) configName :: configPath else configPath
      val d = {
        val value = Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar))
        if (value.isDefined) value.get.value else default
      }
      if (!contains(key, submodule, freeVar) && d == null) {
        if (required) {
          logger.error("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
          throw new IllegalStateException("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
        } else return null
      }
      if (d == null) return globalConfig(m, p, key, freeVar)
      else return globalConfig(m, p, key, d, freeVar)
    }
    
    def contains(key: String, submodule: String = null, freeVar:Boolean = true) = {
      val m = if (submodule != null) submodule else configName
      val p = if (submodule != null) configName :: configPath else configPath

      globalConfig.contains(m, p, key, freeVar) || !Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar)).isEmpty
    }
  }
  
  implicit def configValue2file(value: ConfigValue): File = if (value != null) new File(Configurable.any2string(value.value)) else null
  implicit def configValue2string(value: ConfigValue): String = if (value != null) Configurable.any2string(value.value) else null
  implicit def configValue2long(value: ConfigValue): Long = if (value != null) Configurable.any2long(value.value) else 0
  implicit def configValue2optionLong(value: ConfigValue): Option[Long] = if (value != null) Option(Configurable.any2long(value.value)) else None
  implicit def configValue2int(value: ConfigValue): Int = if (value != null) Configurable.any2int(value.value) else 0
  implicit def configValue2optionInt(value: ConfigValue): Option[Int] = if (value != null) Option(Configurable.any2int(value.value)) else None
  implicit def configValue2double(value: ConfigValue): Double = if (value != null) Configurable.any2double(value.value) else 0
  implicit def configValue2optionDouble(value: ConfigValue): Option[Double] = if (value != null) Option(Configurable.any2double(value.value)) else None
  implicit def configValue2float(value: ConfigValue): Float = if (value != null) Configurable.any2float(value.value) else 0
  implicit def configValue2optionFloat(value: ConfigValue): Option[Float] = if (value != null) Option(Configurable.any2float(value.value)) else None
  implicit def configValue2boolean(value: ConfigValue): Boolean = if (value != null) Configurable.any2boolean(value.value) else false
  implicit def configValue2optionBoolean(value: ConfigValue): Option[Boolean] = if (value != null) Option(Configurable.any2boolean(value.value)) else None
  implicit def configValue2list(value: ConfigValue): List[Any] = if (value != null) Configurable.any2list(value.value) else null
  implicit def configValue2stringList(value: ConfigValue): List[String] = if (value != null) Configurable.any2stringList(value.value) else null
  implicit def configValue2stringSet(value: ConfigValue): Set[String] = if (value != null) Configurable.any2stringList(value.value).toSet else null
  implicit def configValue2map(value: ConfigValue): Map[String, Any] = if (value != null) Configurable.any2map(value.value) else null
}

object Configurable extends Logging {
  def any2string(any: Any): String = {
    if (any == null) return null
    any match {
      case s: String => return s
      case _         => return any.toString
    }
  }

  def any2int(any: Any): Int = {
    any match {
      case i: Double => return i.toInt
      case i: Int    => return i
      case i: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  def any2long(any: Any): Long = {
    any match {
      case l: Double => return l.toLong
      case l: Int    => return l.toLong
      case l: Long   => return l
      case l: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return l.toLong
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  def any2double(any: Any): Double = {
    any match {
      case d: Double => return d
      case d: Float  => return d.toDouble
      case d: Int    => return d.toDouble
      case d: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return d.toDouble
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }
  
  def any2float(any: Any): Float = {
    any match {
      case f: Double => return f.toFloat
      case f: Int    => return f.toFloat
      case f: Float  => return f
      case f: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return f.toFloat
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  def any2boolean(any: Any): Boolean = {
    any match {
      case b: Boolean => return b
      case b: String => {
        logger.warn("Value '" + any + "' is a string insteadof boolean in json file, trying auto convert")
        return b.contains("true")
      }
      case b: Int => {
        logger.warn("Value '" + any + "' is a int insteadof boolean in json file, trying auto convert")
        return (b > 0)
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an boolean")
    }
  }

  def any2list(any: Any): List[Any] = {
    if (any == null) return null
    any match {
      case l: List[_] => return l
      case _          => List(any)
    }
  }

  def any2stringList(any: Any): List[String] = {
    if (any == null) return null
    var l: List[String] = Nil
    for (v <- any2list(any)) l :+= v.toString
    return l
  }

  def any2map(any: Any): Map[String, Any] = {
    if (any == null) return null
    any match {
      case m: Map[_, _] => return m.asInstanceOf[Map[String, Any]]
      case _            => throw new IllegalStateException("Value '" + any + "' is not an Map")
    }
  }
}