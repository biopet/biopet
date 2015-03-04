package nl.lumc.sasc.biopet.pipelines.gentrap.extensions

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ Bgzip, PythonCommandLineFunction, Tabix }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.extensions.varscan.Mpileup2cns

/** Ad-hoc extension for VarScan variant calling that involves 6-command pipe */
// FIXME: generalize piping instead of building something by hand like this!
// Better to do everything quick and dirty here rather than something half-implemented with the objects
class CustomVarScan(val root: Configurable) extends BiopetCommandLineFunction { wrapper =>

  @Input(doc = "Input BAM file", required = true)
  var input: File = null

  @Input(doc = "Reference FASTA file", required = true)
  var reference: File = config("reference")

  @Output(doc = "Output VCF file", required = true)
  var output: File = null

  // mpileup, varscan, fix_mpileup.py, binom_test.py, bgzip, tabix
  private def mpileup = new SamtoolsMpileup(wrapper.root) {
    this.input = wrapper.input
    disableBaq = true
    reference = config("reference")
    depth = Option(1000000)
    outputMappingQuality = true
  }

  private def fixMpileup = new PythonCommandLineFunction {
    setPythonScript("fix_mpileup.py", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")
    override val root: Configurable = wrapper.root
    def cmdLine = getPythonCommand
  }

  private def removeEmptyPile = new BiopetCommandLineFunction {
    override val root: Configurable = wrapper.root
    executable = "grep"
    override def cmdLine: String = required(executable) + required("-vP") + required("""\t\t""")
  }

  private def varscan = new Mpileup2cns(wrapper.root) {
    strandFilter = Option(0)
    outputVcf = Option(1)
  }

  private def compress = new Bgzip(wrapper.root) {
    this.output = wrapper.output
  }

  override def beforeGraph: Unit = {
    require(output.toString.endsWith(".gz"), "Output must have a .gz file extension")
  }

  def cmdLine: String =
    mpileup.cmdPipe + " | " + fixMpileup.commandLine + " | " + removeEmptyPile.commandLine + " | " +
    varscan.commandLine + " | " + compress.commandLine
}
