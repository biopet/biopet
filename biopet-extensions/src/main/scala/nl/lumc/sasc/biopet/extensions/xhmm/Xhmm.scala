package nl.lumc.sasc.biopet.extensions.xhmm

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}

/**
  * Created by Sander Bollen on 23-11-16.
  *
  * Generic abstract class for XHMM commands
  */
abstract class Xhmm extends BiopetCommandLineFunction with Version  {

  executable = config("exe", namespace = "xhmm", default = "xhmm")

  def versionCommand = executable + "--version"
  def versionRegex = """xhmm (.*)""".r
}
