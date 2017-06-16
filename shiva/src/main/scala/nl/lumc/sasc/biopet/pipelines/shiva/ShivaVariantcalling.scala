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

import nl.lumc.sasc.biopet.core.{MultiSampleQScript, PipelineCommand, Reference, SampleLibraryTag}
import MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Tabix
import nl.lumc.sasc.biopet.extensions.gatk.{CombineVariants, GenotypeConcordance}
import nl.lumc.sasc.biopet.extensions.tools.VcfStats
import nl.lumc.sasc.biopet.extensions.vt.{VtDecompose, VtNormalize}
import nl.lumc.sasc.biopet.pipelines.bammetrics.TargetRegions
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.{VarscanCnsSingleSample, _}
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic.{MuTect2, SomaticVariantcaller, TumorNormalPair}
import nl.lumc.sasc.biopet.utils.{BamUtils, ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.summary.db.Schema.Sample
import nl.lumc.sasc.biopet.utils.summary.db.{SummaryDb, SummaryDbWrite}
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Implementation of ShivaVariantcalling
  *
  * Created by pjvan_thof on 2/26/15.
  */
class ShivaVariantcalling(val parent: Configurable)
    extends QScript
    with SummaryQScript
    with SampleLibraryTag
    with Reference
    with TargetRegions { qscript =>

  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  var inputBqsrFiles: Map[String, File] = Map()

  var genders: Map[String, Gender.Value] = _

  var tnPairs: List[TumorNormalPair] = _

  /** Executed before script */
  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
    if (Option(genders).isEmpty) genders = {
      val samples: Map[String, Any] = config("genders", default = Map())
      samples.map {
        case (sampleName, gender) =>
          sampleName -> (gender.toString.toLowerCase match {
            case "male" => Gender.Male
            case "female" => Gender.Female
            case _ => Gender.Unknown
          })
      }
    }
    if (isSomaticVariantCallingConfigured()) {
      loadTnPairsFromConfig()
      validateTnPairs()
      val db = SummaryDb.openSqliteSummary(summaryDbFile)
      val samples: Seq[Sample] = Await.result(db.getSamples(runId = summaryRunId), Duration.Inf)
      for (pair <- tnPairs) {
        var tags: Map[String, String] = Map("tumor" -> pair.tumorSample, "normal" -> pair.normalSample)
        addPairInfoToDb(db, summaryRunId, samples, pair.tumorSample, tags)
        addPairInfoToDb(db, summaryRunId, samples, pair.normalSample, tags)
      }
    }
  }

  var referenceVcf: Option[File] = config("reference_vcf")

  var referenceVcfRegions: Option[File] = config("reference_vcf_regions")

  /** Name prefix, can override this methods if neeeded */
  def namePrefix: String = {
    (sampleId, libId) match {
      case (Some(s), Some(l)) => s + "-" + l
      case (Some(s), _) => s
      case _ => config("name_prefix", default = "multisample")
    }
  }

  override def defaults = Map("bcftoolscall" -> Map("f" -> List("GQ")))

  def isSomaticVariantCallingConfigured(): Boolean = {
    callers.exists(_.isInstanceOf[SomaticVariantcaller])
  }

  /** Final merged output files of all variantcaller modes */
  def finalFile: Option[File] =
    if (callers.exists(_.mergeVcfResults)) Some(new File(outputDir, namePrefix + ".final.vcf.gz"))
    else None

  // TODO if there will be in the future more than one method for somatic variant calling then the outputs from those should be merged
  def finalFileSomaticCallers: Option[File] =
    callers.collectFirst({ case caller if caller.isInstanceOf[MuTect2] => caller.outputFile })

  /** Variantcallers requested by the config */
  protected val configCallers: Set[String] = config("variantcallers")

  val callers: List[Variantcaller] = {
    (for (name <- configCallers) yield {
      if (!ShivaVariantcalling.callersList(this).exists(_.name == name))
        Logging.addError(
          s"variantcaller '$name' does not exist, possible to use: " + ShivaVariantcalling
            .callersList(this)
            .map(_.name)
            .mkString(", "))
      ShivaVariantcalling.callersList(this).find(_.name == name)
    }).flatten.toList.sortBy(_.prio)
  }

  /** This will add jobs for this pipeline */
  def biopetScript(): Unit = {
    require(inputBams.nonEmpty, "No input bams found")
    require(callers.nonEmpty,
            "must select at least 1 variantcaller, choices are: " + ShivaVariantcalling
              .callersList(this)
              .map(_.name)
              .mkString(", "))
    require(
      finalFile.isDefined || finalFileSomaticCallers.isDefined,
      "Error in configuration, when not using somatic variant caller(s) then for at least one caller the parameter 'merge_vcf_results' must be set to true."
    )

    addAll(
      dbsnpVcfFile
        .map(
          Shiva.makeValidateVcfJobs(this, _, referenceFasta(), new File(outputDir, ".validate")))
        .getOrElse(Nil))

    val cv = new CombineVariants(qscript)
    cv.setKey = Some("VariantCaller")
    cv.genotypemergeoption = Some("PRIORITIZE")
    cv.rod_priority_list = Some(callers.filter(_.mergeVcfResults).map(_.name).mkString(","))
    for (caller <- callers) {
      caller.inputBams = inputBams
      caller.inputBqsrFiles = inputBqsrFiles
      caller.namePrefix = namePrefix
      caller.outputDir = new File(outputDir, caller.name)
      caller.genders = genders
      if (caller.isInstanceOf[SomaticVariantcaller])
        caller.asInstanceOf[SomaticVariantcaller].tnPairs = this.tnPairs

      add(caller)
      addStats(caller.outputFile, caller.name)
      val normalize: Boolean =
        config("execute_vt_normalize", default = false, namespace = caller.configNamespace)
      val decompose: Boolean =
        config("execute_vt_decompose", default = false, namespace = caller.configNamespace)

      val vtNormalize = new VtNormalize(this)
      vtNormalize.inputVcf = caller.outputFile
      val vtDecompose = new VtDecompose(this)

      if (normalize && decompose) {
        vtNormalize.outputVcf =
          swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".normalized.vcf.gz")
        vtNormalize.isIntermediate = true
        add(vtNormalize, Tabix(this, vtNormalize.outputVcf))
        vtDecompose.inputVcf = vtNormalize.outputVcf
        vtDecompose.outputVcf =
          swapExt(caller.outputDir, vtNormalize.outputVcf, ".vcf.gz", ".decompose.vcf.gz")
        add(vtDecompose, Tabix(this, vtDecompose.outputVcf))
        if (caller.mergeVcfResults) cv.variant :+= TaggedFile(vtDecompose.outputVcf, caller.name)
      } else if (normalize && !decompose) {
        vtNormalize.outputVcf =
          swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".normalized.vcf.gz")
        add(vtNormalize, Tabix(this, vtNormalize.outputVcf))
        if (caller.mergeVcfResults) cv.variant :+= TaggedFile(vtNormalize.outputVcf, caller.name)
      } else if (!normalize && decompose) {
        vtDecompose.inputVcf = caller.outputFile
        vtDecompose.outputVcf =
          swapExt(caller.outputDir, caller.outputFile, ".vcf.gz", ".decompose.vcf.gz")
        add(vtDecompose, Tabix(this, vtDecompose.outputVcf))
        if (caller.mergeVcfResults) cv.variant :+= TaggedFile(vtDecompose.outputVcf, caller.name)
      } else if (caller.mergeVcfResults) cv.variant :+= TaggedFile(caller.outputFile, caller.name)
    }
    if (cv.variant.nonEmpty) {
      cv.out = finalFile.get
      add(cv)
      addStats(finalFile.get, "final")
    }

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

  private def addPairInfoToDb(db: SummaryDbWrite, runId: Int, existingSamples: Seq[Sample], sampleName: String, pairInfo: Map[String, String]): Unit = {
    var tags : Map[String, Any] = existingSamples.find(_.name == sampleName) match {
      case s: Sample if s.tags.nonEmpty => pairInfo ++ ConfigUtils.jsonToMap(ConfigUtils.textToJson(s.tags.get))
      case _ => pairInfo
    }
    db.createOrUpdateSample(sampleName, runId, Some(ConfigUtils.mapToJson(tags).nospaces))
  }

  private def loadTnPairsFromConfig(): Unit = {
    var samplePairs: List[Any] = config("tumor_normal_pairs").asList
    if (samplePairs != null) {
      try {
        for (elem <- samplePairs) {
          val pair: Map[String, Any] = ConfigUtils.any2map(elem).map({
            case (key, sampleName) => key.toUpperCase() -> sampleName
          })
          tnPairs :+= TumorNormalPair(pair("T").toString, pair("N").toString)
        }
      } catch {
        case e: Exception => Logging.addError("Unable to parse the parameter 'tumor_normal_pairs' from configuration.", cause = e)
      }
    } else {
      Logging.addError("Parameter 'tumor_normal_pairs' is missing from configuration. When using MuTect2, samples configuration must give the pairs of matching tumor and normal samples.")
    }
  }

  private def validateTnPairs(): Unit = {
    var samplesWithBams = inputBams.keySet
    var tnSamples: List[String] = List()
    tnPairs.foreach(pair => tnSamples ++= List(pair.tumorSample, pair.normalSample))
    tnSamples.foreach(sample => {
      if (!samplesWithBams.contains(sample))
        Logging.addError(
          s"Parameter 'tumor_normal_pairs' contains a sample for which no input files can be found, sample name: $sample")
    })
    if (tnSamples.size != tnSamples.distinct.size)
      Logging.addError(
        "Each sample should appear once in the sample pairs given with the parameter 'tumor_normal_pairs'")
    if (tnSamples.size != samplesWithBams.size)
      Logging.addError(
        "The number of samples given with the parameter 'tumor_normal_pairs' has to match the number of samples for which there are input files defined.")
  }

  /** Settings for the summary */
  def summarySettings = Map(
    "variantcallers" -> configCallers.toList,
    "regions_of_interest" -> roiBedFiles.map(_.getName),
    "amplicon_bed" -> ampliconBedFile.map(_.getAbsolutePath),
    "somatic_variant_calling" -> isSomaticVariantCallingConfigured
  )

  /** Files for the summary */
  def summaryFiles: Map[String, File] = {
    var files = callers.map(x => x.name -> x.outputFile).toMap
    if (finalFile.isDefined) files += ("final" -> finalFile.get)
    files
  }
}

object ShivaVariantcalling extends PipelineCommand {

  /** Will generate all available variantcallers */
  protected[pipelines] def callersList(root: Configurable): List[Variantcaller] =
    new HaplotypeCallerGvcf(root) ::
      new HaplotypeCallerAllele(root) ::
      new UnifiedGenotyperAllele(root) ::
      new UnifiedGenotyper(root) ::
      new HaplotypeCaller(root) ::
      new Freebayes(root) ::
      new RawVcf(root) ::
      new Bcftools(root) ::
      new BcftoolsSingleSample(root) ::
      new VarscanCnsSingleSample(root) ::
      new MuTect2(root) :: Nil
}
