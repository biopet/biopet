package nl.lumc.sasc.biopet.core.config

class ConfigValue(val requestIndex:ConfigValueIndex, val foundIndex:ConfigValueIndex, val value:Any) {
  def getString = Configurable.any2string(value)
  def getInt = Configurable.any2int(value)
  def getDouble = Configurable.any2double(value)
  def getList = Configurable.any2list(value)
  def getMap = Configurable.any2map(value)
  
  override def toString: String = {
    var output = "requestIndex = " + requestIndex
    if (foundIndex == null) output += "'not foundin config, used default'"
    else output += ", foundIndex = " + foundIndex
    output += ", value = " + value
    return output
  }
}

object ConfigValue {
  def apply(requestIndex:ConfigValueIndex, foundIndex:ConfigValueIndex, value:Any) = new ConfigValue(requestIndex,foundIndex,value)
}
