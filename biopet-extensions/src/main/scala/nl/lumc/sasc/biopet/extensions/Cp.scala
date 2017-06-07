package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 30/05/2017.
  */
class Cp(val parent: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Source file", required = true)
  var source: File = _

  @Output(doc = "Target file", required = true)
  var target: File = _

  executable = config("exe", default = "cp")

  /** Returns command to execute */
  def cmdLine =
    required(executable) +
      required(source) +
      required(target)
}

object Cp {
  def apply(parent: Configurable, source: File, target: File): Cp = {
    val cp = new Cp(parent)
    cp.source = source
    cp.target = target
    cp
  }
}