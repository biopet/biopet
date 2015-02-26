package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, SamToFastq, MarkDuplicates }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaTrait extends MultiSampleQScript with SummaryQScript {
  qscript =>

  def init: Unit = {

  }

  def biopetScript: Unit = {
    addSamplesJobs()

    addSummaryJobs
  }

  def makeVariantcalling(multisample: Boolean = false): ShivaVariantcallingTrait = {
    if (multisample) new ShivaVariantcalling(qscript) {
      override def namePrefix = "multisample."
      override def configName = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
    }
    else new ShivaVariantcalling(qscript) {
      override def configName = "shivavariantcalling"
    }
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
          case (false, true) => config("bam_to_fastq", default = false).asBoolean match {
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

              val readGroupOke = readGroups.forall(readGroup => {
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
                val newIndex: File = new File(libDir, oldBamFile.getName.stripSuffix(".bam") + ".bai")
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

        if (config("library_variantcalling", default = false).asBoolean && (bamFile.isDefined || preProcessBam.isDefined)) {
          val vc = makeVariantcalling(multisample = false)
          vc.sampleId = Some(libId)
          vc.libId = Some(sampleId)
          vc.outputDir = new File(libDir, "variantcalling")
          if (preProcessBam.isDefined) vc.inputBams = preProcessBam.get :: Nil
          else vc.inputBams = bamFile.get :: Nil
          vc.init
          vc.biopetScript
          addAll(vc.functions)
          addSummaryQScript(vc)
        }
      }
    }

    def doublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input == Nil) None
      else if (input.tail == Nil) {
        val bamFile = new File(sampleDir, input.head.getName)
        val oldIndex: File = new File(input.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex: File = new File(sampleDir, input.head.getName.stripSuffix(".bam") + ".bai")
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
        md.isIntermediate = isIntermediate
        add(md)
        addSummarizable(md, "mark_duplicates", Some(sampleId))
        Some(md.output)
      }
    }

    lazy val preProcessBam: Option[File] = doublePreProcess(libraries.map(lib => {
      (lib._2.bamFile, lib._2.preProcessBam) match {
        case (_, Some(file)) => Some(file)
        case (Some(file), _) => Some(file)
        case _               => None
      }
    }).flatten.toList)

    def addJobs(): Unit = {
      addPerLibJobs()

      if (preProcessBam.isDefined) {
        val bamMetrics = new BamMetrics(root)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = preProcessBam.get
        bamMetrics.outputDir = outputDir
        bamMetrics.init
        bamMetrics.biopetScript
        addAll(bamMetrics.functions)
        addSummaryQScript(bamMetrics)

        if (config("single_sample_variantcalling", default = false).asBoolean) {
          val vc = makeVariantcalling(multisample = false)
          vc.sampleId = Some(sampleId)
          vc.outputDir = new File(sampleDir, "variantcalling")
          vc.inputBams = preProcessBam.get :: Nil
          vc.init
          vc.biopetScript
          addAll(vc.functions)
          addSummaryQScript(vc)
        }
      }
    }
  }

  def addMultiSampleJobs(): Unit = {
    if (config("multisample_sample_variantcalling", default = true).asBoolean) {
      val vc = makeVariantcalling(multisample = true)
      vc.outputDir = new File(outputDir, "variantcalling")
      vc.inputBams = samples.map(_._2.preProcessBam).flatten.toList
      vc.init
      vc.biopetScript
      addAll(vc.functions)
      addSummaryQScript(vc)
    }
  }

  def summaryFile = new File(outputDir, "Shiva.summary.json")

  def summarySettings = Map()

  def summaryFiles = Map()
}
