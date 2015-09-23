package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, BiopetPipe }
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat, Sickle }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 9/22/15.
 */
class QcCommand(val root: Configurable, val fastqc: Fastqc) extends BiopetCommandLineFunction {

  val flexiprep = root match {
    case f: Flexiprep => f
    case _            => throw new IllegalArgumentException("This class may only be used inside Flexiprep")
  }

  @Input(required = true)
  var input: File = _

  @Output(required = true)
  var output: File = _

  val zcat = new Zcat(root)
  val seqtk = new SeqtkSeq(root)
  val cutadept = flexiprep.skipClip match {
    case false => Some(new Cutadapt(root))
    case true  => None
  }
  val sickle = flexiprep.skipTrim match {
    case false => Some(new Sickle(root))
    case true  => None
  }
  val gzip = Gzip(root)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    deps :::= fastqc.outputFiles
  }

  override def defaultCoreMemory = 2.0
  override def defaultThreads = 3

  override def beforeCmd(): Unit = {
    seqtk.Q = fastqc.encoding match {
      case null => None
      case enc if enc.contains("Sanger / Illumina 1.9") => None
      case enc if enc.contains("Illumina <1.3") => Option(64)
      case enc if enc.contains("Illumina 1.3") => Option(64)
      case enc if enc.contains("Illumina 1.5") => Option(64)
      case _ => None
    }
    if (seqtk.Q.isDefined) seqtk.V = true
  }

  def cmdLine = {
    val sanger = seqtk.Q match {
      case Some(_) => Some(seqtk)
      case _       => None
    }
    val clip = cutadept match {
      case Some(cutadept) =>
        val foundAdapters = fastqc.foundAdapters.map(_.seq)
        if (cutadept.default_clip_mode == "3") cutadept.opt_adapter ++= foundAdapters
        else if (cutadept.default_clip_mode == "5") cutadept.opt_front ++= foundAdapters
        else if (cutadept.default_clip_mode == "both") cutadept.opt_anywhere ++= foundAdapters
        if (foundAdapters.nonEmpty) Some(cutadept)
        else None
      case _ => None
    }
    val trim = sickle
    val cmds = ((if (input.getName.endsWith(".gz") || input.getName.endsWith(".gzip")) Some(zcat) else None) ::
      sanger :: clip :: trim :: Some(gzip) :: Nil).flatten
    val cmd = input :<: cmds.tail.foldLeft(cmds.head)((a, b) => a | b) > output
    cmd.beforeGraph()
    cmd.commandLine
  }
}
