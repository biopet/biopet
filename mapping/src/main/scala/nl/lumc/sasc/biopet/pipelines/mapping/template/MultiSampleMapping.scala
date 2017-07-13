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
package nl.lumc.sasc.biopet.pipelines.mapping.template

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, TemplateTool}
import nl.lumc.sasc.biopet.utils.Question

/**
  * Created by pjvanthof on 17/12/2016.
  */
object MultiSampleMapping extends TemplateTool {

  def pipelineName = "MultiSampleMapping"

  override def sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

  def possibleAligners =
    List("bwa-mem",
         "bwa-aln",
         "bowtie",
         "bowtie2",
         "gsnap",
         "hisat2",
         "tophat",
         "stampy",
         "star",
         "star-2pass")

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    val referenceConfig = map ++ Reference.askReference

    val aligner =
      if (map.contains("aligner")) map("aligner").toString
      else Question.string("Aligner", possibleValues = possibleAligners, default = Some("bwa-mem"))
    val mappingToGears = Question.string("Reads to process in metagenomics pipeline",
                                         possibleValues = List("none", "all", "unmapped"),
                                         default = Some("none"))

    referenceConfig ++ Map(
      "aligner" -> aligner,
      "mapping_to_gears" -> mappingToGears
    )
  }
}
