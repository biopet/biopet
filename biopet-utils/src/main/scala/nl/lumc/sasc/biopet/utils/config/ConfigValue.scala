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
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.utils.config

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils._

class ConfigValue(val requestIndex: ConfigValueIndex,
                  val foundIndex: ConfigValueIndex,
                  val value: Any,
                  val default: Boolean) {

  /** Get value as String */
  def asString: String = any2string(value)

  /** Get value as File */
  def asFile = new File(any2string(value))

  /** Get value as Int */
  def asInt: Int = any2int(value)

  /** Get value as Double */
  def asDouble: Double = any2double(value)

  /** Get value as List[Any] */
  def asList: List[Any] = any2list(value)

  /** Get value as List[File] */
  def asFileList: List[File] = for (file <- any2stringList(value)) yield new File(file)

  /** Get value as List[String] */
  def asStringList: List[String] = any2stringList(value)

  /** Get value as Map */
  def asMap: Map[String, Any] = any2map(value)

  /** Get value as Boolean */
  def asBoolean: Boolean = any2boolean(value)

  /** Readable output of indexes and value, just for debug */
  override def toString: String = {
    var output = "key = " + requestIndex.key
    output += ", value = " + value
    output += ", requestIndex = (" + requestIndex + ")"
    if (foundIndex == null && !default) output += ", found on root of config"
    else if (!default) output += ", foundIndex = (" + foundIndex + ")"
    else output += ", default value is used"

    output
  }
}

object ConfigValue {

  /**
    *
    * @param requestIndex Index where to start searching
    * @param foundIndex Index where value is found
    * @param value Found value
    * @return ConfigValue object
    */
  def apply(requestIndex: ConfigValueIndex,
            foundIndex: ConfigValueIndex,
            value: Any): ConfigValue = {
    new ConfigValue(requestIndex, foundIndex, value, false)
  }

  /**
    *
    * @param requestIndex Index where to start searching
    * @param foundIndex Index where value is found
    * @param value Found value
    * @param default Value is a default value
    * @return ConfigValue object
    */
  def apply(requestIndex: ConfigValueIndex,
            foundIndex: ConfigValueIndex,
            value: Any,
            default: Boolean): ConfigValue = {
    new ConfigValue(requestIndex, foundIndex, value, default)
  }
}
