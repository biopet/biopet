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
package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.config.{ ConfigValue, Config, Configurable }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.ConfigUtils._
import scala.reflect.ClassTag
import scala.reflect._
import org.broadinstitute.gatk.utils.commandline.{ Argument }

trait MultiSampleQScript extends BiopetQScript {
  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: List[String] = Nil

  if (!Config.global.map.contains("samples")) logger.warn("No Samples found in config")

  abstract class AbstractSample(val sampleId: String) {
    val config = new ConfigFunctions(defaultSample = Some(sampleId))

    abstract class AbstractLibrary(val libraryId: String) {
      val config = new ConfigFunctions(defaultSample = Some(sampleId), defaultLibrary = Some(libraryId))
      final def run(): Unit = {
        currentSample = Some(sampleId)
        currentLibrary = Some(libraryId)
        runJobs()
        currentLibrary = None
        currentSample = None
      }

      def getLibraryDir: String = {
        getSampleDir + "libraries" + File.pathSeparator + libraryId + File.pathSeparator
      }

      protected def runJobs()
    }

    type Library <: AbstractLibrary

    val libraries: Map[String, Library] = getLibrariesIds.map(id => id -> initClass(id)).toMap

    protected def getLibrariesIds: Set[String] = {
      ConfigUtils.getMapFromPath(Config.global.map, List("samples", sampleId, "libraries")).getOrElse(Map()).keySet
    }

    final def run(): Unit = {
      currentSample = Some(sampleId)
      runJobs()
      currentSample = None
    }

    protected def runJobs()

    protected final def runLibraryJobs(): Unit = {
      for ((libraryId, library) <- libraries) {
        library.run()
      }
    }

    def getSampleDir: String = {
      outputDir + "samples" + File.pathSeparator + sampleId + File.pathSeparator
    }
  }

  type Sample <: AbstractSample

  final private def initClass[T: ClassTag](arg: String): T = {
    logger.debug("init of: " + classTag[T])
    val x = classTag[T].runtimeClass.getConstructor(classOf[String]).newInstance(arg).asInstanceOf[T]
    logger.debug("init of: " + classTag[T] + "  Done")
    x
  }

  val samples: Map[String, Sample] = getSamplesIds.map(id => id -> initClass(id)).toMap

  /** Returns a list of all sampleIDs */
  protected def getSamplesIds: Set[String] = if (onlySample != Nil) onlySample.toSet else {
    ConfigUtils.any2map(Config.global.map.getOrElse("samples", Map())).keySet
  }

  /** Runs runSingleSampleJobs method for each sample */
  final def runSamplesJobs() {
    for ((sampleId, sample) <- samples) {
      sample.run()
    }
  }

  private var currentSample: Option[String] = None
  private var currentLibrary: Option[String] = None

  override protected[core] def configFullPath: List[String] = {
    val s = currentSample match {
      case Some(s) => "samples" :: s :: Nil
      case _       => Nil
    }
    val l = currentLibrary match {
      case Some(l) => "libraries" :: l :: Nil
      case _       => Nil
    }
    s ::: l ::: super.configFullPath
  }
}
