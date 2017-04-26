package nl.lumc.sasc.biopet.pipelines.tarmac

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, PedigreeQscript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.extensions.bedtools.{ BedtoolsIntersect, BedtoolsSort }
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.stouffbed.{ StouffbedHorizontal, StouffbedVertical }
import nl.lumc.sasc.biopet.extensions.wisecondor.{ WisecondorCount, WisecondorGcCorrect, WisecondorNewRef, WisecondorZscore }
import nl.lumc.sasc.biopet.extensions.xhmm.{ XhmmMatrix, XhmmMergeGatkDepths, XhmmNormalize, XhmmPca }
import nl.lumc.sasc.biopet.extensions.{ Bgzip, Ln, Tabix }
import nl.lumc.sasc.biopet.pipelines.tarmac.scripts.{ BedThreshold, SampleFromMatrix }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction

import scalaz.{ -\/, \/, \/- }

/**
 * Created by Sander Bollen on 23-3-17.
 */
class Tarmac(val parent: Configurable) extends QScript with PedigreeQscript with SummaryQScript with Reference {
  qscript =>

  lazy val targets: File = config("targets")
  lazy val stouffWindowSizes: List[Int] = config("stouff_window_size")
  lazy val threshold: Int = config("threshold")

  def this() = this(null)

  private var _finalFiles: Map[Sample, List[File]] = Map()
  def finalFiles: Map[Sample, List[File]] = _finalFiles

  /* Fixed values for xhmm count file */
  override def fixedValues: Map[String, Any] = {
    super.fixedValues ++ Map(
      "depth_of_coverage" -> Map(
        "downsampling_type" -> "BY_SAMPLE",
        "downsample_to_coverage" -> 5000,
        "omit_depth_output_at_each_base" -> true,
        "omit_locus_table" -> true,
        "min_base_quality" -> 0,
        "min_mapping_quality" -> 20,
        "start" -> 1,
        "stop" -> 5000,
        "n_bins" -> 200,
        "include_ref_n_sites" -> true,
        "count_type" -> "COUNT_FRAGMENTS"
      )
    )
  }

  def init() = {

  }

  def biopetScript() = {
    addSamplesJobs()
    addSummaryJobs()
  }

  def addMultiSampleJobs() = {
    val initRefMap = samples map { case (sampleName, sample) => sample -> getReferenceSamplesForSample(sampleName) }
    initRefMap.values.collect { case -\/(error) => error }.foreach(Logging.addError(_))

    val refMap: Map[Sample, Set[Sample]] = initRefMap.collect {
      case (sample, \/-(sampleSet)) =>
        val actualRefSamples = sampleSet map { sampleId =>
          samples.getOrElse(sampleId, Logging.addError(s"Sample $sampleId does not exist"))
        } collect { case s: Sample => s }
        sample -> actualRefSamples
    }

    val wisecondorRefJobs = refMap map {
      case (sample, refSamples) =>
        val refDir = new File(sample.wisecondorDir, "wisecondor_reference")
        sample -> createWisecondorReferenceJobs(refSamples, refDir)
    }

    val xhmmRefJobs = refMap map {
      case (sample, refSamples) =>
        val refDir = new File(sample.xhmmDir, "xhmm_reference")
        sample -> createXhmmReferenceJobs(sample, refSamples, refDir)
    }

    val wisecondorZJobs = wisecondorRefJobs map {
      case (sample, jobsAndRefFile) =>
        val refJobs = jobsAndRefFile._1
        val refFile = jobsAndRefFile._2
        val tbiFile = refJobs.collect { case tbi: Tabix => tbi.outputIndex }.head
        sample -> createWisecondorZScore(sample, refFile, tbiFile)
    }

    val xhmmZJobs = xhmmRefJobs map {
      case (sample, jobsAndRefFile) =>
        sample -> createXhmmZscore(sample, jobsAndRefFile._2)
    }

    val wisecondorSyncJobs = wisecondorZJobs map {
      case (sample, job) =>
        val intersect = new BedtoolsIntersect(this)
        intersect.input = job.output
        intersect.intersectFile = xhmmZJobs(sample)._2
        intersect.output = new File(sample.wisecondorDir, s"${sample.sampleId}.wisecondor.sync.z.bed")
        sample -> intersect
    }

    val xhmmSyncJobs = xhmmZJobs map {
      case (sample, jobsAndFile) =>
        val intersect = new BedtoolsIntersect(this)
        intersect.input = jobsAndFile._2
        intersect.intersectFile = wisecondorZJobs(sample).output
        intersect.output = new File(sample.xhmmDir, s"${sample.sampleId}.xhmm.sync.z.bed")
        sample -> intersect
    }

    val zScoreMergeJobs = samples map {
      case (_, sample) =>
        val horizontal = new StouffbedHorizontal(this)
        val inputs = List(
          wisecondorSyncJobs(sample).output,
          xhmmSyncJobs(sample).output
        )
        horizontal.inputFiles = inputs
        horizontal.output = new File(sample.sampleDir, s"${sample.sampleId}.horizontal.bed")
        sample -> horizontal
    }

    val windowStouffJobs = zScoreMergeJobs map {
      case (sample, horizontal) =>
        val subMap = stouffWindowSizes map { size =>
          val windowDir = new File(sample.sampleDir, s"window_$size")
          val vertical = new StouffbedVertical(this)
          vertical.inputFiles = List(horizontal.output)
          vertical.output = new File(windowDir, s"${sample.sampleId}.window_$size.z.bed")
          vertical.windowSize = size
          size -> vertical
        }
        sample -> subMap.toMap
    }

    val thresholdJobs = windowStouffJobs map {
      case (sample, subMap) =>
        val threshSubMap = subMap map {
          case (size, job) =>
            val windowDir = new File(sample.sampleDir, s"window_$size")
            val thresholder = new BedThreshold(this)
            thresholder.input = job.output
            thresholder.output = Some(new File(windowDir, s"${sample.sampleId}.threshold.bed"))
            thresholder.threshold = threshold
            size -> thresholder
        }
        sample -> threshSubMap
    }

    _finalFiles = thresholdJobs map {
      case (sample, subMap) =>
        sample -> subMap.values.flatMap(_.output).toList
    }

    addAll(xhmmRefJobs.values.flatMap(_._1))
    addAll(wisecondorRefJobs.values.flatMap(_._1))
    addAll(xhmmZJobs.values.flatMap(_._1))
    addAll(wisecondorZJobs.values)
    addAll(wisecondorSyncJobs.values)
    addAll(xhmmSyncJobs.values)
    addAll(zScoreMergeJobs.values)
    addAll(windowStouffJobs.values.flatMap(_.values))
    addAll(thresholdJobs.values.flatMap(_.values))
  }

