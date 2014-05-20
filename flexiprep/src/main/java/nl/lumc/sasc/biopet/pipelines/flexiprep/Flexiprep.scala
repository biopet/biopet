package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.wrappers._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.commandline._

class Flexiprep(private var globalConfig: Config) extends QScript {
  def this() = this(new Config())
  
  @Argument(doc="Config Json file",shortName="config", required=false) var configfiles: List[File] = Nil
  @Input(doc="R1 fastq file", shortName="R1",required=true) var input_R1: File = _
  @Input(doc="R2 fastq file", shortName="R2", required=false) var input_R2: File = _
  @Argument(doc="Output directory", shortName="outputDir", required=true) var outputDir: String = _
  @Argument(doc="Skip Trim fastq files", shortName="skiptrim", required=false) var skipTrim: Boolean = false
  @Argument(doc="Skip Clip fastq files", shortName="skipclip", required=false) var skipClip: Boolean = false
  
  var config: Config = _
  var outputFiles:Map[String,File] = Map()
  var paired: Boolean = (input_R2 != null)
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("flexiprep"), globalConfig)
    logger.debug(config)
    skipTrim = config.getAsBoolean("skiptrim", false)
    skipClip = config.getAsBoolean("skipclip", false)
    if (input_R1 == null) throw new IllegalStateException("Missing R1 on flexiprep module")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on flexiprep module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
    paired = (input_R2 != null)
  }
  
  def script() {
    init()
    
    runInitialFastqc()
    
    outputFiles += ("output_R1" -> zcatIfNeeded(input_R1,outputDir))
    if (paired) outputFiles += ("output_R2" -> zcatIfNeeded(input_R2,outputDir))
    
    var results: Map[String,File] = Map()
    if (paired) {
      results = runTrimClip(outputFiles("output_R1"), outputFiles("output_R2"), outputDir)
      outputFiles += ("output_R1" -> results("output_R1"))
      outputFiles += ("output_R2" -> results("output_R2"))
    } else {
      results = runTrimClip(outputFiles("output_R1"), outputDir)
      outputFiles += ("output_R1" -> results("output_R1"))
    }
    
    runFinalFastqc()
  }
  
  def runInitialFastqc() {
    var fastqc_R1 = runFastqc(input_R1,outputDir + "/fastqc_R1/")
    outputFiles += ("fastqc_R1" -> fastqc_R1.output)
    outputFiles += ("qualtype_R1" -> getQualtype(fastqc_R1))
    outputFiles += ("contams_R1" -> getContams(fastqc_R1))
    
    if (paired) {
      var fastqc_R2 = runFastqc(input_R2,outputDir + "/fastqc_R2/")
      outputFiles += ("fastqc_R2" -> fastqc_R2.output)
      outputFiles += ("qualtype_R2" -> getQualtype(fastqc_R2))
      outputFiles += ("contams_R2" -> getContams(fastqc_R2))
    }
  }
  
  def getQualtype(fastqc:Fastqc): File = {
    val fastqcToQualtype = new FastqcToQualtype(config)
    fastqcToQualtype.fastqc_output = fastqc.output
    var out: File = swapExt(outputDir, fastqc.fastqfile, "", ".qualtype.txt")
    fastqcToQualtype.out = out
    add(fastqcToQualtype)
    return out
  }
  
  def getContams(fastqc:Fastqc): File = {
    val fastqcToContams = new FastqcToContams(config)
    fastqcToContams.fastqc_output = fastqc.output
    var out: File = swapExt(outputDir, fastqc.fastqfile, "", ".contams.txt")
    fastqcToContams.out = out
    fastqcToContams.contams_file = fastqc.contaminants
    add(fastqcToContams)
    return out
  }
  
  def runTrimClip(R1_in:File, outDir:String) : Map[String,File] = {
    return runTrimClip(R1_in, new File(""), outDir)
  }
  def runTrimClip(R1_in:File, R2_in:File, outDir:String) : Map[String,File] = {
    var results: Map[String,File] = Map()
    
    var R1: File = new File(R1_in)
    var R2: File = new File(R2_in)
    var R1_ext: String = R1.getName().substring(R1.getName().lastIndexOf("."), R1.getName().size)
    var R2_ext: String = ""
    if (paired) R2_ext = R2.getName().substring(R2.getName().lastIndexOf("."), R2.getName().size)
    
    if (!skipClip) { // Adapter clipping
      val cutadapt_R1 = new Cutadapt(config)
      cutadapt_R1.fastq_input = R1
      cutadapt_R1.fastq_output = swapExt(outDir, R1, R1_ext, ".clip"+R1_ext)
      if (outputFiles.contains("contams_R1")) cutadapt_R1.contams_file = outputFiles("contams_R1")
      add(cutadapt_R1)
      R1 = cutadapt_R1.fastq_output
      if (paired) {
        val cutadapt_R2 = new Cutadapt(config)
        cutadapt_R2.fastq_input = R2
        cutadapt_R2.fastq_output = swapExt(outDir, R2, R2_ext, ".clip"+R2_ext)
        if (outputFiles.contains("contams_R2")) cutadapt_R2.contams_file = outputFiles("contams_R2")
        add(cutadapt_R2)
        R2 = cutadapt_R2.fastq_output
        val fastqSync = new FastqSync(config)
        fastqSync.input_start_fastq = cutadapt_R1.fastq_input
        fastqSync.input_R1 = cutadapt_R1.fastq_output
        fastqSync.input_R2 = cutadapt_R2.fastq_output
        fastqSync.output_R1 = swapExt(outDir, R1, ".clip"+R1_ext, ".clipsync"+R1_ext)
        fastqSync.output_R2 = swapExt(outDir, R2, ".clip"+R2_ext, ".clipsync"+R2_ext)
        fastqSync.output_stats = swapExt(outDir, R1, ".clip"+R1_ext, ".clipsync.stats")
        add(fastqSync)
        R1 = fastqSync.output_R1
        R2 = fastqSync.output_R2
      }
    }
    
    if (!skipTrim) { // Quality trimming
      val sickle = new Sickle(config)
      sickle.input_R1 = R1
      sickle.output_R1 = swapExt(outDir, R1, R1_ext, ".trim"+R1_ext)
      if (outputFiles.contains("qualtype_R1")) sickle.qualityTypeFile = outputFiles("qualtype_R1")
      if (paired) {
        sickle.input_R2 = R2
        sickle.output_R2 = swapExt(outDir, R2, R2_ext, ".trim"+R2_ext)
        sickle.output_singles = swapExt(outDir, R2, R2_ext, ".trim.singles"+R1_ext)
      }
      sickle.output_stats = swapExt(outDir, R1, R1_ext, ".trim.stats")
      add(sickle)
      R1 = sickle.output_R1
      if (paired) R2 = sickle.output_R2
    }
    
    results += ("output_R1" -> R1)
    if (paired) results += ("output_R2" -> R2)
    return results
  }
  
  def runFinalFastqc() {
    if (!skipTrim || !skipClip) {
      outputFiles += ("fastqc_R1_final" -> runFastqc(outputFiles("output_R1"),outputDir + "/fastqc_qc_R1/").output)
      if (paired) outputFiles += ("fastqc_R2_final" -> runFastqc(outputFiles("output_R2"),outputDir + "/fastqc_qc_R2/").output)
    }
  }
  
  def runFastqc(fastqfile:File, outDir:String) : Fastqc = {
    val fastqcCommand = new Fastqc(config)
    fastqcCommand.fastqfile = fastqfile
    var filename: String = fastqfile.getName()
    if (filename.endsWith(".gz")) filename = filename.substring(0,filename.size - 3)
    if (filename.endsWith(".gzip")) filename = filename.substring(0,filename.size - 5)
    if (filename.endsWith(".fastq")) filename = filename.substring(0,filename.size - 6)
    //if (filename.endsWith(".fq")) filename = filename.substring(0,filename.size - 3)
    fastqcCommand.output = outDir + "/" + filename + "_fastqc.ouput"
    add(fastqcCommand)
    return fastqcCommand
  }
  
  def zcatIfNeeded(file:File, runDir:String) : File = {
    if (file.getName().endsWith(".gz") || file.getName().endsWith(".gzip")) {
      var newFile: File = swapExt(file,".gz","")
      if (file.getName().endsWith(".gzip")) newFile = swapExt(file,".gzip","")
      val zcatCommand = new Zcat(config)
      zcatCommand.in = file
      zcatCommand.out = new File(runDir + newFile)
      add(zcatCommand)
      return zcatCommand.out
    } else return file
  }
}