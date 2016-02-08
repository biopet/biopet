package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 12/10/15.
 */
class MergeOtuTables(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "merge_otu_tables.py")

  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  @Input(required = true)
  var input: List[File] = Nil

  @Output(required = true)
  var outputFile: File = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(input.nonEmpty)
    require(outputFile != null)
  }

  def cmdLine = executable +
    (input match {
      case l: List[_] if l.nonEmpty => required("-i", l.mkString(","))
      case _                        => ""
    }) +
    required("-o", outputFile)
}