package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.core.config.Configurable
import java.io.File
import java.util.Date
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.apps.FastqSplitter
import nl.lumc.sasc.biopet.extensions.aligners.{ Bwa, Star }
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument, ClassType }
import org.broadinstitute.gatk.queue.extensions.picard.{ MergeSamFiles, SortSam, AddOrReplaceReadGroups }
import scala.math._

class Mapping(val root: Configurable) extends QScript with BiopetQScript {
  qscript =>
  def this() = this(null)

  @Input(doc = "R1 fastq file", shortName = "R1", required = true)
  var input_R1: File = _

  @Input(doc = "R2 fastq file", shortName = "R2", required = false)
  var input_R2: File = _

  @Argument(doc = "Output name", shortName = "outputName", required = false)
  var outputName: String = _

  @Argument(doc = "Skip flexiprep", shortName = "skipflexiprep", required = false)
  var skipFlexiprep: Boolean = false

  @Argument(doc = "Skip mark duplicates", shortName = "skipmarkduplicates", required = false)
  var skipMarkduplicates: Boolean = false

  @Argument(doc = "Alginer", shortName = "ALN", required = false)
  var aligner: String = _

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Chunking", shortName = "chunking", required = false)
  var chunking: Boolean = false
  
  @ClassType(classOf[Int])
  @Argument(doc = "Number of chunks, when not defined pipeline will automatic calculate number of chunks", shortName = "numberChunks", required = false)
  var numberChunks: Option[Int] = None

  // Readgroup items
  @Argument(doc = "Readgroup ID", shortName = "RGID", required = false)
  var RGID: String = _

  @Argument(doc = "Readgroup Library", shortName = "RGLB", required = false)
  var RGLB: String = _

  @Argument(doc = "Readgroup Platform", shortName = "RGPL", required = false)
  var RGPL: String = _

  @Argument(doc = "Readgroup platform unit", shortName = "RGPU", required = false)
  var RGPU: String = _

  @Argument(doc = "Readgroup sample", shortName = "RGSM", required = false)
  var RGSM: String = _

  @Argument(doc = "Readgroup sequencing center", shortName = "RGCN", required = false)
  var RGCN: String = _

  @Argument(doc = "Readgroup description", shortName = "RGDS", required = false)
  var RGDS: String = _

  @Argument(doc = "Readgroup sequencing date", shortName = "RGDT", required = false)
  var RGDT: Date = _

  @Argument(doc = "Readgroup predicted insert size", shortName = "RGPI", required = false)
  var RGPI: Int = _

  var paired: Boolean = false

  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    var inputtype: String = config("inputtype", "dna")
    if (aligner == null) {
      if (inputtype == "rna") aligner = config("aligner", "star-2pass")
      else aligner = config("aligner", "bwa")
    }
    if (reference == null) reference = config("reference")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on mapping module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
    if (input_R1 == null) throw new IllegalStateException("Missing Fastq R1 on mapping module")
    paired = (input_R2 != null)

    if (RGLB == null && configContains("RGLB")) RGLB = config("RGLB")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup library on mapping module")
    if (RGSM == null && configContains("RGSM")) RGSM = config("RGSM")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup sample on mapping module")
    if (RGID == null && configContains("RGID")) RGID = config("RGID")
    else if (RGID == null && RGSM != null && RGLB != null) RGID = RGSM + "-" + RGLB
    else if (RGID == null) throw new IllegalStateException("Missing Readgroup ID on mapping module")

    if (RGPL == null) RGPL = config("RGPL", "illumina")
    if (RGPU == null) RGPU = config("RGPU", "na")
    if (RGCN == null && configContains("RGCN")) RGCN = config("RGCN")
    if (RGDS == null && configContains("RGDS")) RGDS = config("RGDS")

    if (outputName == null) outputName = RGID

