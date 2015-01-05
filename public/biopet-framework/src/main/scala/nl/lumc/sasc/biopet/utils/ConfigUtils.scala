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
package nl.lumc.sasc.biopet.utils

import java.io.File
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.core.config.ConfigValue
import argonaut._, Argonaut._
import scalaz._, Scalaz._

/**
 * This object contains general function for the config
 *
 */
object ConfigUtils extends Logging {
  /**
   * Merge 2 maps, when value is in a map in map1 and map2 the value calls recursively this function
   * @param map1 Prio over map2
   * @param map2 Backup for map1
   * @return merged map
   */
  def mergeMaps(map1: Map[String, Any], map2: Map[String, Any]): Map[String, Any] = {
    var newMap: Map[String, Any] = Map()
    for (key <- map1.keySet.++(map2.keySet)) {
      if (!map2.contains(key)) newMap += (key -> map1(key))
      else if (!map1.contains(key)) newMap += (key -> map2(key))
      else {
        map1(key) match {
          case m1: Map[_, _] => {
            map2(key) match {
              case m2: Map[_, _] => newMap += (key -> mergeMaps(any2map(m1), any2map(m2)))
              case _             => newMap += (key -> map1(key))
            }
          }
          case _ => newMap += (key -> map1(key))
        }
      }
    }
    return newMap
  }

  /**
   * Make json aboject from a file
   * @param configFile Input file
   * @return Json object
   */
  def fileToJson(configFile: File): Json = {
    logger.debug("Jsonfile: " + configFile)
    val jsonText = scala.io.Source.fromFile(configFile).mkString
    val json = Parse.parseOption(jsonText)
    logger.debug(json)

    json getOrElse {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }
  }

  /**
   * Convert config value to map
   * @param configFile
   * @return Config map
   */
  def fileToConfigMap(configFile: File): Map[String, Any] = {
    val configJson = jsonToMap(fileToJson(configFile))
    logger.debug("Contain: " + configJson)
    return configJson
  }

  /**
   * Convert json to native scala map/values
   * @param json input json
   * @return
   */
  def jsonToMap(json: Json): Map[String, Any] = {
    var output: Map[String, Any] = Map()
    if (json.isObject) {
      for (key <- json.objectFieldsOrEmpty) {
        val value: Any = jsonToAny(json.field(key).get)
        output += (key -> value)
      }
    } else return null
    return output
  }

