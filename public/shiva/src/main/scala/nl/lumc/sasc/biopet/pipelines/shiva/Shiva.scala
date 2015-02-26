package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{MultiSampleQScript, PipelineCommand}
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{AddOrReplaceReadGroups, SamToFastq, MarkDuplicates}
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 2/24/15.
 */
class Shiva(val root: Configurable) extends QScript with MultiSampleQScript with SummaryQScript {
  qscript =>
  def this() = this(null)

  def init: Unit = {

  }

  def biopetScript: Unit = {
    addSamplesJobs()

    addSummaryJobs
  }

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def makeLibrary(id: String) = new Library(id)

    class Library(libId: String) extends AbstractLibrary(libId) {

      def preProcess(input: File): Option[File] = None

      def makeMapping = {
        val mapping = new Mapping(qscript)
        mapping.sampleId = Some(sampleId)
        mapping.libId = Some(libId)
        mapping.outputDir = libDir
        (Some(mapping), Some(mapping.finalBamFile), preProcess(mapping.finalBamFile))
      }

      lazy val (mapping, bamFile, preProcessBam): (Option[Mapping], Option[File], Option[File]) =
        (config.contains("R1"), config.contains("bam")) match {
          case (true, _) => makeMapping // Default starting from fastq files
          case (false, true) => // Starting from bam file
            config("bam_to_fastq", default = false).asBoolean match {
              case true => makeMapping // bam file will be converted to fastq
              case false => {
                val file = new File(libDir, sampleId + "-" + libId + ".final.bam")
                (None, Some(file), preProcess(file))
              }
            }
          case _ => (None, None, None)
        }

      def addJobs(): Unit = {
        (config.contains("R1"), config.contains("bam")) match {
          case (true, _) => mapping.foreach(mapping => {
            mapping.input_R1 = config("R1")
            mapping.input_R2 = config("R2")
          })
          case (false, true) =>  config("bam_to_fastq", default = false).asBoolean match {
            case true => {
              val samToFastq = SamToFastq(qscript, config("bam"),
                new File(libDir, sampleId + "-" + libId + ".R1.fastq"),
                new File(libDir, sampleId + "-" + libId + ".R2.fastq"))
              samToFastq.isIntermediate = true
              qscript.add(samToFastq)
              mapping.foreach(mapping => {
                mapping.input_R1 = samToFastq.fastqR1
                mapping.input_R2 = Some(samToFastq.fastqR2)
              })
            }
            case false => {
              val inputSam = SamReaderFactory.makeDefault.open(config("bam"))
              val readGroups = inputSam.getFileHeader.getReadGroups

              val readGroupOke = readGroups.forall( readGroup => {
                if (readGroup.getSample != sampleId) logger.warn("Sample ID readgroup in bam file is not the same")
                if (readGroup.getLibrary != libId) logger.warn("Library ID readgroup in bam file is not the same")
                readGroup.getSample == sampleId && readGroup.getLibrary == libId
              })
              inputSam.close

              if (!readGroupOke) {
                if (config("correct_readgroups", default = false).asBoolean) {
                  logger.info("Correcting readgroups, file:" + config("bam"))
                  val aorrg = AddOrReplaceReadGroups(qscript, config("bam"), bamFile.get)
                  aorrg.RGID = sampleId + "-" + libId
                  aorrg.RGLB = libId
                  aorrg.RGSM = sampleId
                  aorrg.isIntermediate = true
                  qscript.add(aorrg)
                } else throw new IllegalStateException("Sample readgroup and/or library of input bamfile is not correct, file: " + bamFile +
                  "\nPlease note that it is possible to set 'correct_readgroups' to true in the config to automatic fix this")
              } else {
                val oldBamFile: File = config("bam")
                val oldIndex: File = new File(oldBamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
                val newIndex: File = swapExt(libDir, bamFile.get, ".bam", ".bai")
                val baiLn = Ln(qscript, oldIndex, newIndex)
                add(baiLn)

                val bamLn = Ln(qscript, oldBamFile, bamFile.get)
                bamLn.deps :+= baiLn.out
                add(bamLn)
              }

            }
          }
          case _ => logger.warn("Sample: " + sampleId + "  Library: " + libId + ", no reads found")
        }

        mapping.foreach(mapping => {
          mapping.init
          mapping.biopetScript
          addAll(mapping.functions) // Add functions of mapping to curent function pool
          addSummaryQScript(mapping)
        })
      }
    }

    def doublePreProcess(input: List[File]): Option[File] = {
      if (input == Nil) None
      else if (input.tail == Nil) {
        val bamFile = new File(sampleDir, input.head.getName)
        val oldIndex: File = new File(input.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex: File = swapExt(sampleDir, bamFile, ".bam", ".bai")
        val baiLn = Ln(qscript, oldIndex, newIndex)
        add(baiLn)

        val bamLn = Ln(qscript, input.head, bamFile)
        bamLn.deps :+= baiLn.out
        add(bamLn)
        Some(bamFile)
      } else {
        val md = new MarkDuplicates(qscript)
        md.input = input
        md.output = new File(sampleDir, sampleId + ".dedup.bam")
        md.outputMetrics = new File(sampleDir, sampleId + ".dedup.bam")
        Some(md.output)
      }
    }

    lazy val preProcessBam: Option[File] = doublePreProcess(libraries.map(_._2.preProcessBam).filter(_.isDefined).map(_.get).toList)

    def addJobs(): Unit = {
      addPerLibJobs()

      //TODO: Singlesample variantcalling
    }
  }

  def addMultiSampleJobs(): Unit = {
    //TODO: Mutisample variantcalling
  }

  def summaryFile = new File(outputDir, "Shiva.summary.json")

  def summarySettings = Map()

  def summaryFiles = Map()
}

object Shiva extends PipelineCommand