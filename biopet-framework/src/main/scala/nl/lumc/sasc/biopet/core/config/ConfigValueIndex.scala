package nl.lumc.sasc.biopet.core.config

class ConfigValueIndex(val module: String, val path: List[String], val key: String, val freeVar:Boolean = true) {
  override def toString = "Module = " + module + ",  path = " + path + ",  key = " + key + ",  freeVar = " + freeVar
}

object ConfigValueIndex {
  private var cache: Map[(String, List[String], String), ConfigValueIndex] = Map()

  def apply(module: String, path: List[String], key: String, freeVar:Boolean = true): ConfigValueIndex = {
    if (!cache.contains(module, path, key)) cache += ((module, path, key) -> new ConfigValueIndex(module, path, key, freeVar))
    return cache(module, path, key)
  }
}