package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand}
import java.io.File
import nl.lumc.sasc.biopet.tools.{ MpileupToVcf, VcfFilter }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.{AnalyzeCovariates,BaseRecalibrator,GenotypeGVCFs,HaplotypeCaller,IndelRealigner,PrintReads,RealignerTargetCreator, SelectVariants, CombineVariants, UnifiedGenotyper}
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import scala.collection.SortedMap

class GatkVariantcalling(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  val scriptOutput = new GatkVariantcalling.ScriptOutput
  
  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM")
  var inputBams: List[File] = Nil
  
  @Input(doc = "Raw vcf file", shortName = "raw")
  var rawVcfInput: File = _

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Dbsnp", shortName = "dbsnp", required = false)
  var dbsnp: File = _

  @Argument(doc = "OutputName", required = false)
  var outputName: String = _

  @Argument(doc = "Sample name", required = false)
  var sampleID: String = _
  
  var preProcesBams: Option[Boolean] = None
  var variantcalling: Boolean = true
  var doublePreProces: Option[Boolean] = None
  var useHaplotypecaller: Option[Boolean] = None
  var useUnifiedGenotyper: Option[Boolean] = None
  var useAllelesOption: Option[Boolean] = None

  def init() {
    if (useAllelesOption == None) useAllelesOption = config("use_alleles_option", default = false)
    if (preProcesBams == None) preProcesBams = config("pre_proces_bams", default = true)
    if (doublePreProces == None) doublePreProces = config("double_pre_proces", default = true)
    if (useHaplotypecaller == None) useHaplotypecaller = config("use_haplotypecaller", default = true)
    if (useUnifiedGenotyper == None) useUnifiedGenotyper = config("use_unifiedgenotyper", default = false)
    if (reference == null) reference = config("reference", required = true)
    if (dbsnp == null) dbsnp = config("dbsnp")
    if (outputName == null && sampleID != null) outputName = sampleID
    else if (outputName == null) outputName = "noname"
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  private def doublePreProces(files:List[File]): List[File] = {
      if (files.size == 1) return files
      if (files.isEmpty) throw new IllegalStateException("Files can't be empty")
      if (!doublePreProces.get) return files
      val markDub = MarkDuplicates(this, files, new File(outputDir + outputName + ".dedup.bam"))
      if (dbsnp != null) {
        add(markDub, isIntermediate = true)
        List(addIndelRealign(markDub.output, outputDir, isIntermediate = false))
      } else {
        add(markDub, isIntermediate = true)
        List(markDub.output)
      }
    }
  
  def biopetScript() {
    scriptOutput.bamFiles = if (preProcesBams.get) {
      var bamFiles: List[File] = Nil
      for (inputBam <- inputBams) {
        var bamFile = addIndelRealign(inputBam, outputDir)
        bamFiles :+= addBaseRecalibrator(bamFile, outputDir, isIntermediate = bamFiles.size > 1)
      }
      doublePreProces(bamFiles)
    } else if (inputBams.size > 1 && doublePreProces.get) {
      doublePreProces(inputBams)
    } else inputBams
    
    if (variantcalling) {
      var mergBuffer: SortedMap[String, File] = SortedMap()
      
      if (sampleID != null && (useHaplotypecaller.get || config("joint_genotyping", default = false).getBoolean)) {
        val hcGvcf = new HaplotypeCaller(this)
        hcGvcf.useGvcf
        hcGvcf.input_file = scriptOutput.bamFiles
        hcGvcf.out = outputDir + outputName + ".hc.discovery.gvcf.vcf.gz"
        add(hcGvcf)
        scriptOutput.gvcfFile = hcGvcf.out
      }
      
      if (useHaplotypecaller.get) {
        if (sampleID != null) {
          val genotypeGVCFs = GenotypeGVCFs(this, List(scriptOutput.gvcfFile), outputDir + outputName + ".hc.discovery.vcf.gz")
          add(genotypeGVCFs)
          scriptOutput.hcVcfFile = genotypeGVCFs.out
        } else {
          val hcGvcf = new HaplotypeCaller(this)
          hcGvcf.input_file = scriptOutput.bamFiles
          hcGvcf.out = outputDir + outputName + ".hc.discovery.vcf.gz"
          add(hcGvcf)
          scriptOutput.hcVcfFile = hcGvcf.out
        }
        mergBuffer += ("1.HC-Discovery" -> scriptOutput.hcVcfFile)
      }
      
      if (useUnifiedGenotyper.get) {
        val ugVcf = new UnifiedGenotyper(this)
        ugVcf.input_file = scriptOutput.bamFiles
        ugVcf.out = outputDir + outputName + ".ug.discovery.vcf.gz"
        add(ugVcf)
        scriptOutput.ugVcfFile = ugVcf.out
        mergBuffer += ("2.UG-Discovery" -> scriptOutput.ugVcfFile)
      }
      
      // Generate raw vcf
      if (sampleID != null && scriptOutput.bamFiles.size == 1) {
        val m2v = new MpileupToVcf(this)
        m2v.inputBam = scriptOutput.bamFiles.head
        m2v.sample = sampleID
        m2v.output = outputDir + outputName + ".raw.vcf"
        add(m2v)
        scriptOutput.rawVcfFile = m2v.output

        val vcfFilter = new VcfFilter(this)
        vcfFilter.defaults ++= Map("min_sample_depth" -> 8, 
                                   "min_alternate_depth" -> 2, 
                                   "min_samples_pass" -> 1, 
                                   "filter_ref_calls" -> true)
        vcfFilter.inputVcf = m2v.output
        vcfFilter.outputVcf = this.swapExt(outputDir, m2v.output, ".vcf", ".filter.vcf.gz")
        add(vcfFilter)
        scriptOutput.rawFilterVcfFile = vcfFilter.outputVcf
      } else if (rawVcfInput != null) scriptOutput.rawFilterVcfFile = rawVcfInput
      if (scriptOutput.rawFilterVcfFile == null) throw new IllegalStateException("Files can't be empty")
      mergBuffer += ("9.raw" -> scriptOutput.rawFilterVcfFile)
      
      if (useAllelesOption.get) {
        val alleleOnly = new CommandLineFunction {
          @Input val input: File = scriptOutput.rawFilterVcfFile
          @Output val output: File = outputDir + "raw.allele_only.vcf.gz"
          @Output val outputindex: File = outputDir + "raw.allele_only.vcf.gz.tbi"
          def commandLine = "zcat " + input + " | cut -f1,2,3,4,5,6,7,8 | bgzip -c > " + output + " && tabix -pvcf " + output
        }
        add(alleleOnly, isIntermediate = true)
        
        if (useHaplotypecaller.get) {
          val hcAlleles = new HaplotypeCaller(this)
          hcAlleles.input_file = scriptOutput.bamFiles
          hcAlleles.out = outputDir + outputName + ".hc.allele.vcf.gz"
          hcAlleles.alleles = alleleOnly.output
          hcAlleles.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
          add(hcAlleles)
          scriptOutput.hcAlleleVcf = hcAlleles.out
          mergBuffer += ("3.HC-alleles" -> hcAlleles.out)
        }
        
        if (useUnifiedGenotyper.get) {
          val ugAlleles = new UnifiedGenotyper(this)
          ugAlleles.input_file = scriptOutput.bamFiles
          ugAlleles.out = outputDir + outputName + ".ug.allele.vcf.gz"
          ugAlleles.alleles = alleleOnly.output
          ugAlleles.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
          add(ugAlleles)
          scriptOutput.ugAlleleVcf = ugAlleles.out
          mergBuffer += ("4.UG-alleles" -> ugAlleles.out)
        }
      }

      def removeNoneVariants(input:File): File = {
        val output = input.getAbsolutePath.stripSuffix(".vcf.gz") + ".variants_only.vcf.gz"
        val sv = SelectVariants(this, input, output)
        sv.excludeFiltered = true
        sv.excludeNonVariants = true
        add(sv, isIntermediate = true)
        sv.out
      }

      def mergeList = mergBuffer map {case (key, file) => TaggedFile(removeNoneVariants(file), "name=" + key)}
      val cvFinal = CombineVariants(this, mergeList.toList, outputDir + outputName + ".final.vcf.gz")
      add(cvFinal)
      scriptOutput.finalVcfFile = cvFinal.out
    }
  }

  def addIndelRealign(inputBam: File, dir: String, isIntermediate: Boolean = true): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    add(realignerTargetCreator, isIntermediate = true)

    val indelRealigner = IndelRealigner.apply(this, inputBam, realignerTargetCreator.out, dir)
    add(indelRealigner, isIntermediate = isIntermediate)

    return indelRealigner.o
  }

  def addBaseRecalibrator(inputBam: File, dir: String, isIntermediate: Boolean = false): File = {
    val baseRecalibrator = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal")) //with gatkArguments {
    
    if (baseRecalibrator.knownSites.isEmpty) {
      logger.warn("No Known site found, skipping base recalibration")
      return inputBam
    }
    add(baseRecalibrator)

    val baseRecalibratorAfter = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.after")) //with gatkArguments {
    baseRecalibratorAfter.BQSR = baseRecalibrator.o
    add(baseRecalibratorAfter)

    add(AnalyzeCovariates(this, baseRecalibrator.o, baseRecalibratorAfter.o, swapExt(dir, inputBam, ".bam", ".baserecal.pdf")))

    val printReads = PrintReads(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.bam"))
    printReads.BQSR = baseRecalibrator.o
    add(printReads, isIntermediate = isIntermediate)

    return printReads.o
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
