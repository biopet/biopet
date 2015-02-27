package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 2/26/15.
 */
class CombineVariants(val root: Configurable) extends Gatk {
  val analysisType = "CombineVariants"

  @Input(doc = "", required = true)
  var inputFiles: List[File] = Nil

  @Output(doc = "", required = true)
  var outputFile: File = null

  var setKey: String = null
  var rodPriorityList: String = null
  var minimumN: Int = config("minimumN", default = 1)
  var genotypeMergeOptions: Option[String] = config("genotypeMergeOptions")

  var inputMap: Map[File, String] = Map()

  def addInput(file: File, name: String): Unit = {
    inputFiles :+= file
    inputMap += file -> name
  }

  override def beforeGraph: Unit = {
    genotypeMergeOptions match {
      case Some("UNIQUIFY") | Some("PRIORITIZE") | Some("UNSORTED") | Some("REQUIRE_UNIQUE") | None =>
      case _ => throw new IllegalArgumentException("Wrong option for genotypeMergeOptions")
    }
  }

  override def commandLine = super.commandLine +
    (for (file <- inputFiles) yield {
      inputMap.get(file) match {
        case Some(name) => required("-V:" + name, file)
        case _          => required("-V", file)
      }
    }).mkString +
    required("-o", outputFile) +
    optional("--setKey", setKey) +
    optional("--rod_priority_list", rodPriorityList) +
    optional("-genotypeMergeOptions", genotypeMergeOptions)
}
