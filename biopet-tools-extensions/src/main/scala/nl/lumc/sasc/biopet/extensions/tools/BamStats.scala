package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, ToolCommandFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
  * Created by pjvanthof on 18/11/2016.
  */
class BamStats(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.bamstats.BamStats

  @Input(required = true)
  var reference: File = _

  @Input(required = true)
  var bamFile: File = _

  var outputDir: File = _

  var binSize: Option[Int] = config("bin_size")
  var threadBinSize: Option[Int] = config("thread_bin_size")

  override def defaultThreads = 3
  override def defaultCoreMemory = 5.0

  /** Creates command to execute extension */
  override def cmdLine = super.cmdLine +
    required("-b", bamFile) +
    required("-o", outputDir) +
    required("-R", reference) +
    optional("--binSize", binSize),
    optional("--threadBinSize", threadBinSize)

}
