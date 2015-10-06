package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import nl.lumc.sasc.biopet.core.summary.{ SummaryQScript, Summarizable }
import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, BiopetCommandLineFunction, BiopetPipe }
import nl.lumc.sasc.biopet.extensions.{ Cat, Gzip, Sickle, Cutadapt }
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSeq
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 9/22/15.
 */
class QcCommand(val root: Configurable, val fastqc: Fastqc) extends BiopetCommandLineFunction with Summarizable {

  val flexiprep = root match {
    case f: Flexiprep => f
    case _            => throw new IllegalArgumentException("This class may only be used inside Flexiprep")
  }

  @Input(required = true)
  var input: File = _

  @Output(required = true)
  var output: File = _

  var compress = true

  var read: String = _

  override def defaultCoreMemory = 2.0
  override def defaultThreads = 3

  val seqtk = new SeqtkSeq(root)
  var clip: Option[Cutadapt] = None
  var trim: Option[Sickle] = None
  var outputCommand: BiopetCommandLineFunction = null

  def jobs = (Some(seqtk) :: clip :: trim :: Some(outputCommand) :: Nil).flatten

  def summaryFiles = Map()

  def summaryStats = Map()

  override def addToQscriptSummary(qscript: SummaryQScript, name: String): Unit = {
    clip match {
      case Some(job) => qscript.addSummarizable(job, s"clipping_$read")
      case _         =>
    }
    trim match {
      case Some(job) => qscript.addSummarizable(job, s"trimming_$read")
      case _         =>
    }
  }

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(read != null)
    deps ::= input
    outputFiles :+= output
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

    clip = if (!flexiprep.skipClip) {
      val foundAdapters = fastqc.foundAdapters.map(_.seq)
      if (foundAdapters.nonEmpty) {
        val cutadept = new Cutadapt(root)
        cutadept.fastq_input = seqtk.output
        cutadept.fastq_output = new File(output.getParentFile, input.getName + ".cutadept.fq")
        cutadept.stats_output = new File(flexiprep.outputDir, s"${flexiprep.sampleId.getOrElse("x")}-${flexiprep.libId.getOrElse("x")}.$read.clip.stats")
        if (cutadept.default_clip_mode == "3") cutadept.opt_adapter ++= foundAdapters
        else if (cutadept.default_clip_mode == "5") cutadept.opt_front ++= foundAdapters
        else if (cutadept.default_clip_mode == "both") cutadept.opt_anywhere ++= foundAdapters
        Some(cutadept)
      } else None
    } else None

    trim = if (!flexiprep.skipTrim) {
      val sickle = new Sickle(root)
      sickle.output_stats = new File(flexiprep.outputDir, s"${flexiprep.sampleId.getOrElse("x")}-${flexiprep.libId.getOrElse("x")}.$read.trim.stats")
      sickle.input_R1 = clip match {
        case Some(clip) => clip.fastq_output
        case _          => seqtk.output
      }
      sickle.output_R1 = new File(output.getParentFile, input.getName + ".sickle.fq")
      Some(sickle)
    } else None

    val outputFile = (clip, trim) match {
      case (_, Some(trim)) => trim.output_R1
      case (Some(clip), _) => clip.fastq_output
      case _               => seqtk.output
    }

    if (compress) outputCommand = {
      val gzip = new Gzip(root)
      gzip.output = output
      outputFile :<: gzip
    }
    else outputCommand = {
      val cat = new Cat(root)
      cat.input = outputFile :: Nil
      cat.output = output
      cat
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
      case (Some(clip), Some(trim)) => new BiopetFifoPipe(root, seqtk :: clip :: trim :: outputCommand :: Nil)
      case (Some(clip), _)          => new BiopetFifoPipe(root, seqtk :: clip :: outputCommand :: Nil)
      case (_, Some(trim))          => new BiopetFifoPipe(root, seqtk :: trim :: outputCommand :: Nil)
      case _                        => new BiopetFifoPipe(root, seqtk :: outputCommand :: Nil)
    }

    //val cmds = (Some(seqtk) :: clip :: trim :: Some(new Gzip(root)) :: Nil).flatten
    cmd.beforeGraph()
    cmd.commandLine
  }
}
