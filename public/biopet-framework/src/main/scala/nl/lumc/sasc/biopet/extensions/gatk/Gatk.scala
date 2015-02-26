package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 2/26/15.
 */
class Gatk(val root: Configurable) extends BiopetJavaCommandLineFunction {
  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  override val defaultVmem = "5G"

  @Input(required = true)
  val reference: File = config("reference")

  @Input(required = false)
  val gatkKey: File = config("gatk_key")

  //val intervals: List[File] = config("intervals")
}