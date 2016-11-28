package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmMatrix(val root: Configurable) extends Xhmm {

  @Input
  var inputMatrix: File = _

  @Output
  var outputMatrix: File = _

  @Output(required = false)
  var outputExcludedTargets: Option[File] = None

  @Output(required = false)
  var outputExcludedSamples: Option[File] = None

  @Input(required = false)
  var inputExcludeTargets: List[File] = Nil

  @Input(required = false)
  var inputExcludeSamples: List[File] = Nil

  @Argument
  var minTargetSize: Int = config("min_target_size", namespace = "xhmm", default = 10)

  @Argument
  var maxTargetSize: Int = config("max_target_size", namespace = "xhmm", default = 10000)

  @Argument
  var minMeanTargetRD: Int = config("min_mean_target_rd", namespace = "xhmm", default = 10)

  @Argument
  var maxMeanTargetRD: Int = config("max_mean_target_rd", namespace = "xhmm", default = 500)

  @Argument
  var minMeanSampleRD: Int = config("min_mean_sample_rd", namespace = "xhmm", default = 25)

  @Argument
  var maxMeanSampleRD: Int = config("max_mean_sample_rd", namespace = "xhmm", default = 200)

  @Argument
  var maxSdSampleRD: Int = config("max_sd_sample_rd", namespace = "xhmm", default = 150)

  @Argument
  var maxsdTargetRD: Int = config("max_sd_target_rd", namespace = "xhmm", default = 30)

  @Argument
  var centerData: Boolean = false

  @Argument
  var centerType: String = "sample"

  @Argument
  var zScoreData: Boolean = false

  private def subCmdLine = {
    if (inputExcludeSamples.nonEmpty && inputExcludeTargets.nonEmpty) {
      repeat("--excludeTargets", inputExcludeTargets) + repeat("--excludeSamples", inputExcludeSamples)
    } else if (centerData && zScoreData) {
      conditional(centerData, "--centerData") +
        required("--centerType", centerType) +
        conditional(zScoreData, "--zScoreData") +
        required("--maxSdTargetRD", maxsdTargetRD)
    } else {
      required("--minTargetSize", minTargetSize) +
        required("--maxTargetSize", maxTargetSize) +
        required("--minMeanTargetRD", minMeanTargetRD) +
        required("--maxMeanTargetRD", maxMeanTargetRD) +
        required("--minMeanSampleRD", minMeanSampleRD) +
        required("--maxMeanSampleRD", maxMeanSampleRD) +
        required("--maxSdSampleRD", maxSdSampleRD)
    }

  }

  def cmdLine = {
    executable + required("--matrix") +
      required("-r", inputMatrix) +
      required("-o", outputMatrix) +
      optional("--outputExcludedTargets", outputExcludedTargets) +
      optional("--outputExcludedSamples", outputExcludedSamples) + subCmdLine
  }

}
