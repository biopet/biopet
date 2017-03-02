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

import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.ConfigUtils.ImplicitConversions

trait Configurable extends ImplicitConversions {
  /** Should be object of parant object */
  def parent: Configurable
  def globalConfig: Config = if (parent != null) parent.globalConfig else Config.global

  final def root: Configurable = if (parent == null) this else parent.root

  /** suffix to the path */
  def subPath: List[String] = Nil

  /** Get default path to search config values for current object */
  def configPath: List[String] = if (parent != null) parent.configFullPath else Nil

  /** Gets name of module for config */
  def configNamespace = getClass.getSimpleName.toLowerCase

  /** ull path with module in there */
  def configFullPath: List[String] = configPath ::: configNamespace :: Nil

  /** Map to store defaults for config */
  def defaults: Map[String, Any] = Map()

  /** This method merge defaults from the root to it's own */
  protected[config] def internalDefaults: Map[String, Any] = {
    (parent != null, defaults.isEmpty) match {
      case (true, true)   => parent.internalDefaults
      case (true, false)  => ConfigUtils.mergeMaps(defaults, parent.internalDefaults)
      case (false, true)  => globalConfig.defaults
      case (false, false) => ConfigUtils.mergeMaps(defaults, globalConfig.defaults)
    }
  }

  /** All values found in this map will be skipped from the user config */
  def fixedValues: Map[String, Any] = Map()

  /** This method merge fixedValues from the root to it's own */
  protected def internalFixedValues: Map[String, Any] = {
    (parent != null, fixedValues.isEmpty) match {
      case (true, true)  => parent.internalFixedValues
      case (true, false) => ConfigUtils.mergeMaps(fixedValues, parent.internalFixedValues)
      case _             => fixedValues
    }
  }

  val config = new ConfigFunctions

  /**
   * Creates path with a prefix for sample and library
   * "samples" -> "sampleID" -> "libraries" -> "libraryID" -> rest of path
   */
  def getConfigPath(sample: String = null, library: String = null, submodule: String = null) = {
    (if (sample != null) "samples" :: sample :: Nil else Nil) :::
      (if (library != null) "libraries" :: library :: Nil else Nil) :::
      (if (submodule != null) configPath ::: configNamespace :: Nil else configPath)
  }

  /** Class is used for retrieval of config values */
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
     * @param namespace Adds to the path
     * @param freeVar Default true, if set false value must exist in module
     * @param sample Default null, when set path is prefixed with "samples" -> "sampleID"
     * @param library Default null, when set path is prefixed with "libraries" -> "libraryID"
     * @return
     */
    def apply(key: String,
              default: Any = null,
              namespace: String = null,
              freeVar: Boolean = true,
              sample: String = null,
              library: String = null,
              extraSubPath: List[String] = Nil,
              path: List[String] = null): ConfigValue = {
      val s = if (sample != null || defaultSample.isEmpty) sample else defaultSample.get
      val l = if (library != null || defaultLibrary.isEmpty) library else defaultLibrary.get
      val m = if (namespace != null) namespace else configNamespace
      val p = if (path == null) getConfigPath(s, l, namespace) ::: subPath ::: extraSubPath else path ::: extraSubPath
      val d = {
        val value = Config.getValueFromMap(internalDefaults, ConfigValueIndex(m, p, key, freeVar))
        if (value.isDefined) value.get.value else default
      }
      if (d == null) globalConfig(m, p, key, freeVar = freeVar, fixedValues = internalFixedValues)
      else globalConfig(m, p, key, d, freeVar, fixedValues = internalFixedValues)
    }

    /**
     * Check if value exist in config
     * @param key Name of value
     * @param namespace Adds to the path
     * @param freeVar Default true, if set false value must exist in module
     * @param sample Default null, when set path is prefixed with "samples" -> "sampleID"
     * @param library Default null, when set path is prefixed with "libraries" -> "libraryID"
     * @return true when value is found in config
     */
    def contains(key: String,
                 namespace: String = null,
                 freeVar: Boolean = true,
                 sample: String = null,
                 library: String = null,
                 extraSubPath: List[String] = Nil,
                 path: List[String] = null) = {
      val s = if (sample != null || defaultSample.isEmpty) sample else defaultSample.get
      val l = if (library != null || defaultLibrary.isEmpty) library else defaultLibrary.get
      val m = if (namespace != null) namespace else configNamespace
      val p = if (path == null) getConfigPath(s, l, namespace) ::: subPath ::: extraSubPath else path ::: extraSubPath

      globalConfig.contains(m, p, key, freeVar, internalFixedValues) || Config.getValueFromMap(internalDefaults, ConfigValueIndex(m, p, key, freeVar)).isDefined
    }
  }
}
