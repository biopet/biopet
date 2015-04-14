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
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.core.config.ConfigValue
import argonaut._, Argonaut._
import org.yaml.snakeyaml.Yaml
import scalaz._, Scalaz._
import scala.collection.JavaConversions._

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
  def mergeMaps(map1: Map[String, Any], map2: Map[String, Any],
                resolveConflict: (Any, Any, String) => Any = (m1, m2, key) => m1): Map[String, Any] = {
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
          case _ => newMap += (key -> resolveConflict(map1(key), map2(key), key))
        }
      }
    }
    return newMap
  }

  /**
   * Get nested map
   * @param map Map to search in
   * @param path Nested path to get from map
   * @return Nested map
   */
  def getMapFromPath(map: Map[String, Any], path: List[String]): Option[Map[String, Any]] = {
    val value = getValueFromPath(map, path) getOrElse { return None }
    value match {
      case m: Map[_, _] => Some(m.asInstanceOf[Map[String, Any]])
      case _            => throw new IllegalStateException("Value is not a map: " + value)
    }
  }

  /**
   * Get nested value
   * @param map Map to search in
   * @param path Path to the value
   * @return Some(value) or None if not found
   */
  def getValueFromPath(map: Map[String, Any], path: List[String]): Option[Any] = {
    val value = map.get(path.head)
    if (path.tail == Nil || value == None) value
    else value.get match {
      case map: Map[_, _]                     => getValueFromPath(map.asInstanceOf[Map[String, Any]], path.tail)
      case map: java.util.LinkedHashMap[_, _] => getValueFromPath(map.toMap.asInstanceOf[Map[String, Any]], path.tail)
      case _                                  => None
    }
  }

  /** Make json aboject from a file */
  def fileToJson(configFile: File): Json = {
    logger.debug("Jsonfile: " + configFile)
    val jsonText = scala.io.Source.fromFile(configFile).mkString
    val json = Parse.parseOption(jsonText)
    logger.debug(json)

    json getOrElse {
      throw new IllegalStateException("The config JSON file is either not properly formatted or not a JSON file, file: " + configFile)
    }
  }

  /** Convert config value to map */
  def fileToConfigMap(configFile: File): Map[String, Any] = {

    val configMap = {
      if (configFile.getName.endsWith(".yaml")) yamlToMap(configFile)
      else jsonToMap(fileToJson(configFile))
    }
    logger.debug("Contain: " + configMap)
    return configMap
  }

  /** Convert a yaml file to map[String, Any] */
  def yamlToMap(file: File): Map[String, Any] = {
    val yaml = new Yaml()
    val a = yaml.load(scala.io.Source.fromFile(file).reader())
    ConfigUtils.any2map(a)
  }

  /** Convert json to native scala map/values */
  def jsonToMap(json: Json): Map[String, Any] = {
    var output: Map[String, Any] = Map()
    if (json.isObject) {
      for (key <- json.objectFieldsOrEmpty) {
        val value: Any = jsonToAny(json.field(key).get)
        output += (key -> value)
      }
    } else throw new IllegalStateException("Given value is no json object: " + json)
    return output
  }

  /** Convert json value to native scala value */
  def jsonToAny(json: Json): Any = {
    if (json.isObject) return jsonToMap(json)
    else if (json.isArray) {
      var list: List[Any] = List()
      for (value <- json.array.get) list ::= jsonToAny(value)
      return list.reverse
    } else if (json.isBool) return json.bool.get
    else if (json.isString) return json.string.get.toString
    else if (json.isNumber) {
      val num = json.number.get
      if (num % 1 > 0) return num.toDouble
      else return num.toLong
    } else if (json.isNull) return None
    else throw new IllegalStateException("Config value type not supported, value: " + json)
  }

  /** Convert native scala map to json */
  def mapToJson(map: Map[String, Any]): Json = {
    map.foldLeft(jEmptyObject)((acc, kv) => (kv._1 := {
      kv._2 match {
        case m: Map[_, _] => mapToJson(m.map(m => m._1.toString -> anyToJson(m._2)))
        case _            => anyToJson(kv._2)
      }
    }) ->: acc)
  }

  /** Convert native scala value to json, fall back on .toString if type is not a native scala value */
  def anyToJson(any: Any): Json = {
    any match {
      case j: Json      => j
      case None         => Json.jNull
      case m: Map[_, _] => mapToJson(m.map(m => m._1.toString -> anyToJson(m._2)))
      case l: List[_]   => Json.array(l.map(anyToJson(_)): _*)
      case b: Boolean   => Json.jBool(b)
      case n: Int       => Json.jNumberOrString(n)
      case n: Double    => Json.jNumberOrString(n)
      case n: Long      => Json.jNumberOrString(n)
      case n: Short     => Json.jNumberOrString(n)
      case n: Float     => Json.jNumberOrString(n)
      case n: Byte      => Json.jNumberOrString(n)
      case _            => jString(any.toString)
    }
  }

  /** Convert Any to String */
  def any2string(any: Any): String = {
    if (any == null) return null
    any match {
      case s: String => s
      case _         => any.toString
    }
  }

  /** Convert Any to Int */
  def any2int(any: Any): Int = {
    any match {
      case i: Int    => i
      case i: Double => i.toInt
      case i: Long   => i.toInt
      case i: String => {
        logger.warn("Value '" + any + "' is a string insteadof int in json file, trying auto convert")
        i.toInt
      }
      case _ => throw new IllegalStateException("Value '" + any + "' is not an int")
    }
  }

  /** Convert Any to Long */
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

  /** Convert Any to Double */
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

  /** Convert Any to Float */
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

  /** Convert Any to Boolean */
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

  /** Convert Any to List[Any], fallback on list with 1 value */
  def any2list(any: Any): List[Any] = {
    if (any == null) return null
    any match {
      case l: List[_] => l
      case _          => List(any)
    }
  }

  /** Convert Any to List[String] */
  def any2stringList(any: Any): List[String] = {
    if (any == null) return null
    any2list(any).map(_.toString)
  }

  /** Convert Any to List[File] */
  def any2fileList(any: Any): List[File] = {
    if (any == null) return null
    any2list(any).map(x => new File(x.toString))
  }

  /** Convert Any to Map[String, Any] */
  def any2map(any: Any): Map[String, Any] = {
    if (any == null) return null
    any match {
      case m: Map[_, _]                     => m.map(x => x._1.toString -> x._2)
      case m: java.util.LinkedHashMap[_, _] => nestedJavaHashMaptoScalaMap(m)
      case _                                => throw new IllegalStateException("Value '" + any + "' is not an Map")
    }
  }

  /** Convert nested java hash map to scala hash map */
  def nestedJavaHashMaptoScalaMap(input: java.util.LinkedHashMap[_, _]): Map[String, Any] = {
    input.map(value => {
      value._2 match {
        case m: java.util.LinkedHashMap[_, _] => value._1.toString -> nestedJavaHashMaptoScalaMap(m)
        case _                                => value._1.toString -> value._2
      }
    }).toMap
  }

  /** Trait for implicit conversions for ConfigValue to native scala values */
  trait ImplicitConversions {
    import scala.language.implicitConversions

    private def requiredValue(value: ConfigValue): Boolean = {
      val exist = valueExists(value)
      if (!exist)
        BiopetQScript.addError("Value does not exist but is required, key: " + value.requestIndex.key +
          "  module: " + value.requestIndex.module,
          (if (value.requestIndex.path != Nil) "  path: " + value.requestIndex.path.mkString("->") else null))
      exist
    }

    private def valueExists(value: ConfigValue): Boolean = {
      value != null && value.value != null && value.value != None
    }

    /** Convert ConfigValue to File */
    implicit def configValue2file(value: ConfigValue): File = {
      if (requiredValue(value)) new File(any2string(value.value))
      else new File("")
    }

    /** Convert ConfigValue to File */
    implicit def configValue2optionFile(value: ConfigValue): Option[File] = {
      if (valueExists(value)) Some(new File(any2string(value.value)))
      else None
    }

    /** Convert ConfigValue to String */
    implicit def configValue2string(value: ConfigValue): String = {
      if (requiredValue(value)) any2string(value.value)
      else ""
    }

    /** Convert ConfigValue to String */
    implicit def configValue2optionString(value: ConfigValue): Option[String] = {
      if (valueExists(value)) Some(any2string(value.value))
      else None
    }

    /** Convert ConfigValue to Long */
    implicit def configValue2long(value: ConfigValue): Long = {
      if (requiredValue(value)) any2long(value.value)
      else 0L
    }

    /** Convert ConfigValue top Option[Long] */
    implicit def configValue2optionLong(value: ConfigValue): Option[Long] = {
      if (valueExists(value)) Option(any2long(value.value))
      else None
    }

    /** Convert ConfigValue to Int */
    implicit def configValue2int(value: ConfigValue): Int = {
      if (requiredValue(value)) any2int(value.value)
      else 0
    }

    /** Convert ConfigValue to Option[Int] */
    implicit def configValue2optionInt(value: ConfigValue): Option[Int] = {
      if (valueExists(value)) Option(any2int(value.value))
      else None
    }

    /** Convert ConfigValue to Double */
    implicit def configValue2double(value: ConfigValue): Double = {
      if (requiredValue(value)) any2double(value.value)
      else 0.0
    }

    /** Convert ConfigValue to Option[Double] */
    implicit def configValue2optionDouble(value: ConfigValue): Option[Double] = {
      if (valueExists(value)) Option(any2double(value.value))
      else None
    }

    /** Convert ConfigValue to Float */
    implicit def configValue2float(value: ConfigValue): Float = {
      if (requiredValue(value)) any2float(value.value)
      else 0f
    }

    /** Convert ConfigValue to Option[Float] */
    implicit def configValue2optionFloat(value: ConfigValue): Option[Float] = {
      if (valueExists(value)) Option(any2float(value.value))
      else None
    }

    /** Convert ConfigValue to Boolean */
    implicit def configValue2boolean(value: ConfigValue): Boolean = {
      if (requiredValue(value)) any2boolean(value.value)
      else false
    }

    /** Convert ConfigValue to Option[Boolean] */
    implicit def configValue2optionBoolean(value: ConfigValue): Option[Boolean] = {
      if (valueExists(value)) Option(any2boolean(value.value))
      else None
    }

    /** Convert ConfigValue to List[Any] */
    implicit def configValue2list(value: ConfigValue): List[Any] = {
      if (requiredValue(value)) any2list(value.value)
      else Nil
    }

    /** Convert ConfigValue to List[String] */
    implicit def configValue2stringList(value: ConfigValue): List[String] = {
      if (requiredValue(value)) any2stringList(value.value)
      else Nil
    }

    /** Convert ConfigValue to List[File] */
    implicit def configValue2fileList(value: ConfigValue): List[File] = {
      if (requiredValue(value)) any2fileList(value.value)
      else Nil
    }

    /** Convert ConfigValue to Set[String] */
    implicit def configValue2stringSet(value: ConfigValue): Set[String] = {
      if (requiredValue(value)) any2stringList(value.value).toSet
      else Set()
    }

    /** Config config value to Map[String, Any] */
    implicit def configValue2map(value: ConfigValue): Map[String, Any] = {
      if (requiredValue(value)) any2map(value.value)
      else Map()
    }
  }
}