  /**
   * Get set of sample names constituting reference samples for a given sample name
   *
   * Reference samples must match the own gender, while excluding own parents (if any) and self
   *
   * @param sampleName: The sample name to create reference set for
   * @return
   */
  def getReferenceSamplesForSample(sampleName: String): String \/ Set[String] = {
    val allSampleNames = pedSamples.map(_.individualId)
    if (!allSampleNames.toSet.contains(sampleName)) {
      -\/(s"Sample $sampleName does not exist in PED samples")
    } else {
      val theSample = pedSamples(allSampleNames.indexOf(sampleName))
      val totalSet = pedSamples.filter(p => p.gender == theSample.gender).map(_.individualId).toSet
      val referenceSet = (theSample.maternalId, theSample.paternalId) match {
        case (Some(m), Some(f)) => totalSet - (m, f, sampleName)
        case (None, Some(f))    => totalSet - (f, sampleName)
        case (Some(m), None)    => totalSet - (m, sampleName)
        case _                  => totalSet - sampleName
      }
      \/-(referenceSet)
    }
  }

  /*
  Create jobs for Xhmm reference creation
  Returns both jobs and the resulting reference file
   */
  def createXhmmReferenceJobs(sample: Sample, referenceSamples: Set[Sample], outputDirectory: File): Tuple2[List[QFunction], File] = {
    /* XHMM requires refset including self */
    val totalSet = referenceSamples + sample
    val merger = new XhmmMergeGatkDepths(this)
    merger.gatkDepthsFiles = totalSet.map(_.outputXhmmCountFile).collect { case \/-(file) => file }.toList
    val refFile = new File(outputDirectory, "reference.matrix")
    merger.output = refFile
    (List(merger), refFile)
  }

  /*
  Create jobs for wisecondor reference creation
  Returns both jobs and the resulting reference file
   */
  def createWisecondorReferenceJobs(referenceSamples: Set[Sample], outputDirectory: File): Tuple2[List[QFunction], File] = {
    val gccs = referenceSamples.map(_.outputWisecondorGccFile).collect { case \/-(file) => file }.toList
    val reference = new WisecondorNewRef(this)
    reference.inputBeds = gccs
    reference.output = new File(outputDirectory, "reference.bed")
    reference.isIntermediate = true
    val sort = new BedtoolsSort(this)
    sort.input = reference.output
    sort.output = new File(outputDirectory, "reference.sorted.bed")
    sort.isIntermediate = true
    val refFile = new File(outputDirectory, "reference.bed.gz")
    val gzipRef = new Bgzip(this) // FIXME change to pipe with sort
    gzipRef.input = List(sort.output)
    gzipRef.output = refFile
    val tabix = Tabix(this, refFile)
    (List(new BiopetFifoPipe(this, reference :: sort :: gzipRef :: Nil), tabix), refFile)
  }

