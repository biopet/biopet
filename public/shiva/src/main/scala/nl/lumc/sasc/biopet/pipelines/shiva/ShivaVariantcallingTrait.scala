package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Gzip
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsCall
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.tools.{ VcfStats, VcfFilter, MpileupToVcf }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.generic.Sorted

/**
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaVariantcallingTrait extends SummaryQScript with SampleLibraryTag {
  qscript =>

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  var inputBams: List[File] = Nil

  def namePrefix: String = {
    (sampleId, libId) match {
      case (Some(sampleId), Some(libId)) => sampleId + "-" + libId
      case (Some(sampleId), _)           => sampleId
      case _                             => config("name_prefix")
    }
  }

  def init: Unit = {
  }

  def finalFile = new File(outputDir, namePrefix + ".final.vcf.gz")

  def biopetScript: Unit = {
    val configCallers: Set[String] = config("variantcallers")

    for (cal <- configCallers) {
      if (!callersList.exists(_.name == cal))
        BiopetQScript.addError("variantcaller '" + cal + "' does not exist, possible to use: " + callersList.map(_.name).mkString(", "))
    }

    val callers = callersList.filter(x => configCallers.contains(x.name)).sortBy(_.prio)

    require(!inputBams.isEmpty, "No input bams found")
    require(!callers.isEmpty, "must select atleast 1 variantcaller, possible to use: " + callersList.map(_.name).mkString(", "))

    val cv = new CombineVariants(qscript)
    cv.outputFile = finalFile
    cv.setKey = "VariantCaller"
    cv.genotypeMergeOptions = Some("PRIORITIZE")
    cv.rodPriorityList = callers.map(_.name).mkString(",")
    for (caller <- callers) {
      caller.addJobs()
      cv.addInput(caller.outputFile, caller.name)

      val vcfStats = new VcfStats(qscript)
      vcfStats.input = caller.outputFile
      vcfStats.setOutputDir(new File(caller.outputDir, "vcfstats"))
      add(vcfStats)
      addSummarizable(vcfStats, namePrefix + "-vcfstats-" + caller.name)
    }
    add(cv)

    val vcfStats = new VcfStats(qscript)
    vcfStats.input = finalFile
    vcfStats.setOutputDir(new File(outputDir, "vcfstats"))
    vcfStats.infoTags :+= cv.setKey
    add(vcfStats)
    addSummarizable(vcfStats, namePrefix + "-vcfstats-final")

    addSummaryJobs
  }

  protected def callersList: List[Variantcaller] = List(new RawVcf, new Bcftools)

  trait Variantcaller {
    val name: String
    def outputDir = new File(qscript.outputDir, name)
    protected val defaultPrio: Int
    lazy val prio: Int = config("prio_" + name, default = defaultPrio)
    def addJobs()
    def outputFile: File
  }

  class Bcftools extends Variantcaller {
    val name = "bcftools"
    protected val defaultPrio = 8

    def outputFile = new File(outputDir, namePrefix + ".bcftools.vcf.gz")

    def addJobs() {
      val mp = new SamtoolsMpileup(qscript)
      mp.input = inputBams
      mp.u = true

      val bt = new BcftoolsCall(qscript)
      bt.O = "z"
      bt.v = true
      bt.c = true

      //TODO: add proper class with piping support, see also issue #114
      add(new CommandLineFunction {
        @Input
        var input = inputBams

        @Output
        var output = outputFile

        def commandLine: String = mp.cmdPipe + " | " + bt.cmdPipeInput + " > " + outputFile + " && tabix -p vcf " + outputFile
      })
    }
  }

  class RawVcf extends Variantcaller {
    val name = "raw"
    protected val defaultPrio = 999

    def outputFile = new File(outputDir, namePrefix + ".raw.vcf.gz")

    def addJobs() {
      val rawFiles = inputBams.map(bamFile => {
        val m2v = new MpileupToVcf(qscript)
        m2v.inputBam = bamFile
        m2v.output = new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.vcf")
        add(m2v)

        val vcfFilter = new VcfFilter(qscript) {
          override def configName = "vcffilter"
          override def defaults = ConfigUtils.mergeMaps(Map("min_sample_depth" -> 8,
            "min_alternate_depth" -> 2,
            "min_samples_pass" -> 1,
            "filter_ref_calls" -> true
          ), super.defaults)
        }
        vcfFilter.inputVcf = m2v.output
        vcfFilter.outputVcf = new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.filter.vcf.gz")
        add(vcfFilter)
        vcfFilter.outputVcf
      })

      val cv = new CombineVariants(qscript)
      cv.inputFiles = rawFiles
      cv.outputFile = outputFile
      cv.setKey = "null"
      cv.excludeNonVariants = true
      add(cv)
    }
  }

  def summaryFile = new File(outputDir, "ShivaVariantcalling.summary.json")

  def summarySettings = Map()

  def summaryFiles: Map[String, File] = {
    val callers: Set[String] = config("variantcallers")
    callersList.filter(x => callers.contains(x.name)).map(x => (x.name -> x.outputFile)).toMap + ("final" -> finalFile)
  }
}