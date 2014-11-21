package nl.lumc.sasc.biopet.core.config

import java.io.File
import nl.lumc.sasc.biopet.utils.ConfigUtils._

class ConfigValue(val requestIndex: ConfigValueIndex, val foundIndex: ConfigValueIndex, val value: Any, val default: Boolean) {
  def asString = any2string(value)
  def asInt = any2int(value)
  def asDouble = any2double(value)
  def asList = any2list(value)
  def asFileList: List[File] = for (file <- any2stringList(value)) yield new File(file)
  def asStringList: List[String] = any2stringList(value)
  def asMap = any2map(value)
  def asBoolean = any2boolean(value)

  override def toString: String = {
    var output = "key = " + requestIndex.key
    output += ", value = " + value
    output += ", requestIndex = (" + requestIndex + ")"
    if (foundIndex == null && !default) output += ", found on root of config"
    else if (!default) output += ", foundIndex = (" + foundIndex + ")"
    else output += ", default value is used"

    return output
  }
}

object ConfigValue {
  def apply(requestIndex: ConfigValueIndex, foundIndex: ConfigValueIndex, value: Any) = {
    new ConfigValue(requestIndex, foundIndex, value, false)
  }
  def apply(requestIndex: ConfigValueIndex, foundIndex: ConfigValueIndex, value: Any, default: Boolean) = {
    new ConfigValue(requestIndex, foundIndex, value, default)
  }
}
