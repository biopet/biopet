package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.{PipelineCommand, Reference, MultiSampleQScript}
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{AddOrReplaceReadGroups, SamToFastq}
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 18/12/15.
  */
class MultisampleMapping(val root: Configurable) extends QScript
  with MultiSampleQScript
  with Reference { qscript =>
  def this() = this(null)

  def init: Unit = {
  }

  def biopetScript: Unit = {
    addSamplesJobs() // This executes jobs for all samples
  }

  def addMultiSampleJobs: Unit = {
    // this code will be executed after all code of all samples is executed
  }

  def summaryFile: File = new File(outputDir, "MultisamplePipeline.summary.json")

  def summaryFiles: Map[String, File] = Map()

  def summarySettings: Map[String, Any] = Map()

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      def summaryFiles: Map[String, File] = Map()

      def summaryStats: Map[String, Any] = Map()

      lazy val inputR1: Option[File] = MultisampleMapping.fileMustBeAbsulute(config("R1"))
      lazy val inputR2: Option[File] = MultisampleMapping.fileMustBeAbsulute(config("R2"))
      lazy val inputBam: Option[File] = MultisampleMapping.fileMustBeAbsulute(if (inputR1.isEmpty) config("bam") else None)
      lazy val bamToFastq: Boolean = config("bam_to_fastq", default = false)
      lazy val correctReadgroups: Boolean = config("correct_readgroups", default = false)

      lazy val mapping = if (inputR1.isDefined || (inputBam.isDefined && bamToFastq)) {
        val m = new Mapping(qscript)
        m.sampleId = Some(sampleId)
        m.libId = Some(libId)
        Some(m)
      } else None

      def bamFile = mapping match {
        case Some(m) => Some(m.finalBamFile)
        case _ if inputBam.isDefined => Some(new File(libDir, s"$sampleId-$libId.bam"))
        case _ => None
      }

      def addJobs: Unit = {
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
            bamMetrics.init()
            bamMetrics.biopetScript()
            addAll(bamMetrics.functions)
            addSummaryQScript(bamMetrics)
          }
        } else logger.warn(s"Sample '$sampleId' does not have any input files")
      }
    }

    def summaryFiles: Map[String, File] = Map()

    def summaryStats: Map[String, Any] = Map()

    def addJobs: Unit = {
      addPerLibJobs() // This add jobs for each library
    }
  }
}

object MultisampleMapping extends PipelineCommand {

  object MergeStrategy extends Enumeration {
    val None, MergeSam, MarkDuplicates = Value
  }

  def fileMustBeAbsulute(file: Option[File]): Option[File] = {
    if (file.forall(_.isAbsolute)) file
    else {
      Logging.addError(s"$file should be a absolute file path")
      file.map(_.getAbsoluteFile)
    }
  }

}