  def createWisecondorZScore(sample: Sample, referenceFile: File, tbiFile: File): WisecondorZscore = {
    val zscore = new WisecondorZscore(this)
    val zFile = new File(sample.wisecondorDir, s"${sample.sampleId}.z.bed")
    zscore.inputBed = sample.outputWisecondorGzFile.getOrElse(new File(""))
    zscore.deps ++= sample.outputWisecondorTbiFile.toList
    zscore.deps :+= tbiFile
    zscore.referenceDictionary = referenceFile
    zscore.output = zFile
    zscore
  }

  def createXhmmZscore(sample: Sample, referenceMatrix: File): Tuple2[List[QFunction], File] = {

    // the filtered and centered matrix
    val filtMatrix = new XhmmMatrix(this)
    filtMatrix.inputMatrix = referenceMatrix
    filtMatrix.outputMatrix = new File(sample.xhmmDir, s"${sample.sampleId}.filtered-centered.matrix")
    filtMatrix.outputExcludedSamples = Some(new File(sample.xhmmDir, s"${sample.sampleId}.filtered-samples.txt"))
    filtMatrix.outputExcludedTargets = Some(new File(sample.xhmmDir, s"${sample.sampleId}.filtered-targets.txt"))

    // pca generation
    val pca = new XhmmPca(this)
    pca.inputMatrix = filtMatrix.outputMatrix
    pca.pcaFile = new File(sample.xhmmDir, s"${sample.sampleId}.pca.matrix")

    // normalization
    val normalize = new XhmmNormalize(this)
    normalize.inputMatrix = filtMatrix.outputMatrix
    normalize.pcaFile = pca.pcaFile
    normalize.normalizeOutput = new File(sample.xhmmDir, s"${sample.sampleId}.normalized.matrix")

    // create matrix of zscores
    val zMatrix = new XhmmMatrix(this)
    zMatrix.inputMatrix = normalize.normalizeOutput
    zMatrix.centerData = true
    zMatrix.centerType = "sample"
    zMatrix.zScoreData = true
    zMatrix.outputExcludedTargets = Some(new File(sample.xhmmDir, s"${sample.sampleId}.z-filtered-targets.txt"))
    zMatrix.outputExcludedSamples = Some(new File(sample.xhmmDir, s"${sample.sampleId}.z-filtered-samples.txt"))
    zMatrix.outputMatrix = new File(sample.xhmmDir, "zscores.matrix")

    // select sample from matrix
    val selector = new SampleFromMatrix(this)
    selector.sample = sample.sampleId
    selector.inputMatrix = zMatrix.outputMatrix
    val zscoreFile = new File(sample.xhmmDir, s"${sample.sampleId}.zscore-xhmm.bed")
    selector.output = Some(zscoreFile)

    (selector :: zMatrix :: normalize :: pca :: filtMatrix :: Nil, zscoreFile)
  }

  class Sample(name: String) extends AbstractSample(name) {

    val inputXhmmCountFile: Option[File] = config("xhmm_count_file")
    val inputWisecondorCountFile: Option[File] = config("wisecondor_count_file")
    val bamFile: Option[File] = config("bam")

    val wisecondorDir: File = new File(sampleDir, "wisecondor")
    val xhmmDir: File = new File(sampleDir, "xhmm")

    /**
     * Create XHMM count file or create link to input count file
     * Precedence is given to existing count files.
     * Returns a disjunction where right is the file, and left is
     * a potential error message
     */
    protected lazy val outputXhmmCountJob: String \/ QFunction = {
      val outFile = new File(xhmmDir, s"$name.dcov")
      (inputXhmmCountFile, bamFile) match {
        case (Some(f), _) => {
          if (bamFile.isDefined) {
            logger.warn(s"Both BAM and Xhmm count files are given for sample $name. The BAM file will be ignored")
          }
          val ln = new Ln(root)
          ln.input = f
          ln.output = outFile
          \/-(ln)
        }
        case (None, Some(bam)) => {
          val dcov = DepthOfCoverage(root, List(bam), outFile, List(targets))
          \/-(dcov)
        }
        case _ => -\/(s"Cannot find bam file or xhmm count file for sample" +
          s" $name in config. At least one must be given.")
      }

    }

