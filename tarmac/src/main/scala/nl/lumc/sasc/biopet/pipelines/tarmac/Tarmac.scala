package nl.lumc.sasc.biopet.pipelines.tarmac

import java.io.File

import nl.lumc.sasc.biopet.core.{ PedigreeQscript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{ Gzip, Ln }
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.wisecondor.{ WisecondorCount, WisecondorGcCorrect, WisecondorNewRef }
import nl.lumc.sasc.biopet.extensions.xhmm.XhmmMergeGatkDepths
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

  private val targets: File = config("targets")
  def this() = this(null)

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
    val initRefMap = samples map { x => x._2 -> getReferenceSamplesForSample(x._1) }
    initRefMap.values.filter(_.isLeft).foreach {
      case -\/(message) => Logging.addError(message)
      case _            => ;
    }

    val refMap: Map[Sample, Set[Sample]] = initRefMap.filter(_._2.isRight).map { x =>
      val refSamples = x._2.getOrElse(Nil)
      val actualRefSamples = refSamples.map { s =>
        samples.getOrElse(s, Logging.addError(s"Sample $s does not exist"))
      }.filter {
        case p: Sample => true
        case _         => false
      }.map { case s: Sample => s }.toSet
      x._1 -> actualRefSamples
    }

    val wisecondorRefJobs = refMap map {
      case (sample, refSamples) =>
        val refDir = new File(new File(outputDir, sample.sampleId), "wisecondor_reference")
        createWisecondorReferenceJobs(refSamples, refDir)
    }

    val xhmmRefJobs = refMap map {
      case (sample, refSamples) =>
        val refDir = new File(new File(outputDir, sample.sampleId), "xhmm_reference")
        createXhmmReferenceJobs(sample, refSamples, refDir)
    }

    (wisecondorRefJobs.toList ::: xhmmRefJobs.toList).flatten.foreach(job => add(job))
  }

  /**
   * Get set of sample names constituting reference samples for a given sample name
   *
   * Reference samples must match the own gender, while excluding own parents (if any) and self
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

  def createXhmmReferenceJobs(sample: Sample, referenceSamples: Set[Sample], outputDirectory: File): List[QFunction] = {
    /* XHMM requires refset including self */
    val totalSet = referenceSamples + sample
    val merger = new XhmmMergeGatkDepths(this)
    merger.gatkDepthsFiles = totalSet.map(_.outputXhmmCountFile).filter(_.isRight).map(_.getOrElse("")).filter {
      case f: File => true
      case _       => false
    }.map { case f: File => f }.toList
    merger.output = new File(outputDirectory, "reference.matrix")
    List(merger)
  }

  def createWisecondorReferenceJobs(referenceSamples: Set[Sample], outputDirectory: File): List[QFunction] = {
    val gccs = referenceSamples.map { x =>
      val gcc = new WisecondorGcCorrect(this)
      x.outputWisecondorCountFile match {
        case \/-(file) => gcc.inputBed = file
        case _         => ;
      }
      gcc.output = new File(outputDirectory, s"${x.sampleId}.gcc")
      gcc
    }

    val reference = new WisecondorNewRef(this)
    reference.inputBeds = gccs.map(_.output).toList
    reference.output = new File(outputDirectory, "reference.bed")
    reference.isIntermediate = true
    val gzipRef = new Gzip(this)
    gzipRef.input = List(reference.output)
    gzipRef.output = new File(outputDirectory, "reference.bed.gz")
    gccs.toList ::: reference :: gzipRef :: Nil
  }

  class Sample(name: String) extends AbstractSample(name) {

    val inputXhmmCountFile: Option[File] = config("xhmm_count_file")
    val inputWisecondorCountFile: Option[File] = config("wisecondor_count_file")
    val bamFile: Option[File] = config("bam")

    /**
     * Create XHMM count file or create link to input count file
     * Precedence is given to existing count files.
     * Returns a disjunction where right is the file, and left is
     * a potential error message
     */
    protected lazy val outputXhmmCountJob: String \/ QFunction = {
      val outFile = new File(sampleDir + File.separator + s"$name.dcov")
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
      val outFile = new File(sampleDir + File.separator + s"$name.wisecondor.bed")
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

    /** Function to add sample jobs */
    def addJobs(): Unit = {
      (outputWisecondorCountJob :: outputXhmmCountJob :: Nil).foreach {
        case -\/(error)    => Logging.addError(error)
        case \/-(function) => add(function)
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

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  def makeSample(sampleId: String) = new Sample(sampleId)

  def summarySettings: Map[String, Any] = Map()
  def summaryFiles: Map[String, File] = Map()

  def summaryFile: File = new File(outputDir, "tarmac.summary.json")
}

object Tarmac extends PipelineCommand