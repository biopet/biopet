package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 2/26/15.
 */
abstract class Gatk extends BiopetJavaCommandLineFunction {
  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  val analysisType: String

  override val defaultVmem = "5G"

  @Input(required = true)
  var reference: File = config("reference")

  @Input(required = false)
  var gatkKey: File = config("gatk_key")

  @Input(required = false)
  var intervals: List[File] = config("intervals")

  @Input(required = false)
  var excludeIntervals: List[File] = config("exclude_intervals")

  @Input(required = false)
  var pedigree: List[File] = config("pedigree")

  override def commandLine = super.commandLine +
    required("-T", analysisType) +
    required("-R", reference) +
    optional("-K", gatkKey) +
    repeat("-I", intervals) +
    repeat("-XL", excludeIntervals) +
    repeat("-ped", pedigree)
}