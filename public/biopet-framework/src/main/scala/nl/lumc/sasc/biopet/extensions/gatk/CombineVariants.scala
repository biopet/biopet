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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for CombineVariants from GATK
 *
 * Created by pjvan_thof on 2/26/15.
 */
class CombineVariants(val root: Configurable) extends Gatk {
  val analysisType = "CombineVariants"

  @Input(doc = "", required = true)
  var inputFiles: List[File] = Nil

  @Output(doc = "", required = true)
  var outputFile: File = null

  var setKey: String = null
  var rodPriorityList: String = null
  var minimumN: Int = config("minimumN", default = 1)
  var genotypeMergeOptions: Option[String] = config("genotypeMergeOptions")
  var excludeNonVariants: Boolean = false

  var inputMap: Map[File, String] = Map()

  def addInput(file: File, name: String): Unit = {
    inputFiles :+= file
    inputMap += file -> name
  }

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    genotypeMergeOptions match {
      case Some("UNIQUIFY") | Some("PRIORITIZE") | Some("UNSORTED") | Some("REQUIRE_UNIQUE") | None =>
      case _ => throw new IllegalArgumentException("Wrong option for genotypeMergeOptions")
    }
  }

  override def commandLine = super.commandLine +
    (for (file <- inputFiles) yield {
      inputMap.get(file) match {
        case Some(name) => required("-V:" + name, file)
        case _          => required("-V", file)
      }
    }).mkString +
    required("-o", outputFile) +
    optional("--setKey", setKey) +
    optional("--rod_priority_list", rodPriorityList) +
    optional("-genotypeMergeOptions", genotypeMergeOptions) +
    conditional(excludeNonVariants, "--excludeNonVariants")
}
