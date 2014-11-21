package nl.lumc.sasc.biopet.utils

import java.io.File
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.core.config.ConfigValue

object ConfigUtils extends Logging {
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

  trait ImplictConversions {
    import scala.language.implicitConversions

    implicit def configValue2file(value: ConfigValue): File = {
      if (value != null) new File(any2string(value.value))
      else null
    }
    implicit def configValue2string(value: ConfigValue): String = {
      if (value != null) any2string(value.value)
      else null
    }
    implicit def configValue2long(value: ConfigValue): Long = {
      if (value != null) any2long(value.value)
      else 0
    }
    implicit def configValue2optionLong(value: ConfigValue): Option[Long] = {
      if (value != null) Option(any2long(value.value))
      else None
    }
    implicit def configValue2int(value: ConfigValue): Int = {
      if (value != null) any2int(value.value)
      else 0
    }
    implicit def configValue2optionInt(value: ConfigValue): Option[Int] = {
      if (value != null) Option(any2int(value.value))
      else None
    }
    implicit def configValue2double(value: ConfigValue): Double = {
      if (value != null) any2double(value.value)
      else 0
    }
    implicit def configValue2optionDouble(value: ConfigValue): Option[Double] = {
      if (value != null) Option(any2double(value.value))
      else None
    }
    implicit def configValue2float(value: ConfigValue): Float = {
      if (value != null) any2float(value.value)
      else 0
    }
    implicit def configValue2optionFloat(value: ConfigValue): Option[Float] = {
      if (value != null) Option(any2float(value.value))
      else None
    }
    implicit def configValue2boolean(value: ConfigValue): Boolean = {
      if (value != null) any2boolean(value.value)
      else false
    }
    implicit def configValue2optionBoolean(value: ConfigValue): Option[Boolean] = {
      if (value != null) Option(any2boolean(value.value))
      else None
    }
    implicit def configValue2list(value: ConfigValue): List[Any] = {
      if (value != null) any2list(value.value)
      else null
    }
    implicit def configValue2stringList(value: ConfigValue): List[String] = {
      if (value != null) any2stringList(value.value)
      else null
    }
    implicit def configValue2stringSet(value: ConfigValue): Set[String] = {
      if (value != null) any2stringList(value.value).toSet
      else null
    }
    implicit def configValue2map(value: ConfigValue): Map[String, Any] = {
      if (value != null) any2map(value.value)
      else null
    }
  }
}
