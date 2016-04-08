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
/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.gatk.engine.phonehome.GATKRunReport
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with CommandLineResources with Reference with Version {
  var executable: String = config("java", default = "java", namespace = "java", freeVar = false)

  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  reference_sequence = referenceFasta()

  override def defaultCoreMemory = 4.0
  override def faiRequired = true
  override def dictRequired = true

  if (config.contains("intervals")) intervals = config("intervals").asFileList
  if (config.contains("exclude_intervals")) excludeIntervals = config("exclude_intervals").asFileList

  Option(config("et").value) match {
    case Some("NO_ET")  => et = GATKRunReport.PhoneHomeOption.NO_ET
    case Some("AWS")    => et = GATKRunReport.PhoneHomeOption.AWS
    case Some("STDOUT") => et = GATKRunReport.PhoneHomeOption.STDOUT
    case Some(x)        => throw new IllegalArgumentException(s"Unknown et option for gatk: $x")
    case _              =>
  }

  if (config.contains("gatk_key")) gatk_key = config("gatk_key")
  if (config.contains("pedigree")) pedigree = config("pedigree")

  def versionRegex = """(.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = "java" + " -jar " + jarFile + " -version"

  override def getVersion = {
    BiopetCommandLineFunction.preProcessExecutable(executable).path.foreach(executable = _)
    super.getVersion.collect { case v => "Gatk " + v }
  }
}
