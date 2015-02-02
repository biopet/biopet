package nl.lumc.sasc.biopet.extensions.macs2

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

class Macs2CallPeak(val root: Configurable) extends Macs2 {
  @Input(doc = "Treatment input", required = true)
  var treatment: File = _

  @Input(doc = "Control input", required = false)
  var control: File = _

  @Output(doc = "Output file NARROWPEAKS")
  private var output_narrow: File = _

  @Output(doc = "Output file BROADPEAKS")
  private var output_broad: File = _

  @Output(doc = "Output in Excel format")
  private var output_xls: File = _

  @Output(doc = "R script with Bimodal model")
  private var output_r: File = _

  @Output(doc = "Output file Bedgraph")
  private var output_bdg: File = _

  @Output(doc = "Output file gappedPeak")
  private var output_gapped: File = _

  var fileformat: String = config("fileformat")
  var gsize: Option[Float] = config("gsize")
  var keepdup: Boolean = config("keep-dup", default = false)
  var buffersize: Option[Int] = config("buffer-size")
  var outputdir: String = config("outputDir")
  var name: String = config("name")
  var bdg: Boolean = config("bdg", default = false)
  var verbose: Boolean = config("verbose", default = false)
  var tsize: Option[Int] = config("tsize")
  var bandwith: Option[Int] = config("bandwith")
  var mfold: Option[Float] = config("mfold")
  var fixbimodel: Boolean = config("fixbimodel", default = false)
  var nomodel: Boolean = config("nomodel", default = false)
  var shift: Option[Int] = config("shift")
  var qvalue: Option[Float] = config("qvalue")
  var pvalue: Option[Float] = config("pvalue")
  var tolarge: Boolean = config("tolarge", default = false)
  var downsample: Boolean = config("downsample", default = false)
  var nolambda: Boolean = config("nolambda", default = false)
  var slocal: Option[Int] = config("slocal")
  var llocal: Option[Int] = config("llocal")
  var broad: Boolean = config("broad", default = false)
  var broadcutoff: Option[Int] = config("broadcutoff")
  var callsummits: Boolean = config("callsummits", default = false)

  override def afterGraph: Unit = {
    output_narrow = new File(outputdir + name + ".narrowPeak")
    output_broad = new File(outputdir + name + ".broadPeak")
    output_xls = new File(outputdir + name + ".xls")
    output_bdg = new File(outputdir + name + ".bdg")
    output_r = new File(outputdir + name + ".r")
    output_gapped = new File(outputdir + name + ".gappedPeak")
  }

  def cmdLine = {
    required(executable) + required("callpeak") +
      required("--treatment", treatment) + /* Treatment sample */
      optional("--control", control) + /* Control sample */
      optional("-f", fileformat) + /* Input file format */
      required("-g", gsize) + /* Estimated genome size.(format: 2.7e9) (presets: hs, mm, ce, dm) */
      conditional(keepdup, "--keep-dup") + /* Whether to keep duplicates */
      optional("--buffer-size", buffersize) + /* Buffer size */
      required("--outdir", outputdir) + /* The output directory */
      optional("--name", name) + /* prefix name of the output files. (note that also the peak names inside the files will have this name */
      conditional(bdg, "-B") + /* Whether to output in BDG format */
      conditional(verbose, "--verbose") + /* Whether to output verbosely */
      optional("--tsize", tsize) + /* Sets custom tag length, if not specified macs will use first 10 sequences to estimate the size */
      optional("--bw", bandwith) + /* The bandwith to use for model building. Set this parameter as the sonication fragment size estimated in the wetlab */
      optional("--mfold", mfold) + /* The parameter to select regions within the model fold. Must be a upper and lower limit. */
      conditional(fixbimodel, "--fix-bimodal") + /* Whether turn on the auto paired-peak model process. If it's set, when MACS failed to build paired model, it will use the nomodel settings, the '--extsize' parameter to extend each tags. If set, MACS will be terminated if paried-peak model is failed. */
      conditional(nomodel, "--nomodel") + /* While on, MACS will bypass building the shifting model */
      optional("--shift", shift) + /* You can set an arbitrary shift in basepairs here */
      optional("--extsize", shift) + /* While '--nomodel' is set, MACS uses this parameter to extend reads in 5'->3' direction to fix-sized fragments. For example, if the size of binding region for your transcription factor is 200 bp, and you want to bypass the model building by MACS, this parameter can be set as 200. This option is only valid when --nomodel is set or when MACS fails to build model and --fix-bimodal is on. */
      optional("--qvalue", qvalue) + /* the Q-value(FDR) cutoff */
      optional("--pvalue", pvalue) + /* The P-value cutoff, if --pvalue is set no Qvalue is calculated */
      conditional(tolarge, "--to-large") + /* Whether to scale up the smallest input file to the larger one */
      conditional(downsample, "--down-sample") + /* This is the reversed from --to-large */
      conditional(nolambda, "--nolambda") + /* With this flag on, MACS will use the background lambda as local lambda. This means MACS will not consider the local bias at peak candidate regions.*/
      optional("--slocal", slocal) + /* These two parameters control which two levels of regions will be checked around the peak regions to calculate the maximum lambda as local lambda */
      optional("--llocal", llocal) +
      conditional(broad, "--broad") + /* whether to enable broad peak calling */
      optional("--broad-cutoff", broadcutoff) + /* Cutoff for broad region. This option is not available unless --broad is set. If -p is set, this is a pvalue cutoff, otherwise, it's a qvalue cutoff. */
      conditional(callsummits, "--call-summits") /* MACS will now reanalyze the shape of signal profile (p or q-score depending on cutoff setting) to deconvolve subpeaks within each peak called from general procedure. */
  }
}
