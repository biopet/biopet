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
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 23-11-16.
  */
class XhmmMatrix(val parent: Configurable) extends Xhmm {

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

  var minTargetSize: Int = config("min_target_size", namespace = "xhmm_matrix", default = 10)

  var maxTargetSize: Int = config("max_target_size", namespace = "xhmm_matrix", default = 10000)

  var minMeanTargetRD: Int = config("min_mean_target_rd", namespace = "xhmm_matrix", default = 10)

  var maxMeanTargetRD: Int = config("max_mean_target_rd", namespace = "xhmm_matrix", default = 500)

  var minMeanSampleRD: Int = config("min_mean_sample_rd", namespace = "xhmm_matrix", default = 25)

  var maxMeanSampleRD: Int = config("max_mean_sample_rd", namespace = "xhmm_matrix", default = 200)

  var maxSdSampleRD: Int = config("max_sd_sample_rd", namespace = "xhmm_matrix", default = 150)

  var maxsdTargetRD: Int = config("max_sd_target_rd", namespace = "xhmm_matrix", default = 30)

  var centerData: Boolean = false

  var centerType: String = "sample"

  var zScoreData: Boolean = false

  private def subCmdLine = {
    if (inputExcludeSamples.nonEmpty && inputExcludeTargets.nonEmpty) {
      repeat("--excludeTargets", inputExcludeTargets) + repeat("--excludeSamples",
                                                               inputExcludeSamples)
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
