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
package nl.lumc.sasc.biopet.core

import java.io.File

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import nl.lumc.sasc.biopet.core.config.Configurable

import scala.collection.JavaConversions._

/**
 * This trait is used for pipelines and extension that use a reference based on one fasta file.
 * The fasta file can contain multiple contigs.
 *
 * Created by pjvan_thof on 4/6/15.
 */
trait Reference extends Configurable {

  /** Returns species, default to unknown_species */
  def referenceSpecies: String = {
    root match {
      case r: Reference if r.referenceSpecies != "unknown_species" => r.referenceSpecies
      case _ => config("species", default = "unknown_species", path = super.configPath)
    }
  }

  /** Return referencename, default to unknown_ref */
  def referenceName: String = {
    root match {
      case r: Reference if r.referenceName != "unknown_ref" => r.referenceName
      case _ =>
        val default: String = config("default", default = "unknown_ref", path = List("references", referenceSpecies))
        config("reference_name", default = default, path = super.configPath)
    }
  }

  /** All config values will get a prefix */
  override def subPath = {
    referenceConfigPath ::: super.subPath
  }

  /** Returns the reference config path */
  def referenceConfigPath = {
    List("references", referenceSpecies, referenceName)
  }

  /** When set override this on true the pipeline with raise an exception when fai index is not found */
  protected def faiRequired = false

  /** When set override this on true the pipeline with raise an exception when dict index is not found */
  protected def dictRequired = false

  /** Returns the fasta file */
  def referenceFasta(): File = {
    val file: File = config("reference_fasta")
    checkFasta(file)

    val dict = new File(file.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta") + ".dict")
    val fai = new File(file.getAbsolutePath + ".fai")

    this match {
      case c: BiopetCommandLineFunctionTrait => c.deps :::= dict :: fai :: Nil
      case _                                 =>
    }

    file
  }

  /** Create summary part for reference */
  def referenceSummary: Map[String, Any] = {
    Reference.requireDict(referenceFasta())
    val file = new IndexedFastaSequenceFile(referenceFasta())
    Map("contigs" ->
      (for (seq <- file.getSequenceDictionary.getSequences) yield seq.getSequenceName -> {
        val md5 = Option(seq.getAttribute("M5"))
        Map("md5" -> md5, "length" -> seq.getSequenceLength)
      }).toMap,
      "species" -> referenceSpecies,
      "name" -> referenceName
    )
  }

  //TODO: this become obsolete when index get auto generated

  /** Check fasta file if file exist and index file are there */
  def checkFasta(file: File): Unit = {
    if (!Reference.checked.contains(file)) {
      if (!file.exists()) BiopetQScript.addError(s"Reference not found: $file, species: $referenceSpecies, name: $referenceName, configValue: " + config("reference_fasta"))

      if (dictRequired) Reference.requireDict(file)
      if (faiRequired) Reference.requireFai(file)

      Reference.checked += file
    }
  }
}

object Reference {

  /** Used as cache to avoid double checking */
  private var checked: Set[File] = Set()

  /**
   * Raise an exception when given fasta file has no fai file
   * @param fastaFile Fasta file
   * @throws IllegalArgumentException
   */
  def requireFai(fastaFile: File): Unit = {
    val fai = new File(fastaFile.getAbsolutePath + ".fai")
    require(fai.exists(), "Reference is missing a fai file")
    require(IndexedFastaSequenceFile.canCreateIndexedFastaReader(fastaFile),
      "Index of reference cannot be loaded, reference: " + fastaFile)
  }

  /**
   * Raise an exception when given fasta file has no dict file
   * @param fastaFile Fasta file
   * @throws IllegalArgumentException
   */
  def requireDict(fastaFile: File): Unit = {
    val dict = new File(fastaFile.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta") + ".dict")
    require(dict.exists(), "Reference is missing a dict file")
  }
}
