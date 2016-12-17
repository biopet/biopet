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
package nl.lumc.sasc.biopet.core

import java.io.File

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import nl.lumc.sasc.biopet.core.summary.{Summarizable, SummaryQScript}
import nl.lumc.sasc.biopet.utils._
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}

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

  def referenceDict = FastaUtils.getCachedDict(referenceFasta())

  /** All config values will get a prefix */
  override def subPath = {
    referenceConfigPath ::: super.subPath
  }

  lazy val geneAnnotationVersion: Option[String] = config("gene_annotation_name")
  lazy val geneAnnotationSubPath = geneAnnotationVersion.map(x => List("gene_annotations", x)).getOrElse(Nil)
  lazy val dbsnpVersion: Option[Int] = config("dbsnp_version")
  lazy val dbsnpSubPath: List[String] = dbsnpVersion.map(x => List("dbsnp_annotations", x.toString)).getOrElse(Nil)
  def dbsnpVcfFile: Option[File] = config("dbsnp_vcf", extraSubPath = dbsnpSubPath)

  /** Returns the reference config path */
  def referenceConfigPath = {
    List("references", referenceSpecies, referenceName)
  }

  /** When set override this on true the pipeline with raise an exception when fai index is not found */
  def faiRequired = false

  /** When set override this on true the pipeline with raise an exception when dict index is not found */
  def dictRequired = this.isInstanceOf[Summarizable] || this.isInstanceOf[SummaryQScript]

  /** Returns the dict file belonging to the fasta file */
  def referenceDictFile = new File(referenceFasta().getAbsolutePath
    .stripSuffix(".fa")
    .stripSuffix(".fasta")
    .stripSuffix(".fna") + ".dict")

  /** Returns the fai file belonging to the fasta file */
  def referenceFai = new File(referenceFasta().getAbsolutePath + ".fai")

  /** Returns the fasta file */
  def referenceFasta(): File = {
    val file: File = config("reference_fasta")
    if (config.contains("reference_fasta")) checkFasta(file)
    else {
      val defaults = ConfigUtils.mergeMaps(this.defaults, this.internalDefaults)

      def getReferences(map: Map[String, Any]): Set[(String, String)] = (for (
        (species, species_content) <- map.getOrElse("references", Map[String, Any]()).asInstanceOf[Map[String, Any]].toList;
        (reference_name, _) <- species_content.asInstanceOf[Map[String, Any]].toList
      ) yield (species, reference_name)).toSet

      val references = getReferences(defaults) ++ getReferences(Config.global.map)
      if (!references.contains((referenceSpecies, referenceName))) {
        val buffer = new StringBuilder()
        if (references.exists(_._1 == referenceSpecies)) {
          buffer.append(s"Reference: '$referenceName' does not exist in config for species: '$referenceSpecies'")
          buffer.append(s"\nRefrences found for species '$referenceSpecies':")
          references.filter(_._1 == referenceSpecies).foreach(x => buffer.append("\n - " + x._2))
        } else {
          buffer.append(s"Species: '$referenceSpecies' does not exist in config")
          if (references.nonEmpty) buffer.append("\n    References available in config (species -> reference_name):")
          else buffer.append("\n    No references found in user or global config")
          references.toList.sorted.foreach(x => buffer.append(s"\n     - ${x._1} -> ${x._2}"))
        }
        Logging.addError(buffer.toString)
      }
    }
    file
  }

  /** Create summary part for reference */
  def referenceSummary: Map[String, Any] = {
    Reference.requireDict(referenceFasta())
    Map("contigs" ->
      (for (seq <- referenceDict.getSequences) yield seq.getSequenceName -> {
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
      if (!file.exists()) Logging.addError(s"Reference not found: $file, species: $referenceSpecies, name: $referenceName, configValue: " + config("reference_fasta"))
      Reference.checked += file
    }

    if (dictRequired) Reference.requireDict(file)
    if (faiRequired) Reference.requireFai(file)
  }
}

object Reference {

  /** Used as cache to avoid double checking */
  private var checked: Set[File] = Set()

  /**
   * Raise an exception when given fasta file has no fai file
   *
   * @param fastaFile Fasta file
   */
  def requireFai(fastaFile: File): Unit = {
    val fai = new File(fastaFile.getAbsolutePath + ".fai")
    if (!checked.contains(fai)) {
      checked += fai
      if (fai.exists()) {
        if (!IndexedFastaSequenceFile.canCreateIndexedFastaReader(fastaFile))
          Logging.addError(s"Index of reference cannot be loaded, reference: $fastaFile")
      } else Logging.addError("Reference is missing a fai file")
    }
  }

  /**
   * Raise an exception when given fasta file has no dict file
   *
   * @param fastaFile Fasta file
   */
  def requireDict(fastaFile: File): Unit = {
    val dict = new File(fastaFile.getAbsolutePath
      .stripSuffix(".fna")
      .stripSuffix(".fa")
      .stripSuffix(".fasta") + ".dict")
    if (!checked.contains(dict)) {
      checked += dict
      if (!dict.exists()) Logging.addError("Reference is missing a dict file")
    }
  }

  def askReference: Map[String, Any] = {
    val globalSpecies: Map[String, Any] = Config.global.defaults.getOrElse("references", Map()).asInstanceOf
    val species = Question.askValue("species",
      description = Some(if (globalSpecies.nonEmpty)
        s"""Species found in general config:
           |- ${globalSpecies.keys.mkString("\n- ")}
           |It's possible to select something else but be aware of installing all fasta/indexes required by a pipeline
           |""".stripMargin else ""))

    val globalReferences: Map[String, Any] = globalSpecies.getOrElse(species, Map()).asInstanceOf
    val referenceName = Question.askValue("reference_name",
      description = Some(if (globalReferences.nonEmpty)
        s"""Reference for $species found in general config:
            |- ${globalReferences.keys.mkString("\n- ")}
            |It's possible to select something else but be aware of installing all indexes required by a pipeline
            |""".stripMargin else ""))

    val reference: Map[String, Any] = globalReferences.getOrElse(referenceName, Map()).asInstanceOf
    val referenceFasta: Option[String] = if (reference.contains("reference_fasta")) None else {
      Some(Question.askValue("Reference Fasta", validation = List(TemplateTool.isAbsolutePath, TemplateTool.mustExist),
        description = Some(s"No fasta file found for $species -> $referenceName")))
    }

    Map("species" -> species, "reference_name" -> referenceName) ++ referenceFasta.map("reference_fasta" -> _)
  }
}
