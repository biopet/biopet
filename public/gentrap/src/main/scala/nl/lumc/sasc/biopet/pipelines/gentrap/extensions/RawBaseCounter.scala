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
package nl.lumc.sasc.biopet.pipelines.gentrap.extensions

import java.io.File
import scala.language.reflectiveCalls

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

/** Ad-hoc extension for counting bases that involves 3-command pipe */
// FIXME: generalize piping instead of building something by hand like this!
// Better to do everything quick and dirty here rather than something half-implemented with the objects
class RawBaseCounter(val root: Configurable) extends BiopetCommandLineFunction { wrapper =>

  @Input(doc = "Reference BED file", required = true)
  var annotationBed: File = null

  @Input(doc = "Input BAM file from both strands", required = false)
  var inputBoth: File = null

  @Input(doc = "Input BAM file from the plus strand", required = false)
  var inputPlus: File = null

  @Input(doc = "Input BAM file from the minus strand", required = false)
  var inputMinus: File = null

  @Output(doc = "Output base count file", required = true)
  var output: File = null

  /** Internal flag for mixed strand mode */
  private lazy val mixedStrand: Boolean = inputBoth != null && inputPlus == null && inputMinus == null

  /** Internal flag for distinct strand / strand-specific mode */
  private lazy val distinctStrand: Boolean = inputBoth == null && inputPlus != null && inputMinus != null

  private def grepForStrand = new BiopetCommandLineFunction {
    var strand: String = null
    override val root: Configurable = wrapper.root
    executable = config("exe", default = "grep", freeVar = false)
    override def cmdLine: String = required(executable) +
      required("-P", """\""" + strand + """$""") +
      required(annotationBed)
  }

  private def bedtoolsCovHist = new BiopetCommandLineFunction {
    var bam: File = null
    override val root: Configurable = wrapper.root
    executable = config("exe", default = "coverageBed", freeVar = false)
    override def cmdLine: String = required(executable) +
      required("-split") +
      required("-hist") +
      required("-abam", bam) +
      required("-b", if (mixedStrand) annotationBed else "stdin")
  }

  private def hist2Count = new PythonCommandLineFunction {
    setPythonScript("hist2count.py", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")
    override val root: Configurable = wrapper.root
    def cmdLine = getPythonCommand + optional("-c", "3")
  }

  override def beforeGraph: Unit = {
    require(annotationBed != null, "Annotation BED must be supplied")
    require(output != null, "Output must be defined")
    require((mixedStrand && !distinctStrand) || (!mixedStrand && distinctStrand),
      "Invalid input BAM combinations for RawBaseCounter")
  }

  def cmdLine: String =
    if (mixedStrand && !distinctStrand) {

      val btCov = bedtoolsCovHist
      btCov.bam = inputBoth
      btCov.commandLine + "|" + hist2Count.commandLine + " > " + output

    } else {

      val plusGrep = grepForStrand
      plusGrep.strand = "+"
      val plusBtCov = bedtoolsCovHist
      plusBtCov.bam = inputPlus

      val minusGrep = grepForStrand
      minusGrep.strand = "-"
      val minusBtCov = bedtoolsCovHist
      minusBtCov.bam = inputMinus

      plusGrep.commandLine + "|" + plusBtCov.commandLine + "|" + hist2Count.commandLine + " > " + required(output) + " && " +
        minusGrep.commandLine + "|" + minusBtCov.commandLine + "|" + hist2Count.commandLine + " >> " + required(output)
    }
}
