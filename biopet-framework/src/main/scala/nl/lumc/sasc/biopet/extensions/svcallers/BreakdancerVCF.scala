package nl.lumc.sasc.biopet.extensions.svcallers

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

class BreakdancerVCF(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("breakdancer2vcf.py")

  @Input(doc = "Breakdancer TSV")
  var input: File = _

  @Output(doc = "Output VCF to PATH")
  var output: File = _

  def cmdLine = {
    getPythonCommand +
      "-i " + required(input) +
      "-o " + required(output)
  }
}

object BreakdancerVCF {
  def apply(root: Configurable, input: File, output: File): BreakdancerVCF = {
    val bd = new BreakdancerVCF(root)
    bd.input = input
    bd.output = output
    return bd
  }
}