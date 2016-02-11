/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.extensions.gatk.broad._
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
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
  override def makeVariantcalling(multisample: Boolean = false) = {
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

      override def preProcessBam = if (useIndelRealigner && useBaseRecalibration)
        bamFile.map(swapExt(libDir, _, ".bam", ".realign.baserecal.bam"))
      else if (useIndelRealigner) bamFile.map(swapExt(libDir, _, ".bam", ".realign.bam"))
      else if (useBaseRecalibration) bamFile.map(swapExt(libDir, _, ".bam", ".baserecal.bam"))
      else bamFile

      override def addJobs(): Unit = {
        super.addJobs()
        if (useIndelRealigner && useBaseRecalibration) {
          val file = addIndelRealign(bamFile.get, libDir, isIntermediate = true)
          addBaseRecalibrator(file, libDir, libraries.size > 1)
        } else if (useIndelRealigner) {
          addIndelRealign(bamFile.get, libDir, libraries.size > 1)
        } else if (useBaseRecalibration) {
          addBaseRecalibrator(bamFile.get, libDir, libraries.size > 1)
        }
      }
    }

    override def keepMergedFiles: Boolean = config("keep_merged_files", default = !useIndelRealigner)

    override def summarySettings = super.summarySettings + ("use_indel_realigner" -> useIndelRealigner)

    lazy val useIndelRealigner: Boolean = config("use_indel_realigner", default = true)

    override def preProcessBam = if (useIndelRealigner && libraries.values.flatMap(_.preProcessBam).size > 1) {
      bamFile.map(swapExt(sampleDir, _, ".bam", ".realign.bam"))
    } else bamFile

    override def addJobs(): Unit = {
      super.addJobs()

      if (useIndelRealigner && libraries.values.flatMap(_.preProcessBam).size > 1) {
        addIndelRealign(bamFile.get, sampleDir, false)
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