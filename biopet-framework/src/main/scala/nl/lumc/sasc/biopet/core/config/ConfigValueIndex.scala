package nl.lumc.sasc.biopet.core.config

class ConfigValueIndex(val module:String, val path:List[String], val key:String) {
  override def toString = "Module = " + module + ", path = " + path + ", key = " + key
}

object ConfigValueIndex {
  private var cache: Map[(String, List[String], String), ConfigValueIndex] = Map()
  
  def apply(module:String, path:List[String], key:String) : ConfigValueIndex = {
    if (!cache.contains(module, path, key)) cache += ((module, path, key) -> new ConfigValueIndex(module, path, key))
    return cache(module, path, key)
  }
}