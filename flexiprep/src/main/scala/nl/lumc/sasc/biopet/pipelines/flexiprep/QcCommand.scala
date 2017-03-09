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
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, BiopetFifoPipe, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.{ Cat, Gzip, Sickle }
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSeq
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvan_thof on 9/22/15.
 */
class QcCommand(val parent: Configurable, val fastqc: Fastqc, val read: String) extends BiopetCommandLineFunction with Summarizable {

  val flexiprep = parent match {
    case f: Flexiprep => f
    case _            => throw new IllegalArgumentException("This class may only be used inside Flexiprep")
  }

  @Input(required = true)
  var input: File = _

  @Output(required = true)
  var output: File = _

  var compress = true

  override def defaultCoreMemory = 2.0
  override def defaultThreads = 3

  val seqtk = new SeqtkSeq(parent)
  var clip: Option[Cutadapt] = if (!flexiprep.skipClip) Some(new Cutadapt(parent, fastqc)) else None
  var trim: Option[Sickle] = if (!flexiprep.skipTrim) {
    val sickle = new Sickle(parent)
    sickle.outputStats = new File(flexiprep.outputDir, s"${flexiprep.sampleId.getOrElse("x")}-${flexiprep.libId.getOrElse("x")}.$read.trim.stats")
    Some(sickle)
  } else None

  lazy val outputCommand: BiopetCommandLineFunction = if (compress) {
    val gzip = Gzip(parent)
    gzip.output = output
    gzip
  } else {
    val cat = Cat(parent)
    cat.output = output
    cat
  }

  def jobs = (Some(seqtk) :: clip :: trim :: Some(outputCommand) :: Nil).flatten

  def summaryFiles = Map()

  def summaryStats = Map()

  override def summaryDeps = trim.map(_.summaryDeps).toList.flatten ::: super.summaryDeps

  override def addToQscriptSummary(qscript: SummaryQScript): Unit = {
    clip match {
      case Some(job) => qscript.addSummarizable(job, s"clipping_$read")
      case _         =>
    }
    trim match {
      case Some(job) => qscript.addSummarizable(job, s"trimming_$read")
      case _         =>
    }
  }

  @Output
  private var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(read != null)
    deps ::= input
    outputFiles :+= output
    trim.foreach(outputFiles :+= _.outputStats)
  }

  override def beforeCmd(): Unit = {
    seqtk.input = input
    seqtk.output = new File(output.getParentFile, input.getName + ".seqtk.fq")
    seqtk.Q = fastqc.encoding match {
      case null => None
      case enc if enc.contains("Sanger / Illumina 1.9") => None
      case enc if enc.contains("Illumina <1.3") => Option(64)
      case enc if enc.contains("Illumina 1.3") => Option(64)
      case enc if enc.contains("Illumina 1.5") => Option(64)
      case _ => None
    }
    if (seqtk.Q.isDefined) seqtk.V = true
    addPipeJob(seqtk)

    clip = (clip, BiopetQScript.safeIsDone(fastqc)) match {
      case (Some(cutadapt), Some(true)) =>
        val foundAdapters: Set[String] = if (!cutadapt.ignoreFastqcAdapters) {
          fastqc.foundAdapters.map(_.seq)
        } else Set()

        if (foundAdapters.nonEmpty || cutadapt.adapter.nonEmpty || cutadapt.front.nonEmpty || cutadapt.anywhere.nonEmpty) {
          cutadapt.fastqInput = seqtk.output
          cutadapt.fastqOutput = new File(output.getParentFile, input.getName + ".cutadapt.fq")
          cutadapt.statsOutput = new File(flexiprep.outputDir, s"${flexiprep.sampleId.getOrElse("x")}-${flexiprep.libId.getOrElse("x")}.$read.clip.stats")
          if (cutadapt.defaultClipMode == "3") cutadapt.adapter ++= foundAdapters
          else if (cutadapt.defaultClipMode == "5") cutadapt.front ++= foundAdapters
          else if (cutadapt.defaultClipMode == "both") cutadapt.anywhere ++= foundAdapters
          addPipeJob(cutadapt)
          Some(cutadapt)
        } else None
      case (None, _) => None
      case (Some(c), _) =>
        c.fastqInput = seqtk.output
        c.fastqOutput = new File(output.getParentFile, input.getName + ".cutadapt.fq")
        c.statsOutput = new File(flexiprep.outputDir, s"${flexiprep.sampleId.getOrElse("x")}-${flexiprep.libId.getOrElse("x")}.$read.clip.stats")
        Some(c)
    }

    trim.foreach { t =>
      t.outputR1 = new File(output.getParentFile, input.getName + ".sickle.fq")
      t.inputR1 = clip match {
        case Some(c) => c.fastqOutput
        case _       => seqtk.output
      }
      addPipeJob(t)
    }

    val outputFile = (clip, trim) match {
      case (_, Some(t)) => t.outputR1
      case (Some(c), _) => c.fastqOutput
      case _            => seqtk.output
    }

    outputCommand match {
      case gzip: Gzip => outputFile :<: gzip
      case cat: Cat   => cat.input = outputFile :: Nil
    }

    seqtk.beforeGraph()
    clip.foreach(_.beforeGraph())
    trim.foreach(_.beforeGraph())
    outputCommand.beforeGraph()

    seqtk.beforeCmd()
    clip.foreach(_.beforeCmd())
    trim.foreach(_.beforeCmd())
    outputCommand.beforeCmd()
  }

  def cmdLine = {

    val cmd = (clip, trim) match {
      case (Some(c), Some(t)) => new BiopetFifoPipe(parent, seqtk :: c :: t :: outputCommand :: Nil)
      case (Some(c), _)       => new BiopetFifoPipe(parent, seqtk :: c :: outputCommand :: Nil)
      case (_, Some(t))       => new BiopetFifoPipe(parent, seqtk :: t :: outputCommand :: Nil)
      case _                  => new BiopetFifoPipe(parent, seqtk :: outputCommand :: Nil)
    }

    //val cmds = (Some(seqtk) :: clip :: trim :: Some(new Gzip(root)) :: Nil).flatten
    cmd.beforeGraph()
    cmd.commandLine
  }
}
