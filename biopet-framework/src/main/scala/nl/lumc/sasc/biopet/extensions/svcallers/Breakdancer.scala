package nl.lumc.sasc.biopet.extensions.svcallers

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File


class BreakdancerConfig(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "bam2cfg.pl", freeVar = false)
  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output File")
  var output: File = _

  var MIN_MQ: Option[Int] = config("min_mq", default = 20) // minimum of MQ to consider for taking read into histogram
  var USE_MQ: Boolean = config("use_mq", default = false)
  var MIN_INSERTSIZE: Option[Int] = config("min_insertsize", default = 450)
  var SOLID_DATA: Boolean = config("solid", default = false)
  var SD_CUTOFF: Option[Int] = config("sd_cutoff", default = 4) // Cutoff in unit of standard deviation [4]
  
  // we set this to a higher number to avoid biases in small numbers in sorted bams
  var MIN_OBSERVATIONS: Option[Int] = config("min_observations", default = 1000000) //  Number of observation required to estimate mean and s.d. insert size [10_000]
  var COEFVAR_CUTOFF: Option[Int] = config("coef_cutoff", default = 1) // Cutoff on coefficients of variation [1]
  var HISTOGRAM_BINS: Option[Int] = config("histogram_bins", default = 50) // Number of bins in the histogram [50]
  
  def cmdLine = required(executable) +
      optional("-q", MIN_MQ) +
      conditional(USE_MQ, "-m") +
      optional("-s", MIN_INSERTSIZE) +
      conditional(SOLID_DATA, "-s") +
      optional("-c", SD_CUTOFF) +
      optional("-n", MIN_OBSERVATIONS) +
      optional("-v", COEFVAR_CUTOFF) +
      optional("-b", HISTOGRAM_BINS)
      required(input) + " > " + required(output)
}

object BreakdancerConfig {
  def apply(root: Configurable, input: File, output: File): BreakdancerConfig = {
    val bdconf = new BreakdancerConfig(root)
    bdconf.input = input
    bdconf.output = output
    return bdconf
  }

  def apply(root: Configurable, input: File, outputDir: String): BreakdancerConfig = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): BreakdancerConfig = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".breakdancer.cfg"
}







class BreakdancerCaller(val root: Configurable) extends BiopetCommandLineFunction  {
  executable = config("exe", default = "breakdancer-max", freeVar = false)
  
  override val defaultVmem = "4G"
  override val defaultThreads = 8
  
  override val versionRegex = """.*[Vv]ersion:? (.*)""".r
//  override val versionExitcode = List(0, 1)
  override def versionCommand = executable
  
  
  @Input(doc = "Input file (bam)")
  var input: File = _
  
  @Argument(doc = "Work directory")
  var workdir: String = _
  
  @Output(doc = "Breakdancer VCF output")
  var output: File = _
  
//  var T: Option[Int] = config("T", default = defaultThreads)
  var f: Boolean = config("f", default = true) // delete work directory before running
//  var w: String = config("w", default = workdir + "/work")
  var a: Boolean = config("a", default = false) // don't recompute AS tags
  var k: Boolean = config("k", default = false) // keep working directory
  var r: Boolean = config("r", default = false) // take read groups into account
  
  override def beforeCmd {
    if (workdir == null) throw new Exception("Breakdancer :: Workdirectory is not defined")
//    if (input.getName.endsWith(".sort.bam")) sorted = true
  }

  def cmdLine = required(executable) + 
      required(input) + ">" + required(output)
}

object BreakdancerCaller {
  def apply(root: Configurable, input: File, output: File): BreakdancerCaller = {
    val bdcaller = new BreakdancerCaller(root)
    bdcaller.input = input
    bdcaller.output = output
    return bdcaller
  }
}

/// Breakdancer is actually a mini pipeline executing binaries from the breakdancer package
class Breakdancer(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)
  
  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference Fasta file")
  var reference: File = _
  
  @Argument(doc = "Work directory")
  var workdir: String = _
  
  @Output(doc = "Breakdancer VCF output")
  var output: File = _
  
  override def init() {
  }

  def biopetScript() {
    // write the pipeline here
    // start with QC, alignment, call sambamba, call sv callers, reporting

    // read config and set all parameters for the pipeline
    logger.info("Starting Breakdancer")
    
    val bdcfg = BreakdancerConfig(this, input, workdir)
    outputFiles += ("breakdancer_cfg" -> bdcfg.output )
    add( bdcfg )
    
    val output_vcf: File = new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.tsv")
    val breakdancer = BreakdancerCaller( this, input, output_vcf )
    
    // convert this tsv to vcf using the python script
    
    
  }
  
  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".breakdancer.tsv"
}

object Breakdancer {
  def apply(root: Configurable, input: File, reference: File, runDir: String): Breakdancer = {
    val breakdancer = new Breakdancer(root)
    breakdancer.input = input
    breakdancer.reference = reference
    breakdancer.workdir = runDir
    return breakdancer
  }
}