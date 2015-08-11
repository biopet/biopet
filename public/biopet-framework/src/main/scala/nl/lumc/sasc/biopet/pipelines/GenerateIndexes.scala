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

import nl.lumc.sasc.biopet.core.{ PipelineCommand, BiopetQScript }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.bwa.BwaIndex
import nl.lumc.sasc.biopet.extensions.picard.CreateSequenceDictionary
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFaidx
import nl.lumc.sasc.biopet.extensions.{ Ln, Md5sum, Zcat, Curl }
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
        val fastaUri = genomeConfig.getOrElse("fasta_uri",
          throw new IllegalArgumentException(s"No fasta_uri found for $speciesName - $genomeName")).toString

        val genomeDir = new File(speciesDir, genomeName)
        val fastaFile = new File(genomeDir, "reference.fa")

        val curl = new Curl(this)
        curl.url = fastaUri
        if (fastaUri.endsWith(".gz")) {
          curl.output = new File(genomeDir, "reference.fa.gz")
          curl.isIntermediate = true
          add(Zcat(this, curl.output, fastaFile))
        } else curl.output = fastaFile
        add(curl)

        add(Md5sum(this, curl.output, genomeDir))

        val faidx = SamtoolsFaidx(this, fastaFile)
        add(faidx)

        val createDict = new CreateSequenceDictionary(this)
        createDict.reference = fastaFile
        createDict.output = new File(genomeDir, fastaFile.getName.stripSuffix(".fa") + ".dict")
        createDict.species = Some(speciesName)
        createDict.genomeAssembly = Some(genomeName)
        createDict.uri = Some(fastaUri)
        add(createDict)

        def createLinks(dir: File): File = {
          val newFastaFile = new File(dir, fastaFile.getName)
          val newFai = new File(dir, faidx.output.getName)
          val newDict = new File(dir, createDict.output.getName)

          add(Ln(this, faidx.output, newFai))
          add(Ln(this, createDict.output, newDict))
          val lnFasta = Ln(this, fastaFile, newFastaFile)
          lnFasta.deps ++= List(newFai, newDict)
          add(lnFasta)
          newFastaFile
        }

        // Bwa index
        val bwaIndex = new BwaIndex(this)
        bwaIndex.reference = createLinks(new File(genomeDir, "bwa"))
        add(bwaIndex)

        //TODO: Gsnap index

        //TODO: Star index

        //TODO: bowtie index
      }
    }
  }
}

object GenerateIndexes extends PipelineCommand
