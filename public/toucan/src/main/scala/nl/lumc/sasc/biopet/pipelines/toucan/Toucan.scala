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

import java.io.File

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsView
import nl.lumc.sasc.biopet.extensions.bedtools.{BedtoolsIntersect, BedtoolsMerge}
import nl.lumc.sasc.biopet.extensions.gatk.{CatVariants, SelectVariants}
import nl.lumc.sasc.biopet.extensions.manwe.{ManweAnnotateVcf, ManweSamplesImport}
import nl.lumc.sasc.biopet.extensions.tools.{GvcfToBed, VcfWithVcf, VepNormalizer}
import nl.lumc.sasc.biopet.extensions.{Bgzip, Ln, VariantEffectPredictor}
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.queue.QScript

/**
 * Pipeline to annotate a vcf file with VEP
 *
 * Created by ahbbollen on 15-1-15.
 */
class Toucan(val root: Configurable) extends QScript with BiopetQScript with SummaryQScript with Reference {
  def this() = this(null)

  @Input(doc = "Input VCF file", shortName = "Input", required = true)
  var inputVCF: File = _

  @Input(doc = "Input GVCF file", shortName = "gvcf", required = false)
  var inputGvcf: Option[File] = None

  lazy val gonlVcfFile: Option[File] = config("gonl_vcf")
  lazy val exacVcfFile: Option[File] = config("exac_vcf")

  var sampleIds: List[String] = Nil
  def init(): Unit = {
    inputFiles :+= new InputFile(inputVCF)
    sampleIds = root match {
      case m: MultiSampleQScript => m.samples.keys.toList
      case null                  => VcfUtils.getSampleIds(inputVCF)
      case s: SampleLibraryTag   => s.sampleId.toList
      case _                     => throw new IllegalArgumentException("You don't have any samples")
    }
  }

  override def defaults = Map(
    "varianteffectpredictor" -> Map("everything" -> true, "failed" -> 1, "allow_non_variant" -> true)
  )

  def biopetScript(): Unit = {
    val doVarda: Boolean = config("use_varda", default = false)
    val useVcf: File = if (doVarda) {
      inputGvcf match {
        case Some(s) => varda(inputVCF, s)
        case _       => throw new IllegalArgumentException("You have not specified a GVCF file")
      }
    } else inputVCF

    val outputVcfFiles = BedRecordList.fromReference(referenceFasta())
      .scatter(config("bin_size", default = 50000000))
      .allRecords.map { region =>

      val chunkName = s"${region.chr}-${region.start}-${region.end}"
      val chunkDir = new File(outputDir, "chunk" + File.separator + chunkName)
      val sv = new SelectVariants(this)
      sv.inputFiles :+= useVcf
      sv.outputFile = new File(chunkDir, chunkName + ".vcf.gz")
      sv.isIntermediate = true
      add(sv)

      val vep = new VariantEffectPredictor(this)
      vep.input = sv.outputFile
      vep.output = new File(chunkDir, chunkName + ".vep.vcf")
      vep.isIntermediate = true
      add(vep)
      addSummarizable(vep, "variant_effect_predictor")

      val normalizer = new VepNormalizer(this)
      normalizer.inputVCF = vep.output
      normalizer.outputVcf = new File(chunkDir, chunkName + ".normalized.vcf.gz")
      add(normalizer)

      var outputFile = normalizer.outputVcf

      gonlVcfFile match {
        case Some(gonlFile) =>
          val vcfWithVcf = new VcfWithVcf(this)
          vcfWithVcf.input = outputFile
          vcfWithVcf.secondaryVcf = gonlFile
          vcfWithVcf.output = swapExt(chunkDir, normalizer.outputVcf, ".vcf.gz", ".gonl.vcf.gz")
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
          vcfWithVcf.output = swapExt(chunkDir, outputFile, ".vcf.gz", ".exac.vcf.gz")
          vcfWithVcf.fields ::= ("AF", "AF_exac", None)
          add(vcfWithVcf)
          outputFile = vcfWithVcf.output
        case _ =>
      }

      outputFile
    }

    val cv = new CatVariants(this)
    cv.inputFiles = outputVcfFiles.toList
    cv.outputFile = (gonlVcfFile, exacVcfFile) match {
      case (Some(_), Some(_)) => swapExt(outputDir, inputVCF, ".vcf.gz", ".vep.normalized.gonl.exac.vcf.gz")
      case (Some(_), _) => swapExt(outputDir, inputVCF, ".vcf.gz", ".vep.normalized.gonl.vcf.gz")
      case (_, Some(_)) => swapExt(outputDir, inputVCF, ".vcf.gz", ".vep.normalized.exac.vcf.gz")
      case _ => swapExt(outputDir, inputVCF, ".vcf.gz", ".vep.normalized.vcf.gz")
    }
    add(cv)

    addSummaryJobs()
  }

