package nl.lumc.sasc.biopet.extensions.svcallers

import java.io.File

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.PipelineCommand

/// Breakdancer is actually a mini pipeline executing binaries from the breakdancer package
class Breakdancer(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference Fasta file")
  var reference: File = _

  @Argument(doc = "Work directory")
  var workdir: String = _

  var deps: List[File] = Nil

  @Output(doc = "Breakdancer config")
  lazy val configfile: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.cfg")
  }
  @Output(doc = "Breakdancer raw output")
  lazy val outputraw: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.tsv")
  }
  @Output(doc = "Breakdancer VCF output")
  lazy val outputvcf: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.vcf")
  }

  override def init() {
  }

  def biopetScript() {
    // read config and set all parameters for the pipeline
    logger.info("Starting Breakdancer configuration")

    val bdcfg = BreakdancerConfig(this, input, this.configfile)
    bdcfg.deps = this.deps
    outputFiles += ("cfg" -> bdcfg.output)
    add(bdcfg)

    val breakdancer = BreakdancerCaller(this, bdcfg.output, this.outputraw)
    add(breakdancer)
    outputFiles += ("tsv" -> breakdancer.output)

    val bdvcf = BreakdancerVCF(this, breakdancer.output, this.outputvcf)
    add(bdvcf)
    outputFiles += ("vcf" -> bdvcf.output)
  }
}

object Breakdancer extends PipelineCommand {
  def apply(root: Configurable, input: File, reference: File, runDir: String): Breakdancer = {
    val breakdancer = new Breakdancer(root)
    breakdancer.input = input
    breakdancer.reference = reference
    breakdancer.workdir = runDir
    breakdancer.init
    breakdancer.biopetScript
    return breakdancer
  }
}