    /* Get count file for Xhmm method */
    lazy val outputXhmmCountFile: String \/ File = {
      outputXhmmCountJob match {
        case \/-(ln: Ln)               => \/-(ln.output)
        case \/-(doc: DepthOfCoverage) => \/-(doc.intervalSummaryFile)
        case -\/(error)                => -\/(error)
      }
    }

    /**
     * Create wisecondor count file or create link to input count file.
     * Precedence is given to existing count files.
     * Returns a disjunction where right is the file, and left is
     * a potential error message
     */
    protected lazy val outputWisecondorCountJob: String \/ QFunction = {
      val outFile = new File(wisecondorDir, s"$name.wisecondor.bed")
      (inputWisecondorCountFile, bamFile) match {
        case (Some(f), _) => {
          if (bamFile.isDefined) {
            logger.warn(s"Both BAM and Wisecondor count files are given for sample $name. The BAM file will be ignored")
          }
          val ln = new Ln(root)
          ln.input = f
          ln.output = outFile
          \/-(ln)
        }
        case (None, Some(bam)) => {
          val counter = new WisecondorCount(root)
          counter.inputBam = bam
          counter.output = outFile
          counter.binFile = Some(targets)
          \/-(counter)
        }
        case _ => -\/(s"Cannot find bam file or wisecondor count for sample" +
          s" $name. At least one must be given.")
      }

    }

    /* Get count file for wisecondor method */
    lazy val outputWisecondorCountFile: String \/ File = {
      outputWisecondorCountJob match {
        case \/-(ln: Ln)                 => \/-(ln.output)
        case \/-(count: WisecondorCount) => \/-(count.output)
        case -\/(error)                  => -\/(error)
      }
    }

    protected lazy val outputWisecondorGccJob: String \/ QFunction = {
      val outFile = new File(wisecondorDir, s"$name.wisecondor.gcc.bed")
      outputWisecondorCountFile map { bedFile =>
        val gcc = new WisecondorGcCorrect(root)
        gcc.inputBed = bedFile
        gcc.output = outFile
        gcc
      }
    }

    lazy val outputWisecondorGccFile: String \/ File = {
      outputWisecondorGccJob match {
        case \/-(gcc: WisecondorGcCorrect) => \/-(gcc.output)
        case -\/(error)                    => -\/(error)
      }
    }

    protected lazy val outputWisecondorSortJobs: String \/ List[QFunction] = {
      outputWisecondorGccFile map { gccFile =>
        val sort = new BedtoolsSort(root)
        sort.input = gccFile
        sort.output = new File(wisecondorDir, s"$name.wisecondor.sorted.gcc.bed")
        sort.isIntermediate = true
        val gz = new Bgzip(root)
        gz.input = List(sort.output)
        gz.output = new File(wisecondorDir, s"$name.wisecondor.sorted.gcc.bed.gz")
        val tabix = Tabix(root, gz.output)
        List(sort, gz, tabix)
      }
    }

    lazy val outputWisecondorGzFile: String \/ File = {
      outputWisecondorSortJobs match {
        case -\/(error)                      => -\/(error)
        case \/-(functions: List[QFunction]) => \/-(functions.collect { case gz: Bgzip => gz.output }.head)
      }
    }

    lazy val outputWisecondorTbiFile: String \/ File = {
      outputWisecondorSortJobs match {
        case -\/(error)                      => -\/(error)
        case \/-(functions: List[QFunction]) => \/-(functions.collect { case tbi: Tabix => tbi.outputIndex }.head)
      }
    }

    /** Function to add sample jobs */
    def addJobs(): Unit = {
      (outputWisecondorGccJob :: outputWisecondorCountJob :: outputXhmmCountJob :: Nil).foreach {
        case -\/(error)    => Logging.addError(error)
        case \/-(function) => add(function)
      }
      outputWisecondorSortJobs match {
        case -\/(error)                      => Logging.addError(error)
        case \/-(functions: List[QFunction]) => addAll(functions)
      }
    }

    /* This is necessary for compile reasons, but library does not in fact exist for this pipeline */
    def makeLibrary(id: String) = new Library(id)

    class Library(id: String) extends AbstractLibrary(id) {
      def addJobs(): Unit = {}
      def summaryFiles: Map[String, File] = Map()
      def summaryStats: Any = Map()
    }
    /** Must return files to store into summary */
    def summaryFiles: Map[String, File] = Map()

    /** Libs do not exist for this pipeline */
    override def libIds: Set[String] = Set()

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  def makeSample(sampleId: String) = new Sample(sampleId)

  def summarySettings: Map[String, Any] = Map()
  def summaryFiles: Map[String, File] = Map()

  def summaryFile: File = new File(outputDir, "tarmac.summary.json")
}

object Tarmac extends PipelineCommand