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
package nl.lumc.sasc.biopet.extensions.conifer

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

class ConiferAnalyze(val root: Configurable) extends Conifer {

  @Input(doc = "Probes / capture kit definition as bed file: chr,start,stop,gene-annot", required = true)
  var probes: File = _

  @Input(doc = "Path to Conifer RPKM files", required = true)
  var rpkm_dir: File = _

  @Output(doc = "Output RPKM.txt", shortName = "out")
  var output: File = _

  @Argument(doc = "SVD, number of components to remove", minRecommendedValue = 2, maxRecommendedValue = 5,
    minValue = 2, maxValue = 20)
  var svd: Option[Int] = config("svd")

  @Argument(doc="Minimum population median RPKM per probe")
  var min_rpkm: Option[Double] = config("min_rpkm")

  override def afterGraph {
    this.checkExecutable
  }

  def cmdLine = required(executable) +
    required("rpkm")+
    " --probes" + required(probes) +
    " --rpkm_dir" + required(rpkm_dir) +
    " --output" + required(output) +
    optional("--svd",svd) +
    optional("--min_rpkm", min_rpkm)
}
