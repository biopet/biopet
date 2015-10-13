/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import nl.lumc.sasc.biopet.core.{Version, CommandLineResources, Reference, BiopetJavaCommandLineFunction}
import org.broadinstitute.gatk.engine.phonehome.GATKRunReport
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with CommandLineResources with Reference with Version {
  memoryLimit = Option(3)

  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  reference_sequence = referenceFasta()

  override def defaultCoreMemory = 4.0
  override def faiRequired = true

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

  override def getVersion = super.getVersion.collect { case v => "Gatk " + v }
}
