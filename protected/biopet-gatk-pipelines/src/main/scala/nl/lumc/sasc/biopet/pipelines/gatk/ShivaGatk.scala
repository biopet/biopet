package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad._
import nl.lumc.sasc.biopet.pipelines.shiva.{ ShivaVariantcallingTrait, ShivaVariantcalling, ShivaTrait }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaGatk(val root: Configurable) extends QScript with ShivaTrait {
  qscript =>
  def this() = this(null)

  override def makeVariantcalling(multisample: Boolean = false): ShivaVariantcallingTrait = {
    if (multisample) new ShivaVariantcallingGatk(qscript) {
      override def namePrefix = "multisample."
      override def configName = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
    }
    else new ShivaVariantcallingGatk(qscript) {
      override def configName = "shivavariantcalling"
    }
  }

  override def makeSample(id: String) = new this.Sample(id)
  class Sample(sampleId: String) extends super.Sample(sampleId) {
    override def makeLibrary(id: String) = new this.Library(id)
    class Library(libId: String) extends super.Library(libId) {
      override def preProcess(input: File): Option[File] = {
        val useIndelRealigner: Boolean = config("use_indel_realign", default = true)
        val useBaseRecalibration: Boolean = config("use_base_recalibration", default = true)

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

    override def doublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input.size <= 1) super.doublePreProcess(input)
      else super.doublePreProcess(input, true).collect {
        case file => {
          config("use_indel_realign", default = true).asBoolean match {
            case true  => addIndelRealign(file, sampleDir, false)
            case false => file
          }
        }
      }
    }
  }

  def addIndelRealign(inputBam: File, dir: File, isIntermediate: Boolean): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    realignerTargetCreator.isIntermediate = true
    add(realignerTargetCreator)

    val indelRealigner = IndelRealigner(this, inputBam, realignerTargetCreator.out, dir)
    indelRealigner.isIntermediate = isIntermediate
    add(indelRealigner)

    return indelRealigner.o
  }

  def addBaseRecalibrator(inputBam: File, dir: File, isIntermediate: Boolean): File = {
    val baseRecalibrator = BaseRecalibrator(this, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal"))

    if (baseRecalibrator.knownSites.isEmpty) {
      logger.warn("No Known site found, skipping base recalibration, file: " + inputBam)
      return inputBam
    }
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

    return printReads.o
  }
}

object ShivaGatk extends PipelineCommand