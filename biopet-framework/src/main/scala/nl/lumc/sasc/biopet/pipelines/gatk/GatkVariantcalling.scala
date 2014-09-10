package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.apps.MpileupToVcf
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.{AnalyzeCovariates,BaseRecalibrator,GenotypeGVCFs,HaplotypeCaller,IndelRealigner,PrintReads,RealignerTargetCreator}
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class GatkVariantcalling(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM")
  var inputBams: List[File] = Nil

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Dbsnp", shortName = "dbsnp", required = false)
  var dbsnp: File = _

  @Argument(doc = "OutputName", required = false)
  var outputName: String = ""

  @Output(doc = "OutputFile", required = false)
  var outputFile: File = _
  
  var gvcfMode = true
  var singleGenotyping = false
  var preProcesBams = true
  var variantcalling = true

  def init() {
    if (gvcfMode) gvcfMode = config("gvcfmode", default = true)
    if (!singleGenotyping) singleGenotyping = config("singlegenotyping")
    if (reference == null) reference = config("reference", required = true)
    if (dbsnp == null) dbsnp = config("dbsnp")
    if (outputFile == null) outputFile = outputDir + 
      (if (!outputName.isEmpty && !outputName.endsWith(".")) outputName + "." else outputName) + 
      (if (gvcfMode) "hc.gvcf.vcf.gz" else ".vcf.gz")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    def doublePreProces(files:List[File]): File = {
      val markDub = MarkDuplicates(this, files, new File(outputDir + outputName + ".dedup.bam"))
      if (dbsnp != null && config("double_pre_proces", default = true).getBoolean) {
        add(markDub, isIntermediate = true)
        addIndelRealign(markDub.output, outputDir, isIntermediate = false) 
      } else {
        add(markDub, isIntermediate = true)
        markDub.output
      }
    }
    var bamFiles: List[File] = if (preProcesBams) {
      var bamFiles: List[File] = Nil
      for (inputBam <- inputBams) {
        var bamFile = if (dbsnp != null) addIndelRealign(inputBam, outputDir) else inputBam
        bamFiles :+= addBaseRecalibrator(bamFile, outputDir, isIntermediate = bamFiles.size > 1)
      }
      outputFiles += "final_bam" -> doublePreProces(bamFiles)
      List(outputFiles("final_bam"))
    } else if (inputBams.size > 1 && config("double_pre_proces", default = true).getBoolean) {
      List(doublePreProces(inputBams))
    } else inputBams
    
    if (variantcalling) {
      // Haplotypecaller with default settings
      val hc = new HaplotypeCaller(this)
      hc.defaults += "emitRefConfidence" -> "GVCF"
      hc.input_file = bamFiles
      hc.out = outputFile
      add(hc)
      outputFiles += "gvcf" -> hc.out

      // Generate raw vcf
      val bamFile: File = if (bamFiles.size > 1) {
        val markDub = MarkDuplicates(this, bamFiles, new File(outputDir + "dedup.bam"))
        add(markDub, isIntermediate = true)
        markDub.output
      } else bamFiles.head

      val m2v = new MpileupToVcf(this)
      m2v.inputBam = bamFile
      m2v.output = outputDir + outputName + "_raw.vcf"
      add(m2v)
      outputFiles += "raw_vcf" -> m2v.output

      val hcAlleles = new HaplotypeCaller(this)
      hcAlleles.defaults += "emitRefConfidence" -> "NONE"
      hcAlleles.input_file = bamFiles
      hcAlleles.out = outputDir + outputName + ".genotype_raw_alleles.vcf.gz"
      hcAlleles.alleles = m2v.output
      hcAlleles.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
      add(hcAlleles)
      outputFiles += "raw_genotype_vcf" -> hcAlleles.out

      if (gvcfMode && singleGenotyping) {
        val genotypeGVCFs = GenotypeGVCFs(this, List(outputFile), outputDir + outputName + ".vcf.gz")
        add(genotypeGVCFs)
        outputFiles += "vcf" -> genotypeGVCFs.out
      }
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
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkVariantcalling.class"
}
