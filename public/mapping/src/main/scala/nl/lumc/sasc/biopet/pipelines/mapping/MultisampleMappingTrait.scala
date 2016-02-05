package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference, MultiSampleQScript }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{ MarkDuplicates, MergeSamFiles, AddOrReplaceReadGroups, SamToFastq }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import MultisampleMapping.MergeStrategy

import scala.collection.JavaConversions._

/**
 * Created by pjvanthof on 18/12/15.
 */
trait MultisampleMappingTrait extends MultiSampleQScript
  with Reference { qscript: QScript =>

  def mergeStrategy: MergeStrategy.Value = {
    val value: String = config("merge_strategy", default = "preprocessmarkduplicates")
    MergeStrategy.values.find(_.toString.toLowerCase == value) match {
      case Some(v) => v
      case _       => throw new IllegalArgumentException(s"merge_strategy '$value' does not exist")
    }
  }

  def init(): Unit = {
  }

  def biopetScript(): Unit = {
    addSamplesJobs()
    addSummaryJobs()
  }

  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new MultisampleMappingReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryFile = summaryFile
    Some(report)
  }

  def addMultiSampleJobs(): Unit = {
    // this code will be executed after all code of all samples is executed
  }

  def summaryFiles: Map[String, File] = Map("referenceFasta" -> referenceFasta())

  def summarySettings: Map[String, Any] = Map(
    "reference" -> referenceSummary,
    "merge_strategy" -> mergeStrategy.toString)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      def summaryFiles: Map[String, File] = (inputR1.map("input_R1" -> _) :: inputR2.map("input_R2" -> _) ::
        inputBam.map("input_bam" -> _) :: bamFile.map("output_bam" -> _) ::
        preProcessBam.map("output_bam_preprocess" -> _) :: Nil).flatten.toMap

      def summaryStats: Map[String, Any] = Map()

      lazy val inputR1: Option[File] = MultisampleMapping.fileMustBeAbsolute(config("R1"))
      lazy val inputR2: Option[File] = MultisampleMapping.fileMustBeAbsolute(config("R2"))
      lazy val inputBam: Option[File] = MultisampleMapping.fileMustBeAbsolute(if (inputR1.isEmpty) config("bam") else None)
      lazy val bamToFastq: Boolean = config("bam_to_fastq", default = false)
      lazy val correctReadgroups: Boolean = config("correct_readgroups", default = false)

      lazy val mapping = if (inputR1.isDefined || (inputBam.isDefined && bamToFastq)) {
        val m = new Mapping(qscript)
        m.sampleId = Some(sampleId)
        m.libId = Some(libId)
        m.outputDir = libDir
        Some(m)
      } else None

      def bamFile = mapping match {
        case Some(m)                 => Some(m.finalBamFile)
        case _ if inputBam.isDefined => Some(new File(libDir, s"$sampleId-$libId.bam"))
        case _                       => None
      }

      def preProcessBam = bamFile

      def addJobs(): Unit = {
        inputR1.foreach(inputFiles :+= new InputFile(_, config("R1_md5")))
        inputR2.foreach(inputFiles :+= new InputFile(_, config("R2_md5")))
        inputBam.foreach(inputFiles :+= new InputFile(_, config("bam_md5")))

        if (inputR1.isDefined) {
          mapping.foreach { m =>
            m.input_R1 = inputR1.get
            m.input_R2 = inputR2
            add(m)
          }
        } else if (inputBam.isDefined) {
          if (bamToFastq) {
            val samToFastq = SamToFastq(qscript, inputBam.get,
              new File(libDir, sampleId + "-" + libId + ".R1.fq.gz"),
              new File(libDir, sampleId + "-" + libId + ".R2.fq.gz"))
            samToFastq.isIntermediate = true
            qscript.add(samToFastq)
            mapping.foreach(m => {
              m.input_R1 = samToFastq.fastqR1
              m.input_R2 = Some(samToFastq.fastqR2)
              add(m)
            })
          } else {
            val inputSam = SamReaderFactory.makeDefault.open(inputBam.get)
            val readGroups = inputSam.getFileHeader.getReadGroups

            val readGroupOke = readGroups.forall(readGroup => {
              if (readGroup.getSample != sampleId) logger.warn("Sample ID readgroup in bam file is not the same")
              if (readGroup.getLibrary != libId) logger.warn("Library ID readgroup in bam file is not the same")
              readGroup.getSample == sampleId && readGroup.getLibrary == libId
            })
            inputSam.close()

            if (!readGroupOke) {
              if (correctReadgroups) {
                logger.info("Correcting readgroups, file:" + inputBam.get)
                val aorrg = AddOrReplaceReadGroups(qscript, inputBam.get, bamFile.get)
                aorrg.RGID = s"$sampleId-$libId"
                aorrg.RGLB = libId
                aorrg.RGSM = sampleId
                aorrg.RGPL = "unknown"
                aorrg.RGPU = "na"
                aorrg.isIntermediate = true
                qscript.add(aorrg)
              } else throw new IllegalStateException("Sample readgroup and/or library of input bamfile is not correct, file: " + bamFile +
                "\nPlease note that it is possible to set 'correct_readgroups' to true in the config to automatic fix this")
            } else {
              val oldBamFile: File = inputBam.get
              val oldIndex: File = new File(oldBamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
              val newIndex: File = new File(libDir, bamFile.get.getName.stripSuffix(".bam") + ".bai")
              val baiLn = Ln(qscript, oldIndex, newIndex)
              add(baiLn)

              val bamLn = Ln(qscript, oldBamFile, bamFile.get)
              bamLn.deps :+= baiLn.output
              add(bamLn)
            }

            val bamMetrics = new BamMetrics(qscript)
            bamMetrics.sampleId = Some(sampleId)
            bamMetrics.libId = Some(libId)
            bamMetrics.inputBam = bamFile.get
            bamMetrics.outputDir = new File(libDir, "metrics")
            add(bamMetrics)

            if (config("execute_bam2wig", default = true)) add(Bam2Wig(qscript, bamFile.get))
          }
        } else logger.warn(s"Sample '$sampleId' does not have any input files")
      }
    }

    def summaryFiles: Map[String, File] = (bamFile.map("output_bam" -> _) ::
      preProcessBam.map("output_bam_preprocess" -> _) :: Nil).flatten.toMap

    def summaryStats: Map[String, Any] = Map()

    def bamFile = if (libraries.flatMap(_._2.bamFile).nonEmpty &&
      mergeStrategy != MultisampleMapping.MergeStrategy.None)
      Some(new File(sampleDir, s"$sampleId.bam"))
    else None

    def preProcessBam = bamFile

    def keepMergedFiles: Boolean = config("keep_merged_files", default = true)

    def addJobs(): Unit = {
      addPerLibJobs() // This add jobs for each library

      mergeStrategy match {
        case MergeStrategy.None =>
        case (MergeStrategy.MergeSam | MergeStrategy.MarkDuplicates) if libraries.flatMap(_._2.bamFile).size == 1 =>
          add(Ln.linkBamFile(qscript, libraries.flatMap(_._2.bamFile).head, bamFile.get): _*)
        case (MergeStrategy.PreProcessMergeSam | MergeStrategy.PreProcessMarkDuplicates) if libraries.flatMap(_._2.preProcessBam).size == 1 =>
          add(Ln.linkBamFile(qscript, libraries.flatMap(_._2.preProcessBam).head, bamFile.get): _*)
        case MergeStrategy.MergeSam =>
          add(MergeSamFiles(qscript, libraries.flatMap(_._2.bamFile).toList, bamFile.get, isIntermediate = !keepMergedFiles))
        case MergeStrategy.PreProcessMergeSam =>
          add(MergeSamFiles(qscript, libraries.flatMap(_._2.preProcessBam).toList, bamFile.get, isIntermediate = !keepMergedFiles))
        case MergeStrategy.MarkDuplicates =>
          add(MarkDuplicates(qscript, libraries.flatMap(_._2.bamFile).toList, bamFile.get, isIntermediate = !keepMergedFiles))
        case MergeStrategy.PreProcessMarkDuplicates =>
          add(MarkDuplicates(qscript, libraries.flatMap(_._2.preProcessBam).toList, bamFile.get, isIntermediate = !keepMergedFiles))
        case _ => throw new IllegalStateException("This should not be possible, unimplemented MergeStrategy?")
      }

      if (mergeStrategy != MergeStrategy.None && libraries.flatMap(_._2.bamFile).nonEmpty) {
        val bamMetrics = new BamMetrics(qscript)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = preProcessBam.get
        bamMetrics.outputDir = new File(sampleDir, "metrics")
        add(bamMetrics)

        if (config("execute_bam2wig", default = true)) add(Bam2Wig(qscript, preProcessBam.get))
      }
    }
  }
}

class MultisampleMapping(val root: Configurable) extends QScript with MultisampleMappingTrait {
  def this() = this(null)

  def summaryFile: File = new File(outputDir, "MultisamplePipeline.summary.json")
}

object MultisampleMapping extends PipelineCommand {

  object MergeStrategy extends Enumeration {
    val None, MergeSam, MarkDuplicates, PreProcessMergeSam, PreProcessMarkDuplicates = Value
  }

  def fileMustBeAbsolute(file: Option[File]): Option[File] = {
    if (file.forall(_.isAbsolute)) file
    else {
      Logging.addError(s"$file should be a absolute file path")
      file.map(_.getAbsoluteFile)
    }
  }

}
