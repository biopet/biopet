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
package nl.lumc.sasc.biopet.utils.summary

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
 * This class can read in a summary and extract values from it
 *
 * Created by pjvan_thof on 3/26/15.
 */
class Summary(file: File) {
  val map = ConfigUtils.fileToConfigMap(file)

  /** List of all samples in the summary */
  lazy val samples: Set[String] = {
    ConfigUtils.getValueFromPath(map, List("samples")) match {
      case Some(s) => ConfigUtils.any2map(s).keySet
      case _       => Set()
    }
  }

  /** List of all libraries for each sample */
  lazy val libraries: Map[String, Set[String]] = {
    (for (sample <- samples) yield sample -> {
      ConfigUtils.getValueFromPath(map, List("samples", sample, "libraries")) match {
        case Some(libs) => ConfigUtils.any2map(libs).keySet
        case _          => Set[String]()
      }
    }).toMap
  }

  /** getValue from on given nested path */
  def getValue(path: String*): Option[Any] = {
    ConfigUtils.getValueFromPath(map, path.toList)
  }

  /** getValue from on given nested path with prefix "samples" -> [sampleId] */
  def getSampleValue(sampleId: String, path: String*): Option[Any] = {
    ConfigUtils.getValueFromPath(map, "samples" :: sampleId :: path.toList)
  }

  /** Get values for all samples on given path with prefix "samples" -> [sampleId] */
  def getSampleValues(path: String*): Map[String, Option[Any]] = {
    (for (sample <- samples) yield sample -> getSampleValue(sample, path: _*)).toMap
  }

  /** Executes given function for each sample */
  def getSampleValues[T](function: (Summary, String) => Option[T]): Map[String, Option[T]] = {
    (for (sample <- samples) yield sample -> function(this, sample)).toMap
  }

  /** Get value on nested path with prefix "samples" -> [sampleId] -> "libraries" -> [libId] */
  def getLibraryValue(sampleId: String, libId: String, path: String*): Option[Any] = {
    ConfigUtils.getValueFromPath(map, "samples" :: sampleId :: "libraries" :: libId :: path.toList)
  }

  /** Get value on nested path with prefix depending is sampleId and/or libId is None or not */
  def getValue(sampleId: Option[String], libId: Option[String], path: String*): Option[Any] = {
    (sampleId, libId) match {
      case (Some(sample), Some(lib)) => getLibraryValue(sample, lib, path: _*)
      case (Some(sample), _)         => getSampleValue(sample, path: _*)
      case _                         => getValue(path: _*)
    }
  }

  /** Get value on nested path with prefix depending is sampleId and/or libId is None or not */
  def getValueAsArray(sampleId: Option[String], libId: Option[String], path: String*): Option[Array[Any]] = {
    this.getValue(sampleId, libId, path:_*).map(ConfigUtils.any2list(_).toArray)
  }

  /**
   * Get values for all libraries on a given path
   * @param path path to of value
   * @return (sampleId, libId) -> value
   */
  def getLibraryValues(path: String*): Map[(String, String), Option[Any]] = {
    (for (sample <- samples; lib <- libraries.getOrElse(sample, Set())) yield {
      (sample, lib) -> getLibraryValue(sample, lib, path: _*)
    }).toMap
  }

  /**
   * Executes method for each library
   * @param function Function to execute
   * @return (sampleId, libId) -> value
   */
  def getLibraryValues[T](function: (Summary, String, String) => Option[T]): Map[(String, String), Option[T]] = {
    (for (sample <- samples; lib <- libraries.getOrElse(sample, Set())) yield {
      (sample, lib) -> function(this, sample, lib)
    }).toMap
  }
}
