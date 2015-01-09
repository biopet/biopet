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
package nl.lumc.sasc.biopet.core.config

import java.io.File
import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.utils.ConfigUtils.ImplicitConversions

trait Configurable extends ImplicitConversions {
  val root: Configurable
  lazy val configPath: List[String] = if (root != null) root.configFullPath else List()
  protected[config] lazy val configName = getClass.getSimpleName.toLowerCase
  protected[config] lazy val configFullPath: List[String] = configPath ::: configName :: Nil
  var defaults: scala.collection.mutable.Map[String, Any] = if (root != null) scala.collection.mutable.Map(root.defaults.toArray: _*)
  else scala.collection.mutable.Map()

  val config = new ConfigFunctions

  def path(sample: String = null, library: String = null, submodule: String = null) = {
    (if (sample != null) "samples" :: sample :: Nil else Nil) :::
      (if (library != null) "libraries" :: library :: Nil else Nil) :::
      (if (submodule != null) configName :: configPath else configPath)
  }

  protected class ConfigFunctions {
    def apply(key: String,
              default: Any = null,
              submodule: String = null,
              required: Boolean = false,
              freeVar: Boolean = true,
              sample: String = null,
              library: String = null): ConfigValue = {
      val m = if (submodule != null) submodule else configName
      val p = path(sample, library, submodule)
      val d = {
        val value = Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar))
        if (value.isDefined) value.get.value else default
      }
      if (!contains(key, submodule, freeVar, sample = sample, library = library) && d == null) {
        if (required) {
          Logging.logger.error("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
          throw new IllegalStateException("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
        } else return null
      }
      if (d == null) return Config.global(m, p, key, freeVar = freeVar)
      else return Config.global(m, p, key, d, freeVar)
    }

    def contains(key: String,
                 submodule: String = null,
                 freeVar: Boolean = true,
                 sample: String = null,
                 library: String = null) = {
      val m = if (submodule != null) submodule else configName
      val p = path(sample, library, submodule)

      Config.global.contains(m, p, key, freeVar) || !(Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar)) == None)
    }
  }
}