    if (!chunking && numberChunks.isDefined) chunking = true
    if (!chunking) chunking = config("chunking", false)
    if (chunking) {
      if (numberChunks.isEmpty) {
        if (configContains("numberchunks")) numberChunks = config("numberchunks", default = None)
        else {
          val chunkSize: Int = config("chunksize", (1 << 30))
          val filesize = if (input_R1.getName.endsWith(".gz") || input_R1.getName.endsWith(".gzip")) input_R1.length * 3
                         else input_R1.length
          numberChunks = Option(ceil(filesize.toDouble / chunkSize).toInt)
        }
      }
      logger.debug("Chunks: " + numberChunks.getOrElse(1))
    }
  }

  def biopetScript() {
    var fastq_R1: File = input_R1
    var fastq_R2: File = if (paired) input_R2 else ""
    val flexiprep = new Flexiprep(this)
    flexiprep.outputDir = outputDir + "flexiprep/"
    var bamFiles: List[File] = Nil
    var fastq_R1_output: List[File] = Nil
    var fastq_R2_output: List[File] = Nil

    def removeGz(file: String): String = {
      if (file.endsWith(".gz")) return file.substring(0, file.lastIndexOf(".gz"))
      else if (file.endsWith(".gzip")) return file.substring(0, file.lastIndexOf(".gzip"))
      else return file
    }
    var chunks: Map[String, (String, String)] = Map()
    if (chunking) for (t <- 1 to numberChunks.getOrElse(1)) {
      chunks += ("chunk_" + t -> (removeGz(outputDir + "chunk_" + t + "/" + fastq_R1.getName),
        if (paired) removeGz(outputDir + "chunk_" + t + "/" + fastq_R2.getName) else ""))
    }
    else chunks += ("flexiprep" -> (flexiprep.extractIfNeeded(fastq_R1, flexiprep.outputDir),
      flexiprep.extractIfNeeded(fastq_R2, flexiprep.outputDir)))

    if (chunking) {
      val fastSplitter_R1 = new FastqSplitter(this)
      fastSplitter_R1.input = fastq_R1
      fastSplitter_R1.memoryLimit = 4
      fastSplitter_R1.jobResourceRequests :+= "h_vmem=8G"
      for ((chunk, fastqfile) <- chunks) fastSplitter_R1.output :+= fastqfile._1
      add(fastSplitter_R1)

      if (paired) {
        val fastSplitter_R2 = new FastqSplitter(this)
        fastSplitter_R2.input = fastq_R2
        fastSplitter_R2.memoryLimit = 4
        fastSplitter_R2.jobResourceRequests :+= "h_vmem=8G"
        for ((chunk, fastqfile) <- chunks) fastSplitter_R2.output :+= fastqfile._2
        add(fastSplitter_R2)
      }
    }

    for ((chunk, fastqfile) <- chunks) {
      var R1 = fastqfile._1
      var R2 = fastqfile._2
      var deps: List[File] = Nil
      val chunkDir = if (chunking) outputDir + chunk + "/" else outputDir
      if (!skipFlexiprep) {
        flexiprep.input_R1 = fastq_R1
        if (paired) flexiprep.input_R2 = fastq_R2
        flexiprep.sampleName = this.RGSM
        flexiprep.libraryName = this.RGLB
        flexiprep.init
        flexiprep.runInitialJobs
        //flexiprep.biopetScript
        val flexiout = flexiprep.runTrimClip(R1, R2, chunkDir + "flexiprep/", chunk)
        logger.debug(chunk + " - " + flexiout)
        R1 = flexiout._1
        if (paired) R2 = flexiout._2
        deps = flexiout._3
        fastq_R1_output :+= R1
        fastq_R2_output :+= R2
      }
      
      if (aligner == "bwa") {
        val bwaCommand = new Bwa(this)
        bwaCommand.R1 = R1
        if (paired) bwaCommand.R2 = R2
        bwaCommand.deps = deps
        bwaCommand.RG = getReadGroup
        bwaCommand.output = new File(chunkDir + outputName + ".sam")
        add(bwaCommand, isIntermediate = true)
        bamFiles :+= addSortSam(List(bwaCommand.output), swapExt(chunkDir, bwaCommand.output, ".sam", ".bam"), chunkDir)
      } else if (aligner == "star") {
        val starCommand = Star(this, R1, if (paired) R2 else null, outputDir, isIntermediate = true, deps = deps)
        add(starCommand)
        bamFiles :+= addAddOrReplaceReadGroups(List(starCommand.outputSam), new File(chunkDir + outputName + ".bam"), chunkDir)
      } else if (aligner == "star-2pass") {
        val star2pass = Star._2pass(this, R1, if (paired) R2 else null, chunkDir, isIntermediate = true, deps = deps)
        addAll(star2pass._2)
        bamFiles :+= addAddOrReplaceReadGroups(List(star2pass._1), new File(chunkDir + outputName + ".bam"), chunkDir)
      } else throw new IllegalStateException("Option Alginer: '" + aligner + "' is not valid")
    }
    if (!skipFlexiprep) {
      flexiprep.runFinalize(fastq_R1_output, fastq_R2_output)
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
    }

    var bamFile = bamFiles.head
    if (!skipMarkduplicates) {
      bamFile = new File(outputDir + outputName + ".dedup.bam")
      add(MarkDuplicates(this, bamFiles, bamFile))
    } else if (skipMarkduplicates && chunking) bamFile = addMergeBam(bamFiles, new File(outputDir + outputName + ".bam"), outputDir)
    
    addAll(BamMetrics.apply(this, bamFile, outputDir + "metrics/").functions)
    
    outputFiles += ("finalBamFile" -> bamFile)
  }

  def addSortSam(inputSam: List[File], outputFile: File, dir: String): File = {
    val sortSam = new SortSam
    sortSam.input = inputSam
    sortSam.createIndex = true
    sortSam.output = outputFile
    sortSam.memoryLimit = 2
    sortSam.nCoresRequest = 2
    sortSam.jobResourceRequests :+= "h_vmem=4G"
    if (!skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)

    return sortSam.output
  }

  def addMergeBam(inputSam: List[File], outputFile: File, dir: String): File = {
    val mergeSam = new MergeSamFiles
    mergeSam.input = inputSam
    mergeSam.createIndex = true
    mergeSam.output = outputFile
    mergeSam.memoryLimit = 2
    mergeSam.nCoresRequest = 2
    mergeSam.assumeSorted = true
    mergeSam.USE_THREADING = true
    mergeSam.jobResourceRequests :+= "h_vmem=4G"
    if (!skipMarkduplicates) mergeSam.isIntermediate = true
    add(mergeSam)

    return mergeSam.output
  }

  def addAddOrReplaceReadGroups(inputSam: List[File], outputFile: File, dir: String): File = {
    val addOrReplaceReadGroups = new AddOrReplaceReadGroups
    addOrReplaceReadGroups.input = inputSam
    addOrReplaceReadGroups.output = outputFile
    addOrReplaceReadGroups.createIndex = true
    addOrReplaceReadGroups.memoryLimit = 2
    addOrReplaceReadGroups.nCoresRequest = 2
    addOrReplaceReadGroups.jobResourceRequests :+= "h_vmem=4G"

    addOrReplaceReadGroups.RGID = RGID
    addOrReplaceReadGroups.RGLB = RGLB
    addOrReplaceReadGroups.RGPL = RGPL
    addOrReplaceReadGroups.RGPU = RGPU
    addOrReplaceReadGroups.RGSM = RGSM
    if (RGCN != null) addOrReplaceReadGroups.RGCN = RGCN
    if (RGDS != null) addOrReplaceReadGroups.RGDS = RGDS
    if (!skipMarkduplicates) addOrReplaceReadGroups.isIntermediate = true
    add(addOrReplaceReadGroups)

    return addOrReplaceReadGroups.output
  }

  def getReadGroup(): String = {
    var RG: String = "@RG\\t" + "ID:" + RGID + "\\t"
    RG += "LB:" + RGLB + "\\t"
    RG += "PL:" + RGPL + "\\t"
    RG += "PU:" + RGPU + "\\t"
    RG += "SM:" + RGSM + "\\t"
    if (RGCN != null) RG += "CN:" + RGCN + "\\t"
    if (RGDS != null) RG += "DS" + RGDS + "\\t"
    if (RGDT != null) RG += "DT" + RGDT + "\\t"
    if (RGPI > 0) RG += "PI" + RGPI + "\\t"

    return RG.substring(0, RG.lastIndexOf("\\t"))
  }
}

object Mapping extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/mapping/Mapping.class"

  def loadFromRunConfig(root: Configurable, runConfig: Map[String, Any], sampleConfig: Map[String, Any], runDir: String): Mapping = {
    val mapping = new Mapping(root)

    logger.debug("Mapping runconfig: " + runConfig)
    var inputType = ""
    if (runConfig.contains("inputtype")) inputType = runConfig("inputtype").toString
    else inputType = root.config("inputtype", "dna").getString
    if (inputType == "rna") mapping.aligner = root.config("rna_aligner", "star-2pass").getString
    if (runConfig.contains("R1")) mapping.input_R1 = new File(runConfig("R1").toString)
    if (runConfig.contains("R2")) mapping.input_R2 = new File(runConfig("R2").toString)
    mapping.paired = (mapping.input_R2 != null)
    mapping.RGLB = runConfig("ID").toString
    mapping.RGSM = sampleConfig("ID").toString
    if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
    if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
    if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
    mapping.outputDir = runDir

    mapping.init
    mapping.biopetScript
    return mapping
  }
}
