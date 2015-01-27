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

import nl.lumc.sasc.biopet.core.Logging
import nl.lumc.sasc.biopet.utils.ConfigUtils.ImplicitConversions

trait Configurable extends ImplicitConversions {
  /** Should be object of parant object */
  val root: Configurable

  /** subfix to the path */
  def subPath: List[String] = Nil

  /** Get default path to search config values for current object */
  def configPath: List[String] = if (root != null) root.configFullPath ::: subPath else subPath

  /** Gets name of module for config */
  protected[core] def configName = getClass.getSimpleName.toLowerCase

  /** ull path with module in there */
  protected[core] def configFullPath: List[String] = configPath ::: configName :: Nil

  /** Map to store defaults for config */
  var defaults: scala.collection.mutable.Map[String, Any] = {
    if (root != null) scala.collection.mutable.Map(root.defaults.toArray: _*)
    else scala.collection.mutable.Map()
  }

  val config = new ConfigFunctions

  /**
   * Creates path with a prefix for sample and library
   * "samples" -> "sampleID" -> "libraries" -> "libraryID" -> rest of path
   * @param sample
   * @param library
   * @param submodule
   * @return
   */
  def path(sample: String = null, library: String = null, submodule: String = null) = {
    (if (sample != null) "samples" :: sample :: Nil else Nil) :::
      (if (library != null) "libraries" :: library :: Nil else Nil) :::
      (if (submodule != null) configPath ::: configName :: Nil else configPath)
  }

  /**
   * Class is used for retrieval of config values
   */
  protected class ConfigFunctions(val defaultSample: Option[String] = None, val defaultLibrary: Option[String] = None) {
    def this(defaultSample: String, defaultLibrary: String) = {
      this(defaultSample = Some(defaultSample), defaultLibrary = Some(defaultLibrary))
    }

    def this(defaultSample: String) = {
      this(defaultSample = Some(defaultSample), defaultLibrary = None)
    }

    (defaultSample, defaultLibrary) match {
      case (Some(null), _) => throw new IllegalArgumentException("defaultSample can not be null")
      case (_, Some(null)) => throw new IllegalArgumentException("defaultLibrary can not be null")
      case _               =>
    }

    /**
     *
     * @param key Name of value
     * @param default Default value if not found
     * @param submodule Adds to the path
     * @param required Default false, if true and value is not found this function will raise an exception
     * @param freeVar Default true, if set false value must exist in module
     * @param sample Default null, when set path is prefixed with "samples" -> "sampleID"
     * @param library Default null, when set path is prefixed with "libraries" -> "libraryID"
     * @return
     */
    def apply(key: String,
              default: Any = null,
              submodule: String = null,
              required: Boolean = false,
              freeVar: Boolean = true,
              sample: String = null,
              library: String = null): ConfigValue = {
      val s = if (sample != null || defaultSample.isEmpty) sample else defaultSample.get
      val l = if (library != null || defaultLibrary.isEmpty) library else defaultLibrary.get
      val m = if (submodule != null) submodule else configName
      val p = path(s, l, submodule)
      val d = {
        val value = Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar))
        if (value.isDefined) value.get.value else default
      }
      if (!contains(key, submodule, freeVar, sample = s, library = l) && d == null) {
        if (required) {
          Logging.logger.error("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
          throw new IllegalStateException("Value in config could not be found but it is required, key: " + key + "   module: " + m + "   path: " + p)
        } else return null
      }
      if (d == null) return Config.global(m, p, key, freeVar = freeVar)
      else return Config.global(m, p, key, d, freeVar)
    }

    /**
     * Check if value exist in config
     * @param key Name of value
     * @param submodule Adds to the path
     * @param freeVar Default true, if set false value must exist in module
     * @param sample Default null, when set path is prefixed with "samples" -> "sampleID"
     * @param library Default null, when set path is prefixed with "libraries" -> "libraryID"
     * @return true when value is found in config
     */
    def contains(key: String,
                 submodule: String = null,
                 freeVar: Boolean = true,
                 sample: String = null,
                 library: String = null) = {
      val s = if (sample != null || defaultSample.isEmpty) sample else defaultSample.get
      val l = if (library != null || defaultLibrary.isEmpty) library else defaultLibrary.get
      val m = if (submodule != null) submodule else configName
      val p = path(s, l, submodule)

      Config.global.contains(m, p, key, freeVar) || !(Config.getValueFromMap(defaults.toMap, ConfigValueIndex(m, p, key, freeVar)) == None)
    }
  }
}
