/*
 * Copyright 2014 wyleung.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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