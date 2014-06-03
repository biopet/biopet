package nl.lumc.sasc.biopet.pipelines.gatk.modules

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._

class BiopetVariantAnnotator(private var globalConfig: Config) extends QScript with BiopetQScript {
  qscript =>
  def this() = this(new Config())
  
  @Argument(doc="Config Json file",shortName="config", required=false) var configfiles: List[File] = Nil
  @Argument(doc="Reference",shortName="R") var referenceFile: File = _
  @Argument(doc="Variant",shortName="V") var variants: List[File] = Nil
  @Argument(doc="Output directory", shortName="outputDir", required=false) var outputDir: String = _
  @Argument(doc="Scattercount",shortName="SC") var scattercount: Int = 0
  
  def init {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("variantannotator"), globalConfig)
    if (outputDir == null) outputDir = this.qSettings.runDirectory
    if (scattercount == 0 && config.contains("scattercount")) scattercount = config.getAsInt("scattercount")
  }
  
  def script {
    init
    
    for (inputFile <- qscript.variants) {
      val variantAnnotator = new VariantAnnotator with gatkArguments {
        val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("variantannotator"), qscript.config)
        this.variant = inputFile
        if (config.contains("dbsnp")) this.dbsnp = config.getAsString("dbsnp")
        this.out = swapExt(outputDir, inputFile,".vcf",".annotated.vcf")
        if (qscript.scattercount > 1) this.scatterCount = qscript.scattercount
        if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
      }
      add(variantAnnotator)
    }
    
  }
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = referenceFile
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
}
