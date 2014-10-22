package nl.lumc.sasc.biopet.extensions.svcallers

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File


class BreakdancerConfig(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "bam2cfg.pl", freeVar = false)

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output File")
  var output: File = _

  var min_mq: Option[Int] = config("min_mq", default = 20) // minimum of MQ to consider for taking read into histogram
  var use_mq: Boolean = config("use_mq", default = false)
  var min_insertsize: Option[Int] = config("min_insertsize", default = 450)
  var solid_data: Boolean = config("solid", default = false)
  var sd_cutoff: Option[Int] = config("sd_cutoff", default = 4) // Cutoff in unit of standard deviation [4]
  
  // we set this to a higher number to avoid biases in small numbers in sorted bams
  var min_observations: Option[Int] = config("min_observations", default = 10000) //  Number of observation required to estimate mean and s.d. insert size [10_000]
  var coefvar_cutoff: Option[Int] = config("coef_cutoff", default = 1) // Cutoff on coefficients of variation [1]
  var histogram_bins: Option[Int] = config("histogram_bins", default = 50) // Number of bins in the histogram [50]
  
  def cmdLine = required(executable) +
      optional("-q", min_mq) +
      conditional(use_mq, "-m") +
      optional("-s", min_insertsize) +
      conditional(solid_data, "-s") +
      optional("-c", sd_cutoff) +
      optional("-n", min_observations) +
      optional("-v", coefvar_cutoff) +
      optional("-b", histogram_bins) +
      required(input) + " 1> " + required(output)
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




/*
 * The caller
 * 
 **/


class BreakdancerCaller(val root: Configurable) extends BiopetCommandLineFunction  {
  executable = config("exe", default = "breakdancer-max", freeVar = false)
  
  override val defaultVmem = "4G"
  override val defaultThreads = 1 // breakdancer can only work on 1 single thread
  
  override val versionRegex = """.*[Vv]ersion:? (.*)""".r
  override val versionExitcode = List(1)
  override def versionCommand = executable
  
  
  @Input(doc = "The breakdancer configuration file")
  var input: File = _
  
//  @Argument(doc = "Work directory")
//  var workdir: String = _
  
  @Output(doc = "Breakdancer VCF output")
  var output: File = _
  
  /*
   Options: 
       -o STRING       operate on a single chromosome [all chromosome]
       -s INT          minimum length of a region [7]
       -c INT          cutoff in unit of standard deviation [3]
       -m INT          maximum SV size [1000000000]
       -q INT          minimum alternative mapping quality [35]
       -r INT          minimum number of read pairs required to establish a connection [2]
       -x INT          maximum threshold of haploid sequence coverage for regions to be ignored [1000]
       -b INT          buffer size for building connection [100]
       -t              only detect transchromosomal rearrangement, by default off
       -d STRING       prefix of fastq files that SV supporting reads will be saved by library
       -g STRING       dump SVs and supporting reads in BED format for GBrowse
       -l              analyze Illumina long insert (mate-pair) library
       -a              print out copy number and support reads per library rather than per bam, by default off
       -h              print out Allele Frequency column, by default off
       -y INT          output score filter [30]
   */

  var s: Option[Int] = config("s", default = 7)
  var c: Option[Int] = config("c", default = 3)
  var m: Option[Int] = config("m", default = 1000000000)
  var q: Option[Int] = config("qs", default = 35)
  var r: Option[Int] = config("r", default = 2)
  var x: Option[Int] = config("x", default = 1000)
  var b: Option[Int] = config("b", default = 100)  
  var t: Boolean = config("t", default = false)
  var d: String = config("d")
  var g: String = config("g")
  var l: Boolean = config("l", default = false)
  var a: Boolean = config("a", default = false)
  var h: Boolean = config("h", default = false)
  var y: Option[Int] = config("y", default = 30)  
  
  override def beforeCmd {
  }

  def cmdLine = required(executable) + 
      optional("-s", s) +
      optional("-c", c) +
      optional("-m", m) +
      optional("-q", q) +
      optional("-r", r) +
      optional("-x", x) +
      optional("-b", b) +
      conditional(t ,"-t") +
      optional("-d", d) +
      optional("-g", g) +
      conditional(l ,"-l") +
      conditional(a ,"-a") +
      conditional(h ,"-h") +
      optional("-y", y) +
      required(input) + 
      ">" + 
      required(output)
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
  
//  @Output(doc = "Breakdancer VCF output")
//  lazy val outputvcf: File = {
//    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.vcf")
//  }
  
  @Output(doc = "Breakdancer config")
  lazy val configfile: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.cfg")
  }
  @Output(doc = "Breakdancer raw output")
  lazy val outputraw: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.tsv")
  }
  
  override def init() {
  }

  def biopetScript() {
    // read config and set all parameters for the pipeline
    logger.info("Starting Breakdancer configuration")
    
    val bdcfg = BreakdancerConfig(this, input, this.configfile)
    outputFiles += ("breakdancer_cfg" -> bdcfg.output )
    add( bdcfg )
    
    val output_tsv: File = this.outputraw
    val breakdancer = BreakdancerCaller( this, bdcfg.output, output_tsv )
    add( breakdancer )
    outputFiles += ("breakdancer_tsv" -> breakdancer.output )

//    val output_vcf: File = this.outputvcf
    // convert this tsv to vcf using the python script
    
    
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