  /**
   * Performs the varda import and activate for one sample
   * @param sampleID the sampleID to be used
   * @param inputVcf the input VCF
   * @param gVCF the gVCF for coverage
   * @param annotation: ManweDownloadAnnotateVcf object of annotated vcf
   * @return
   */
  def importAndActivateSample(sampleID: String, inputVcf: File,
                              gVCF: File, annotation: ManweAnnotateVcf): ManweActivateAfterAnnotImport = {

    val minGQ: Int = config("minimum_genome_quality", default = 20, namespace = "manwe")
    val isPublic: Boolean = config("varda_is_public", default = true, namespace = "manwe")

    val bedTrack = new GvcfToBed(this)
    bedTrack.inputVcf = gVCF
    bedTrack.outputBed = swapExt(outputDir, gVCF, ".vcf.gz", s""".$sampleID.bed""")
    bedTrack.minQuality = minGQ
    bedTrack.isIntermediate = true
    bedTrack.sample = Some(sampleID)
    add(bedTrack)

    val mergedBed = new BedtoolsMerge(this)
    mergedBed.input = bedTrack.outputBed
    mergedBed.dist = 5
    mergedBed.output = swapExt(outputDir, bedTrack.outputBed, ".bed", ".merged.bed")
    add(mergedBed)

    val bgzippedBed = new Bgzip(this)
    bgzippedBed.input = List(mergedBed.output)
    bgzippedBed.output = swapExt(outputDir, mergedBed.output, ".bed", ".bed.gz")
    add(bgzippedBed)

    val singleVcf = new BcftoolsView(this)
    singleVcf.input = inputVCF
    singleVcf.output = swapExt(outputDir, inputVCF, ".vcf.gz", s""".$sampleID.vcf.gz""")
    singleVcf.samples = List(sampleID)
    singleVcf.minAC = Some(1)
    singleVcf.isIntermediate = true
    add(singleVcf)

    val intersected = new BedtoolsIntersect(this)
    intersected.input = singleVcf.output
    intersected.intersectFile = bgzippedBed.output
    intersected.output = swapExt(outputDir, singleVcf.output, ".vcf.gz", ".intersected.vcf")
    add(intersected)

    val bgzippedIntersect = new Bgzip(this)
    bgzippedIntersect.input = List(intersected.output)
    bgzippedIntersect.output = swapExt(outputDir, intersected.output, ".vcf", ".vcf.gz")
    add(bgzippedIntersect)

    val imported = new ManweSamplesImport(this)
    imported.vcfs = List(bgzippedIntersect.output)
    imported.beds = List(bgzippedBed.output)
    imported.name = Some(sampleID)
    imported.public = isPublic
    imported.waitToComplete = false
    imported.isIntermediate = true
    imported.output = swapExt(outputDir, intersected.output, ".vcf.gz", ".manwe.import")
    add(imported)

    val active = new ManweActivateAfterAnnotImport(this, annotation, imported)
    active.output = swapExt(outputDir, imported.output, ".import", ".activated")
    add(active)
    active

  }

  /**
   * Perform varda analysis
   * @param vcf input vcf
   * @param gVcf The gVCF to be used for coverage calculations
   * @return return vcf
   */
  def varda(vcf: File, gVcf: File): File = {

    val annotationQueries: List[String] = config("annotation_queries", default = List("GLOBAL *"), namespace = "manwe")
    //TODO: add groups!!! Need sample-specific group tags for this

    val annotate = new ManweAnnotateVcf(this)
    annotate.vcf = vcf
    if (annotationQueries.nonEmpty) {
      annotate.queries = annotationQueries
    }
    annotate.waitToComplete = true
    annotate.output = swapExt(outputDir, vcf, ".vcf.gz", ".manwe.annot")
    annotate.isIntermediate = true
    add(annotate)

    val annotatedVcf = new ManweDownloadAfterAnnotate(this, annotate)
    annotatedVcf.output = swapExt(outputDir, annotate.output, ".manwe.annot", "manwe.annot.vcf.gz")
    add(annotatedVcf)

    val activates = sampleIds map { x => importAndActivateSample(x, vcf, gVcf, annotate) }

    val finalLn = new Ln(this)
    activates.foreach(x => finalLn.deps :+= x.output)
    finalLn.input = annotatedVcf.output
    finalLn.output = swapExt(outputDir, annotatedVcf.output, "manwe.annot.vcf.gz", ".varda_annotated.vcf.gz")
    finalLn.relative = true
    add(finalLn)

    finalLn.output
  }

  def summaryFile = new File(outputDir, "Toucan.summary.json")

  def summaryFiles = Map()

  def summarySettings = Map()
}

object Toucan extends PipelineCommand