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
package nl.lumc.sasc.biopet.pipelines.toucan

import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsView
import nl.lumc.sasc.biopet.extensions.manwe.{ManweSamplesImport, ManweAnnotateVcf, ManweDataSourcesAnnotate}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.extensions.{Ln, Gzip, VariantEffectPredictor}
import nl.lumc.sasc.biopet.extensions.tools.{GvcfToBed, VcfFilter, VcfWithVcf, VepNormalizer}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript

/**
 * Pipeline to annotate a vcf file with VEP
 *
 * Created by ahbbollen on 15-1-15.
 */
class Toucan(val root: Configurable) extends QScript with BiopetQScript with SummaryQScript with Reference{
  def this() = this(null)

  @Input(doc = "Input VCF file", shortName = "Input", required = true)
  var inputVCF: File = _

  var sampleIds: List[String] = Nil
  def init(): Unit = {
    inputFiles :+= new InputFile(inputVCF)
    sampleIds = root match {
      case m: MultiSampleQScript => m.samples.keys.toList
      case null => Nil //TODO: get names from vcf header
      case s: SampleLibraryTag => s.sampleId.toList
      case _ => throw new IllegalArgumentException("You don't have any samples")
    }
  }

  override def defaults = Map(
    "varianteffectpredictor" -> Map("everything" -> true)
  )
  //defaults ++= Map("varianteffectpredictor" -> Map("everything" -> true))

  def biopetScript(): Unit = {
    val vep = new VariantEffectPredictor(this)
    vep.input = inputVCF
    vep.output = new File(outputDir, inputVCF.getName.stripSuffix(".gz").stripSuffix(".vcf") + ".vep.vcf")
    vep.isIntermediate = true
    add(vep)

    val normalizer = new VepNormalizer(this)
    normalizer.inputVCF = vep.output
    normalizer.outputVcf = swapExt(outputDir, vep.output, ".vcf", ".normalized.vcf.gz")
    add(normalizer)

    // Optional annotation steps, depend is some files existing in the config
    val gonlVcfFile: Option[File] = config("gonl_vcf")
    val exacVcfFile: Option[File] = config("exac_vcf")

    var outputFile = normalizer.outputVcf

    gonlVcfFile match {
      case Some(gonlFile) =>
        val vcfWithVcf = new VcfWithVcf(this)
        vcfWithVcf.input = outputFile
        vcfWithVcf.secondaryVcf = gonlFile
        vcfWithVcf.output = swapExt(outputDir, normalizer.outputVcf, ".vcf.gz", ".gonl.vcf.gz")
        vcfWithVcf.fields ::= ("AF", "AF_gonl", None)
        add(vcfWithVcf)
        outputFile = vcfWithVcf.output
      case _ =>
    }

    exacVcfFile match {
      case Some(exacFile) =>
        val vcfWithVcf = new VcfWithVcf(this)
        vcfWithVcf.input = outputFile
        vcfWithVcf.secondaryVcf = exacFile
        vcfWithVcf.output = swapExt(outputDir, outputFile, ".vcf.gz", ".exac.vcf.gz")
        vcfWithVcf.fields ::= ("MAF", "MAF_exac", None)
        add(vcfWithVcf)
        outputFile = vcfWithVcf.output
      case _ =>
    }
  }

  /**
   * Perform varda analysis
   * @param vcf input vcf
   * @return return vcf
   */
  def varda(vcf: File): File = {
    val splits = sampleIds.map(x => {
      val view = new BcftoolsView(this)
      view.input = vcf
      view.output = swapExt(vcf, ".vcf.gz", s"$x.vcf.gz")
      view.samples = List(x)
      view.minAC = Some(1)
      add(view)
      view
    })

    val minGQ = config("minimumGenomeQuality", default = 20)
    val vardaConfig: Option[File] = config("vardaConfig") // TODO: create on the fly
    val annotationQueries: List[String] = config("annotationQueries")
    val isPublic: Boolean = config("vardaIsPublic")

    val filteredVcfs = splits.map(x => {
      val filter = new VcfFilter(this)
      filter.inputVcf = x.output
      filter.outputVcf = swapExt(x.output, ".vcf.gz", ".filtered.vcf.gz")
      filter.minGenomeQuality = minGQ
      add(filter)
      filter
    })

    val bedTracks = filteredVcfs.map(x => {
      val bed = new GvcfToBed(this)
      bed.inputVcf = x.outputVcf
      bed.outputBed = swapExt(x.outputVcf, ".vcf.gz", ".bed")
      add(bed)
      bed
    })

    val zippedBedTracks = bedTracks.map(x => {
      val gzip = new Gzip(this)
      gzip.input = List(x.outputBed)
      gzip.output = swapExt(x.outputBed, ".bed", ".bed.gz")
      add(gzip)
      gzip
    })

    val annotate = new ManweAnnotateVcf(this)
    annotate.vcf = vcf
    annotate.manweConfig = vardaConfig
    annotate.queries = annotationQueries
    annotate.waitToComplete = true
    annotate.output = swapExt(vcf, ".vcf.gz", ".tmp.annot")
    add(annotate)

    val annotatedVcf = new ManweDownloadAfterAnnotate(this, annotate)
    annotatedVcf.output = swapExt(annotate.output, ".tmp.annot", "tmp.annot.vcf.gz")

    val imports = for (
      (sample: String, bed, vcf) <- (sampleIds, bedTracks, filteredVcfs).zipped
    ) yield {
        val importing = new ManweSamplesImport(this)
        importing.beds = List(bed.outputBed)
        importing.vcfs = List(vcf.outputVcf)
        importing.name = Some(sample)
        importing.waitToComplete = true
        importing.output = swapExt(vcf.outputVcf, ".vcf.gz", ".tmp.import")
        importing.public = isPublic
        importing.manweConfig = vardaConfig
        importing
      }

    val activates = imports.map(x => {
      val active = new ManweActivateAfterAnnotImport(this, annotate, x)
      active.manweConfig = vardaConfig
      active.output = swapExt(x.output, ".tmp.import", ".tmp.activated")
      active
    })

    val finalLn = new Ln(this)
    activates.foreach(x => finalLn.deps :+= x)
    finalLn.input = annotatedVcf.output
    finalLn.output = swapExt(annotatedVcf.output, "tmp.annot.vcf.gz", ".varda_annotated.vcf.gz")
    finalLn.relative = true

    finalLn.output
  }

  def summaryFile = new File(outputDir, "Toucan.summary.json")

  def summaryFiles = Map()

  def summarySettings = Map()
}

object Toucan extends PipelineCommand