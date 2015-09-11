/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.{File, FileOutputStream, PrintWriter}

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.samtools.util.Interval
import htsjdk.variant.variantcontext.{Allele, Genotype, VariantContext}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.summary.{Summarizable, SummaryQScript}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Random

/**
 * This tool will generate statistics from a vcf file
 *
 * Created by pjvan_thof on 1/10/15.
 */
class VcfStats(val root: Configurable) extends ToolCommandFuntion with Summarizable with Reference {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var input: File = _

  @Input
  protected var index: File = null

  @Output
  protected var generalStats: File = null

  @Output
  protected var genotypeStats: File = null

  override def defaultCoreMemory = 3.0
  override def defaultThreads = 3

  protected var outputDir: File = _

  var infoTags: List[String] = Nil
  var genotypeTags: List[String] = Nil
  var allInfoTags = false
  var allGenotypeTags = false
  var reference: File = _

  override def beforeGraph(): Unit = {
    reference = referenceFasta()
    index = new File(input.getAbsolutePath + ".tbi")
  }

  /** Set output dir and a output file */
  def setOutputDir(dir: File): Unit = {
    outputDir = dir
    generalStats = new File(dir, "general.tsv")
    genotypeStats = new File(dir, "genotype-general.tsv")
    jobOutputFile = new File(dir, ".vcfstats.out")
  }

  /** Creates command to execute extension */
  override def commandLine = super.commandLine +
    required("-I", input) +
    required("-o", outputDir) +
    repeat("--infoTag", infoTags) +
    repeat("--genotypeTag", genotypeTags) +
    conditional(allInfoTags, "--allInfoTags") +
    conditional(allGenotypeTags, "--allGenotypeTags") +
    required("-R", reference)

  /** Returns general stats to the summary */
  def summaryStats: Map[String, Any] = {
    Map("info" -> (for (
      line <- Source.fromFile(generalStats).getLines().toList.tail;
      values = line.split("\t") if values.size >= 2 && !values(0).isEmpty
    ) yield values(0) -> values(1).toInt
    ).toMap)
  }

  /** return only general files to summary */
  def summaryFiles: Map[String, File] = Map(
    "general_stats" -> generalStats,
    "genotype_stats" -> genotypeStats
  )

  override def addToQscriptSummary(qscript: SummaryQScript, name: String): Unit = {
    val data = Source.fromFile(genotypeStats).getLines().map(_.split("\t")).toArray

    for (s <- 1 until data(0).size) {
      val sample = data(0)(s)
      val stats = Map("genotype" -> (for (f <- 1 until data.length) yield {
        data(f)(0) -> data(f)(s)
      }).toMap)

      val sum = new Summarizable {
        override def summaryFiles: Map[String, File] = Map()
        override def summaryStats: Map[String, Any] = stats
      }

      qscript.addSummarizable(sum, name, Some(sample))
    }
  }
}
