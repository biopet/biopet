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
package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference, SampleLibraryTag }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Tabix
import nl.lumc.sasc.biopet.extensions.gatk.{ CombineVariants, GenotypeConcordance }
import nl.lumc.sasc.biopet.extensions.tools.VcfStats
import nl.lumc.sasc.biopet.extensions.vt.{ VtDecompose, VtNormalize }
import nl.lumc.sasc.biopet.pipelines.bammetrics.TargetRegions
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.{ VarscanCnsSingleSample, _ }
import nl.lumc.sasc.biopet.utils.{ BamUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

/**
 * Implementation of ShivaVariantcalling
 *
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaVariantcalling(val root: Configurable) extends QScript
  with SummaryQScript
  with SampleLibraryTag
  with Reference
  with TargetRegions {
  qscript =>

  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  /** Executed before script */
  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
  }

  var referenceVcf: Option[File] = config("reference_vcf")

  var referenceVcfRegions: Option[File] = config("reference_vcf_regions")

  /** Name prefix, can override this methods if neeeded */
  def namePrefix: String = {
    (sampleId, libId) match {
      case (Some(s), Some(l)) => s + "-" + l
      case (Some(s), _)       => s
      case _                  => config("name_prefix")
    }
  }

  override def defaults = Map("bcftoolscall" -> Map("f" -> List("GQ")))

  /** Final merged output files of all variantcaller modes */
  def finalFile = new File(outputDir, namePrefix + ".final.vcf.gz")

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("variantcallers")

  val callers: List[Variantcaller] = {
    (for (name <- configCallers) yield {
      if (!callersList.exists(_.name == name))
        Logging.addError(s"variantcaller '$name' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
      callersList.find(_.name == name)
    }).flatten.toList.sortBy(_.prio)
  }

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty, "must select at least 1 variantcaller, choices are: " + callersList.map(_.name).mkString(", "))

    addAll(dbsnpVcfFile.map(Shiva.makeValidateVcfJobs(this, _, referenceFasta())).getOrElse(Nil))

    val cv = new CombineVariants(qscript)
    cv.out = finalFile
    cv.setKey = Some("VariantCaller")
    cv.genotypemergeoption = Some("PRIORITIZE")
    cv.rod_priority_list = Some(callers.map(_.name).mkString(","))
    for (caller <- callers) {
      caller.inputBams = inputBams
      caller.namePrefix = namePrefix
      caller.outputDir = new File(outputDir, caller.name)
      add(caller)
      addStats(caller.outputFile, caller.name)
      val normalize: Boolean = config("execute_vt_normalize", default = false, namespace = caller.configNamespace)
      val decompose: Boolean = config("execute_vt_decompose", default = false, namespace = caller.configNamespace)

      val vtNormalize = new VtNormalize(this)
      vtNormalize.inputVcf = caller.outputFile
      val vtDecompose = new VtDecompose(this)

      if (normalize && decompose) {
        vtNormalize.outputVcf = swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".normalized.vcf.gz")
        vtNormalize.isIntermediate = true
        add(vtNormalize, Tabix(this, vtNormalize.outputVcf))
        vtDecompose.inputVcf = vtNormalize.outputVcf
        vtDecompose.outputVcf = swapExt(caller.outputDir, vtNormalize.outputVcf, ".vcf.gz", ".decompose.vcf.gz")
        add(vtDecompose, Tabix(this, vtDecompose.outputVcf))
        cv.variant :+= TaggedFile(vtDecompose.outputVcf, caller.name)
      } else if (normalize && !decompose) {
        vtNormalize.outputVcf = swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".normalized.vcf.gz")
        add(vtNormalize, Tabix(this, vtNormalize.outputVcf))
        cv.variant :+= TaggedFile(vtNormalize.outputVcf, caller.name)
      } else if (!normalize && decompose) {
        vtDecompose.inputVcf = caller.outputFile
        vtDecompose.outputVcf = swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".decompose.vcf.gz")
        add(vtDecompose, Tabix(this, vtDecompose.outputVcf))
        cv.variant :+= TaggedFile(vtDecompose.outputVcf, caller.name)
      } else cv.variant :+= TaggedFile(caller.outputFile, caller.name)
    }
    add(cv)

    addStats(finalFile, "final")

    addSummaryJobs()
  }

  protected def addStats(vcfFile: File, name: String): Unit = {
    val vcfStats = new VcfStats(qscript)
    vcfStats.input = vcfFile
    vcfStats.setOutputDir(new File(vcfFile.getParentFile, "vcfstats"))
    if (name == "final") vcfStats.infoTags :+= "VariantCaller"
    add(vcfStats)
    addSummarizable(vcfStats, s"$namePrefix-vcfstats-$name")

    referenceVcf.foreach(referenceVcfFile => {
      val gc = new GenotypeConcordance(this)
      gc.eval = vcfFile
      gc.comp = referenceVcfFile
      gc.out = new File(vcfFile.getParentFile, s"$namePrefix-genotype_concordance.$name.txt")
      referenceVcfRegions.foreach(gc.intervals ::= _)
      add(gc)
      addSummarizable(gc, s"$namePrefix-genotype_concordance-$name")
    })

    for (bedFile <- ampliconBedFile.toList ::: roiBedFiles) {
      val regionName = bedFile.getName.stripSuffix(".bed")
      val vcfStats = new VcfStats(qscript)
      vcfStats.input = vcfFile
      vcfStats.intervals = Some(bedFile)
      vcfStats.setOutputDir(new File(vcfFile.getParentFile, s"vcfstats-$regionName"))
      if (name == "final") vcfStats.infoTags :+= "VariantCaller"
      add(vcfStats)
      addSummarizable(vcfStats, s"$namePrefix-vcfstats-$name-$regionName")
    }
  }

  /** Will generate all available variantcallers */
  protected def callersList: List[Variantcaller] =
    new HaplotypeCallerGvcf(this) ::
      new HaplotypeCallerAllele(this) ::
      new UnifiedGenotyperAllele(this) ::
      new UnifiedGenotyper(this) ::
      new HaplotypeCaller(this) ::
      new Freebayes(this) ::
      new RawVcf(this) ::
      new Bcftools(this) ::
      new BcftoolsSingleSample(this) ::
      new VarscanCnsSingleSample(this) :: Nil

  /** Location of summary file */
  def summaryFile = new File(outputDir, "ShivaVariantcalling.summary.json")

  /** Settings for the summary */
  def summarySettings = Map(
    "variantcallers" -> configCallers.toList,
    "regions_of_interest" -> roiBedFiles.map(_.getName),
    "amplicon_bed" -> ampliconBedFile.map(_.getAbsolutePath)
  )

  /** Files for the summary */
  def summaryFiles: Map[String, File] = {
    callers.map(x => x.name -> x.outputFile).toMap + ("final" -> finalFile)
  }
}

object ShivaVariantcalling extends PipelineCommand