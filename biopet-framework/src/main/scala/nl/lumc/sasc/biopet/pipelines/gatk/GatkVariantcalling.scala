package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.{AnalyzeCovariates,BaseRecalibrator,GenotypeGVCFs,HaplotypeCaller,IndelRealigner,PrintReads,RealignerTargetCreator}
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
  var outputName: String = _

  @Output(doc = "OutputFile", required = false)
  var outputFile: File = _
  
  var gvcfMode = true
  var singleGenotyping = false

  def init() {
    if (gvcfMode) gvcfMode = config("gvcfmode", default = true)
    if (!singleGenotyping) singleGenotyping = config("singlegenotyping")
    if (reference == null) reference = config("reference", required = true)
    if (dbsnp == null) dbsnp = config("dbsnp")
    if (outputFile == null) outputFile = outputDir + 
      (if (outputName != null && !outputName.endsWith(".")) outputName + "." else outputName) + 
      (if (gvcfMode) "hc.gvcf.vcf" else ".vcf")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    var bamFiles: List[File] = Nil
    for (inputBam <- inputBams) {
      var bamFile = if (dbsnp != null) addIndelRealign(inputBam, outputDir) else inputBam
      bamFiles :+= addBaseRecalibrator(bamFile, outputDir)
    }
    addHaplotypeCaller(bamFiles, outputFile)
    if (gvcfMode && singleGenotyping) addGenotypeGVCFs(List(outputFile), outputDir)
  }

  def addIndelRealign(inputBam: File, dir: String): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    add(realignerTargetCreator, isIntermediate = true)

    val indelRealigner = IndelRealigner.apply(this, inputBam, realignerTargetCreator.out, dir)
    add(indelRealigner, isIntermediate = true)

    return indelRealigner.o
  }

  def addBaseRecalibrator(inputBam: File, dir: String): File = {
    val baseRecalibrator = BaseRecalibrator.apply(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal")) //with gatkArguments {
    add(baseRecalibrator)

    val baseRecalibratorAfter = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.after")) //with gatkArguments {
    baseRecalibratorAfter.BQSR = baseRecalibrator.o
    add(baseRecalibratorAfter)

    add(AnalyzeCovariates(this, baseRecalibrator.o, baseRecalibratorAfter.o, swapExt(dir, inputBam, ".bam", ".baserecal.pdf")))

    val printReads = PrintReads(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.bam"))
    printReads.BQSR = baseRecalibrator.o
    add(printReads, isIntermediate = false)

    return printReads.o
  }

  def addHaplotypeCaller(bamfiles: List[File], outputfile: File): File = {
    val haplotypeCaller = new HaplotypeCaller(this)
    haplotypeCaller.input_file = bamfiles
    haplotypeCaller.out = outputfile
    add(haplotypeCaller)

    return haplotypeCaller.out
  }

  def addGenotypeGVCFs(gvcfFiles: List[File], dir: String): File = {
    val genotypeGVCFs = GenotypeGVCFs(this, gvcfFiles, outputDir + outputName + ".vcf")
    add(genotypeGVCFs)
    return genotypeGVCFs.out
  }
}

object GatkVariantcalling extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkVariantcalling.class"
}
