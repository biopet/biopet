package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{BiopetQScript, PipelineCommand}
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.{CommandLineGATK, GenotypeGVCFs, SelectVariants}
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}

class GatkGenotyping(val root:Configurable) extends QScript with BiopetQScript {
  def this() = this(null)
  
  @Input(doc="Gvcf files", shortName="I")
  var inputGvcfs: List[File] = Nil
  
  @Argument(doc="Reference", shortName="R", required=false)
  var reference: File = _
  
  @Argument(doc="Dbsnp", shortName="dbsnp", required=false)
  var dbsnp: File = _
  
  @Argument(doc="OutputName", required=false)
  var outputName: String = "genotype"
  
  @Output(doc="OutputFile", shortName="O", required=false)
  var outputFile: File = _
  
  @Argument(doc="Samples", shortName="sample", required=false)
  var samples: List[String] = Nil
  
  def init() {
    if (reference == null) reference = config("reference")
    if (dbsnp == null) dbsnp = config("dbsnp")
    if (outputFile == null) outputFile = outputDir + outputName + ".vcf"
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }
  
  def biopetScript() {
    addGenotypeGVCFs(inputGvcfs, outputFile)
    if (!samples.isEmpty) {
      if (samples.size > 1) addSelectVariants(outputFile, samples, outputDir + "samples/", "all")
      for (sample <- samples) addSelectVariants(outputFile, List(sample), outputDir + "samples/", sample)
    }
  }
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = reference
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
  
  def addGenotypeGVCFs(gvcfFiles: List[File], outputFile:File): File = {
    val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments {
      this.variant = gvcfFiles
      if (configContains("dbsnp")) this.dbsnp = config("dbsnp")
      if (configContains("scattercount", submodule="genotypegvcfs"))
        this.scatterCount = config("scattercount", submodule="genotypegvcfs")
      this.out = outputFile
      if (config("inputtype", "dna").getString == "rna") {
        this.stand_call_conf = config("stand_call_conf", default=20, submodule="haplotypecaller")
        this.stand_emit_conf = config("stand_emit_conf", default=20, submodule="haplotypecaller")
      } else {
        this.stand_call_conf = config("stand_call_conf", default=30, submodule="haplotypecaller")
        this.stand_emit_conf = config("stand_emit_conf", default=30, submodule="haplotypecaller")
      }
    }
    add(genotypeGVCFs)
    return genotypeGVCFs.out
  }
  
  def addSelectVariants(inputFile:File, samples:List[String], outputDir:String, name:String) {
    val selectVariants = new SelectVariants with gatkArguments {
      this.variant = inputFile
      for (sample <- samples) this.sample_name :+= sample
      this.excludeNonVariants = true
      if (configContains("scattercount", submodule="selectvariants")) 
        this.scatterCount = config("scattercount", submodule="selectvariants")
      this.out = outputDir + name + ".vcf"
    }
    add(selectVariants)
  }
}

object GatkGenotyping extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkGenotyping.class"
}
