package nl.lumc.sasc.biopet.core.config

class ConfigValue(val requestIndex: ConfigValueIndex, val foundIndex: ConfigValueIndex, val value: Any, val default: Boolean) {
  def getString = Configurable.any2string(value)
  def getInt = Configurable.any2int(value)
  def getDouble = Configurable.any2double(value)
  def getList = Configurable.any2list(value)
  def getMap = Configurable.any2map(value)

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
