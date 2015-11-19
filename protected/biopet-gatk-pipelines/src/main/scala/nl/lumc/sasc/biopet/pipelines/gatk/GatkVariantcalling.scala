/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.gatk.broad._
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import nl.lumc.sasc.biopet.extensions.tools.{ MergeAlleles, MpileupToVcf, VcfFilter, VcfStats }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

import scala.collection.SortedMap
import scala.language.reflectiveCalls

class GatkVariantcalling(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  val scriptOutput = new GatkVariantcalling.ScriptOutput

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM")
  var inputBams: List[File] = Nil

  @Input(doc = "Raw vcf file", shortName = "raw")
  var rawVcfInput: File = _

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = config("reference")

  @Argument(doc = "OutputName", required = false)
  var outputName: String = _

  @Argument(doc = "Sample name", required = false)
  var sampleID: String = _

  var preProcesBams: Boolean = config("pre_proces_bams", default = true)
  var variantcalling: Boolean = true
  var doublePreProces: Boolean = config("double_pre_proces", default = true)
  var useHaplotypecaller: Boolean = config("use_haplotypecaller", default = true)
  var useUnifiedGenotyper: Boolean = config("use_unifiedgenotyper", default = false)
  var useAllelesOption: Boolean = config("use_alleles_option", default = false)
  var useMpileup: Boolean = config("use_mpileup", default = true)
  var useIndelRealigner: Boolean = config("use_indel_realign", default = true)
  var useBaseRecalibration: Boolean = config("use_base_recalibration", default = true)

  def init() {
    if (outputName == null && sampleID != null) outputName = sampleID
    else if (outputName == null) outputName = config("output_name", default = "noname")

    val baseRecalibrator = new BaseRecalibrator(this)
    if (preProcesBams && useBaseRecalibration && baseRecalibrator.knownSites.isEmpty) {
      logger.warn("No Known site found, skipping base recalibration")
      useBaseRecalibration = false
    }
  }

  private def doublePreProces(files: List[File]): List[File] = {
    if (files.isEmpty) throw new IllegalStateException("Files can't be empty")
    else if (!doublePreProces) files
    else if (files.size == 1) {
      val bamFile = new File(outputDir, files.head.getName)
      if (bamFile != files.head) {
        val oldIndex: File = new File(files.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex: File = swapExt(outputDir, bamFile, ".bam", ".bai")
        val baiLn = Ln(this, oldIndex, newIndex)
        add(baiLn)

        val bamLn = Ln(this, files.head, bamFile)
        bamLn.deps :+= baiLn.output
        add(bamLn)
      }
      List(bamFile)
    } else {
      val markDup = MarkDuplicates(this, files, new File(outputDir, outputName + ".dedup.bam"))
      markDup.isIntermediate = useIndelRealigner
      add(markDup)
      if (useIndelRealigner) {
        List(addIndelRealign(markDup.output, outputDir, isIntermediate = false))
      } else {
        List(markDup.output)
      }
    }
  }

  def biopetScript() {
    scriptOutput.bamFiles = {
      doublePreProces(if (preProcesBams) {
        for (inputBam <- inputBams) yield {
          var bamFile = inputBam
          if (useIndelRealigner)
            bamFile = addIndelRealign(bamFile, outputDir, isIntermediate = useBaseRecalibration)
          if (useBaseRecalibration)
            bamFile = addBaseRecalibrator(bamFile, outputDir, isIntermediate = inputBams.size > 1)
          bamFile
        }
      } else {
        inputBams
      })
    }

    if (variantcalling) {
      var mergBuffer: SortedMap[String, File] = SortedMap()
      def mergeList = mergBuffer map { case (key, file) => TaggedFile(removeNoneVariants(file), "name=" + key) }

      if (sampleID != null && (useHaplotypecaller || config("joint_genotyping", default = false).asBoolean)) {
        val hcGvcf = new HaplotypeCaller(this)
        hcGvcf.useGvcf()
        hcGvcf.input_file = scriptOutput.bamFiles
        hcGvcf.out = new File(outputDir, outputName + ".hc.discovery.gvcf.vcf.gz")
        add(hcGvcf)
        scriptOutput.gvcfFile = hcGvcf.out
      }

      if (useHaplotypecaller) {
        if (sampleID != null) {
          val genotypeGVCFs = GenotypeGVCFs(this, List(scriptOutput.gvcfFile), new File(outputDir, outputName + ".hc.discovery.vcf.gz"))
          add(genotypeGVCFs)
          scriptOutput.hcVcfFile = genotypeGVCFs.out
        } else {
          val hcGvcf = new HaplotypeCaller(this)
          hcGvcf.input_file = scriptOutput.bamFiles
          hcGvcf.out = new File(outputDir, outputName + ".hc.discovery.vcf.gz")
          add(hcGvcf)
          scriptOutput.hcVcfFile = hcGvcf.out
        }
        mergBuffer += ("1.HC-Discovery" -> scriptOutput.hcVcfFile)
      }

      if (useUnifiedGenotyper) {
        val ugVcf = new UnifiedGenotyper(this)
        ugVcf.input_file = scriptOutput.bamFiles
        ugVcf.out = new File(outputDir, outputName + ".ug.discovery.vcf.gz")
        add(ugVcf)
        scriptOutput.ugVcfFile = ugVcf.out
        mergBuffer += ("2.UG-Discovery" -> scriptOutput.ugVcfFile)
      }

      // Generate raw vcf
      if (useMpileup) {
        if (sampleID != null && scriptOutput.bamFiles.size == 1) {
          val m2v = new MpileupToVcf(this)
          m2v.inputBam = scriptOutput.bamFiles.head
          m2v.sample = sampleID
          m2v.output = new File(outputDir, outputName + ".raw.vcf")
          add(m2v)
          scriptOutput.rawVcfFile = m2v.output

          val vcfFilter = new VcfFilter(this) {
            override def defaults = Map("min_sample_depth" -> 8,
              "min_alternate_depth" -> 2,
              "min_samples_pass" -> 1,
              "filter_ref_calls" -> true
            )
          }
          vcfFilter.inputVcf = m2v.output
          vcfFilter.outputVcf = swapExt(outputDir, m2v.output, ".vcf", ".filter.vcf.gz")
          add(vcfFilter)
          scriptOutput.rawFilterVcfFile = vcfFilter.outputVcf
        } else if (rawVcfInput != null) scriptOutput.rawFilterVcfFile = rawVcfInput
        if (scriptOutput.rawFilterVcfFile != null) mergBuffer += ("9.raw" -> scriptOutput.rawFilterVcfFile)
      }

      // Allele mode
      if (useAllelesOption) {
        val mergeAlleles = MergeAlleles(this, mergeList.toList, outputDir + "raw.allele__temp_only.vcf.gz")
        mergeAlleles.isIntermediate = true
        add(mergeAlleles)

        if (useHaplotypecaller) {
          val hcAlleles = new HaplotypeCaller(this)
          hcAlleles.input_file = scriptOutput.bamFiles
          hcAlleles.out = new File(outputDir, outputName + ".hc.allele.vcf.gz")
          hcAlleles.alleles = mergeAlleles.output
          hcAlleles.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
          add(hcAlleles)
          scriptOutput.hcAlleleVcf = hcAlleles.out
          mergBuffer += ("3.HC-alleles" -> hcAlleles.out)
        }

        if (useUnifiedGenotyper) {
          val ugAlleles = new UnifiedGenotyper(this)
          ugAlleles.input_file = scriptOutput.bamFiles
          ugAlleles.out = new File(outputDir, outputName + ".ug.allele.vcf.gz")
          ugAlleles.alleles = mergeAlleles.output
          ugAlleles.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
          add(ugAlleles)
          scriptOutput.ugAlleleVcf = ugAlleles.out
          mergBuffer += ("4.UG-alleles" -> ugAlleles.out)
        }
      }

      def removeNoneVariants(input: File): File = {
        val output = input.getAbsolutePath.stripSuffix(".vcf.gz") + ".variants_only.vcf.gz"
        val sv = SelectVariants(this, input, output)
        sv.excludeFiltered = true
        sv.excludeNonVariants = true
        sv.isIntermediate = true
        add(sv)
        sv.out
      }

      val cvFinal = CombineVariants(this, mergeList.toList, new File(outputDir, outputName + ".final.vcf.gz"))
      cvFinal.genotypemergeoption = org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.GenotypeMergeType.UNSORTED
      add(cvFinal)

      val vcfStats = new VcfStats(this)
      vcfStats.input = cvFinal.out
      vcfStats.setOutputDir(new File(outputDir, "vcfstats"))
      add(vcfStats)

      scriptOutput.finalVcfFile = cvFinal.out
    }
  }

  def addIndelRealign(inputBam: File, dir: File, isIntermediate: Boolean = true): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    realignerTargetCreator.isIntermediate = true
    add(realignerTargetCreator)

    val indelRealigner = IndelRealigner(this, inputBam, realignerTargetCreator.out, dir)
    indelRealigner.isIntermediate = isIntermediate
    add(indelRealigner)

    indelRealigner.o
  }

  def addBaseRecalibrator(inputBam: File, dir: File, isIntermediate: Boolean = false): File = {
    val baseRecalibrator = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal"))

    if (baseRecalibrator.knownSites.isEmpty) {
      logger.warn("No Known site found, skipping base recalibration, file: " + inputBam)
      return inputBam
    }
    add(baseRecalibrator)

    if (config("use_analyze_covariates", default = false).asBoolean) {
      val baseRecalibratorAfter = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.after"))
      baseRecalibratorAfter.BQSR = baseRecalibrator.o
      add(baseRecalibratorAfter)

      add(AnalyzeCovariates(this, baseRecalibrator.o, baseRecalibratorAfter.o, swapExt(dir, inputBam, ".bam", ".baserecal.pdf")))
    }

    val printReads = PrintReads(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.bam"))
    printReads.BQSR = baseRecalibrator.o
    printReads.isIntermediate = isIntermediate
    add(printReads)

    printReads.o
  }
}

object GatkVariantcalling extends PipelineCommand {
  class ScriptOutput {
    var bamFiles: List[File] = _
    var gvcfFile: File = _
    var hcVcfFile: File = _
    var ugVcfFile: File = _
    var rawVcfFile: File = _
    var rawFilterVcfFile: File = _
    var hcAlleleVcf: File = _
    var ugAlleleVcf: File = _
    var finalVcfFile: File = _
  }
}
