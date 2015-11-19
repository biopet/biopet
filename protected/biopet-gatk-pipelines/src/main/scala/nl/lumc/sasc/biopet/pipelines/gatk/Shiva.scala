/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad._
import nl.lumc.sasc.biopet.pipelines.shiva.{ ShivaTrait, ShivaVariantcallingTrait }
import org.broadinstitute.gatk.queue.QScript

/**
 * Shiva inplementation with GATK steps
 *
 * Created by pjvan_thof on 2/26/15.
 */
class Shiva(val root: Configurable) extends QScript with ShivaTrait {
  qscript =>
  def this() = this(null)

  /** Make variantcalling submodule, this with the gatk modes in there */
  override def makeVariantcalling(multisample: Boolean = false): ShivaVariantcallingTrait = {
    if (multisample) new ShivaVariantcalling(qscript) {
      override def namePrefix = "multisample"
      override def configName = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
    }
    else new ShivaVariantcalling(qscript) {
      override def configName = "shivavariantcalling"
    }
  }

  /** Makes a sample */
  override def makeSample(id: String) = new this.Sample(id)

  /** Class will generate sample jobs */
  class Sample(sampleId: String) extends super.Sample(sampleId) {
    /** Makes a library */
    override def makeLibrary(id: String) = new this.Library(id)

    /** Class will generate library jobs */
    class Library(libId: String) extends super.Library(libId) {

      lazy val useIndelRealigner: Boolean = config("use_indel_realigner", default = true)
      lazy val useBaseRecalibration: Boolean = {
        val c: Boolean = config("use_base_recalibration", default = true)
        val br = new BaseRecalibrator(qscript)
        if (c && br.knownSites.isEmpty)
          logger.warn("No Known site found, skipping base recalibration, file: " + inputBam)
        c && br.knownSites.nonEmpty
      }

      override def summarySettings = super.summarySettings +
        ("use_indel_realigner" -> useIndelRealigner) +
        ("use_base_recalibration" -> useBaseRecalibration)

      /** This will adds preprocess steps, gatk indel realignment and base recalibration is included here */
      override def preProcess(input: File): Option[File] = {
        if (!useIndelRealigner && !useBaseRecalibration) None
        else {
          val indelRealignFile = useIndelRealigner match {
            case true  => addIndelRealign(input, libDir, useBaseRecalibration || libraries.size > 1)
            case false => input
          }

          useBaseRecalibration match {
            case true  => Some(addBaseRecalibrator(indelRealignFile, libDir, libraries.size > 1))
            case false => Some(indelRealignFile)
          }
        }
      }
    }

    override def summarySettings = super.summarySettings + ("use_indel_realigner" -> useIndelRealigner)

    lazy val useIndelRealigner: Boolean = config("use_indel_realigner", default = true)

    /** This methods will add double preprocess steps, with GATK indel realignment */
    override protected def addDoublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input.size <= 1) super.addDoublePreProcess(input)
      else super.addDoublePreProcess(input, isIntermediate = useIndelRealigner).collect {
        case file =>
          useIndelRealigner match {
            case true  => addIndelRealign(file, sampleDir, isIntermediate = false)
            case false => file
          }
      }
    }
  }

  /** Adds indel realignment jobs */
  def addIndelRealign(inputBam: File, dir: File, isIntermediate: Boolean): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    realignerTargetCreator.isIntermediate = true
    add(realignerTargetCreator)

    val indelRealigner = IndelRealigner(this, inputBam, realignerTargetCreator.out, dir)
    indelRealigner.isIntermediate = isIntermediate
    add(indelRealigner)

    indelRealigner.o
  }

  /** Adds base recalibration jobs */
  def addBaseRecalibrator(inputBam: File, dir: File, isIntermediate: Boolean): File = {
    val baseRecalibrator = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal"))

    if (baseRecalibrator.knownSites.isEmpty) return inputBam
    add(baseRecalibrator)

    if (config("use_analyze_covariates", default = false).asBoolean) {
      val baseRecalibratorAfter = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.after"))
      baseRecalibratorAfter.BQSR = baseRecalibrator.o
      add(baseRecalibratorAfter)

      add(AnalyzeCovariates(this, baseRecalibrator.o, baseRecalibratorAfter.o, swapExt(dir, inputBam, ".bam", ".baserecal.pdf")))
    }

    val printReads = PrintReads(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.bam"))
    printReads.BQSR = baseRecalibrator.o
    printReads.isIntermediate = isIntermediate
    add(printReads)

    printReads.o
  }
}

/** This object give a default main methods for this pipeline */
object Shiva extends PipelineCommand