  /**
   * Convert json value to native scala value
   * @param json input json
   * @return
   */
  def jsonToAny(json: Json): Any = {
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

  /**
   * Convert native scala map to json
   * @param map Input map
   * @return
   */
  def mapToJson(map: Map[String, Any]): Json = {
    map.foldLeft(jEmptyObject)((acc, kv) => (kv._1 := {
      kv._2 match {
        case m: Map[_, _] => mapToJson(m.map(m => m._1.toString -> anyToJson(m._2)))
        case _            => anyToJson(kv._2)
      }
    }) ->: acc)
  }

  /**
   * Convert native scala value to json, fall back on .toString if type is not a native scala value
   * @param any Input Any value
   * @return
   */
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

  /**
   * Convert Any to String
   * @param any Input Any value
   * @return
   */
  def any2string(any: Any): String = {
    if (any == null) return null
    any match {
      case s: String => s
      case _         => any.toString
    }
  }

  /**
   * Convert Any to Int
   * @param any Input Any value
   * @return
   */
  def any2int(any: Any): Int = {
    any match {
      case i: Int    => i
      case i: Double => i.toInt
      case i: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  /**
   * Convert Any to Long
   * @param any Input Any value
   * @return
   */
  def any2long(any: Any): Long = {
    any match {
      case l: Double => l.toLong
      case l: Int    => l.toLong
      case l: Long   => l
      case l: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        l.toLong
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  /**
   * Convert Any to Double
   * @param any Input Any value
   * @return
   */
  def any2double(any: Any): Double = {
    any match {
      case d: Double => d
      case d: Float  => d.toDouble
      case d: Int    => d.toDouble
      case f: Long   => f.toDouble
      case d: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        return d.toDouble
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an number")
    }
  }

  /**
   * Convert Any to Float
   * @param any Input Any value
   * @return
   */
  def any2float(any: Any): Float = {
    any match {
      case f: Double => f.toFloat
      case f: Int    => f.toFloat
      case f: Long   => f.toFloat
      case f: Float  => f
      case f: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        f.toFloat
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an number")
    }
  }

  /**
   * Convert Any to Boolean
   * @param any Input Any value
   * @return
   */
  def any2boolean(any: Any): Boolean = {
    any match {
      case b: Boolean => b
      case b: String => {
        logger.warn("Value '" + any + "' is a string insteadof boolean in json file, trying auto convert")
        b.contains("true")
      }
      case b: Int => {
        logger.warn("Value '" + any + "' is a int insteadof boolean in json file, trying auto convert")
        (b > 0)
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an boolean")
    }
  }

  /**
   * Convert Any to List[Any], fallback on list with 1 value
   * @param any Input Any value
   * @return
   */
  def any2list(any: Any): List[Any] = {
    if (any == null) return null
    any match {
      case l: List[_] => l
      case _          => List(any)
    }
  }

  /**
   * Convert Any to List[String]
   * @param any Input Any value
   * @return
   */
  def any2stringList(any: Any): List[String] = {
    if (any == null) return null
    any2list(any).map(_.toString)
  }

  /**
   * Convert Any to Map[String, Any]
   * @param any Input Any value
   * @return
   */
  def any2map(any: Any): Map[String, Any] = {
    if (any == null) return null
    any match {
      case m: Map[_, _] => m.map(x => x._1.toString -> x._2)
      case _            => throw new IllegalStateException("Value '" + any + "' is not an Map")
    }
  }

  /**
   * Trait for implicit conversions for ConfigValue to native scala values
   */
  trait ImplicitConversions {
    import scala.language.implicitConversions

    /**
     * Convert ConfigValue to File
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2file(value: ConfigValue): File = {
      if (value != null && value.value != null) new File(any2string(value.value)) else null
    }

    /**
     * Convert ConfigValue to String
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2string(value: ConfigValue): String = {
      if (value != null) any2string(value.value) else null
    }

    /**
     * Convert ConfigValue to Long
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2long(value: ConfigValue): Long = {
      if (value != null) any2long(value.value) else 0
    }

    /**
     * Convert ConfigValue top Option[Long]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2optionLong(value: ConfigValue): Option[Long] = {
      if (value != null) Option(any2long(value.value)) else None
    }

    /**
     * Convert ConfigValue to Int
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2int(value: ConfigValue): Int = {
      if (value != null) any2int(value.value) else 0
    }

    /**
     * Convert ConfigValue to Option[Int]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2optionInt(value: ConfigValue): Option[Int] = {
      if (value != null) Option(any2int(value.value)) else None
    }

    /**
     * Convert ConfigValue to Double
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2double(value: ConfigValue): Double = {
      if (value != null) any2double(value.value) else 0
    }

    /**
     * Convert ConfigValue to Option[Double]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2optionDouble(value: ConfigValue): Option[Double] = {
      if (value != null) Option(any2double(value.value)) else None
    }

    /**
     * Convert ConfigValue to Float
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2float(value: ConfigValue): Float = {
      if (value != null) any2float(value.value) else 0
    }

    /**
     * Convert ConfigValue to Option[Float]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2optionFloat(value: ConfigValue): Option[Float] = {
      if (value != null) Option(any2float(value.value)) else None
    }

    /**
     * Convert ConfigValue to Boolean
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2boolean(value: ConfigValue): Boolean = {
      if (value != null) any2boolean(value.value) else false
    }

    /**
     * Convert ConfigValue to Option[Boolean]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2optionBoolean(value: ConfigValue): Option[Boolean] = {
      if (value != null) Option(any2boolean(value.value)) else None
    }

    /**
     * Convert ConfigValue to List[Any]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2list(value: ConfigValue): List[Any] = {
      if (value != null) any2list(value.value) else null
    }

    /**
     * Convert ConfigValue to List[String]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2stringList(value: ConfigValue): List[String] = {
      if (value != null) any2stringList(value.value) else null
    }

    /**
     * Convert ConfigValue to Set[String]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2stringSet(value: ConfigValue): Set[String] = {
      if (value != null && value.value != null) any2stringList(value.value).toSet else null
    }

    /**
     * Config config value to Map[String, Any]
     * @param value Input ConfigValue
     * @return
     */
    implicit def configValue2map(value: ConfigValue): Map[String, Any] = {
      if (value != null) any2map(value.value) else null
    }
  }
}
