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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetJavaCommandLineFunction }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.engine.phonehome.GATKRunReport
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 2/26/15.
 */
abstract class Gatk extends BiopetJavaCommandLineFunction with Reference {
  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  val analysisType: String

  override val defaultCoreMemory = 3.0

  @Input(required = true)
  var reference: File = null

  @Input(required = false)
  var gatkKey: Option[File] = config("gatk_key")

  @Input(required = false)
  var intervals: List[File] = config("intervals", default = Nil)

  @Input(required = false)
  var excludeIntervals: List[File] = config("exclude_intervals", default = Nil)

  @Input(required = false)
  var pedigree: List[File] = config("pedigree", default = Nil)

  var et: Option[String] = config("et")

  override def dictRequired = true

  override def beforeGraph: Unit = {
    super.beforeGraph
    if (reference == null) reference = referenceFasta()
  }

  override def commandLine = super.commandLine +
    required("-T", analysisType) +
    required("-R", reference) +
    optional("-K", gatkKey) +
    optional("-et", et) +
    repeat("-I", intervals) +
    repeat("-XL", excludeIntervals) +
    repeat("-ped", pedigree)
}