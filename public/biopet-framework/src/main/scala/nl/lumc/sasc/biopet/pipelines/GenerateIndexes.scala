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
package nl.lumc.sasc.biopet.pipelines

import java.io.File

import nl.lumc.sasc.biopet.core.{PipelineCommand, BiopetQScript}
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFaidx
import nl.lumc.sasc.biopet.extensions.{Zcat, Curl}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline

class GenerateIndexes(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Argument
  var referenceConfigFile: File = _

  var referenceConfig: Map[String, Any] = Map()

  /** This is executed before the script starts */
  def init(): Unit = {
    referenceConfig = ConfigUtils.fileToConfigMap(referenceConfigFile)
  }

  /** Method where jobs must be added */
  def biopetScript(): Unit = {

    for ((speciesName, c) <- referenceConfig) {
      val speciesConfig = ConfigUtils.any2map(c)
      val speciesDir = new File(outputDir, speciesName)
      for ((genomeName, c) <- speciesConfig) {
        val genomeConfig = ConfigUtils.any2map(c)
        val fastaUrl = genomeConfig.getOrElse("fasta_url",
          throw new IllegalArgumentException(s"No fasta_url found for $speciesName - $genomeName")).toString

          val genomeDir = new File(speciesDir, genomeName)
          val fastaFile = new File(genomeDir, "reference.fa")

          val curl = new Curl(this)
          curl.url = fastaUrl
          if (fastaUrl.endsWith(".gz")) {
            curl.output = new File(genomeDir, "reference.fa.gz")
            curl.isIntermediate = true
            add(Zcat(this, curl.output, fastaFile))
          } else curl.output = fastaFile
          add(curl)

          val faidx = SamtoolsFaidx(this, fastaFile)
          add(faidx)

          //TODO: dict

        //TODO: other indexes
      }
    }
  }
}

object GenerateIndexes extends PipelineCommand
