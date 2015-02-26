package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

/**
 * Created by pjvan_thof on 2/26/15.
 */
class CombineVariants(val root: Configurable) extends Gatk {
  val analysisType = "CombineVariants"

  @Input(required = true)
  var inputFiles: List[File] = Nil

  @Output(required = true)
  var outputFile: File = null

  var setKey: String = null
  var rodPriorityList: List[String] = Nil
  var minimumN: Int = config("minimumN", default = 1)
  var genotypeMergeOptions: String = config("genotypeMergeOptions")

  var inputMap: Map[File, String] = Map()

  def addInput(file:File, name:String): Unit = {
    inputFiles :+= file
    inputMap += file -> name
  }

  override def beforeGraph: Unit = {
    genotypeMergeOptions match {
      case null | "UNIQUIFY" | "PRIORITIZE" | "UNSORTED" | "REQUIRE_UNIQUE" =>
      case _ => throw new IllegalArgumentException("Wrong option for genotypeMergeOptions")
    }
  }

  override def commandLine = super.commandLine +
    (for (file <- inputFiles) yield {
      inputMap.get(file) match {
        case Some(name) => required("--variant:" + name, file)
        case _ => required("--variant", file)
      }}).mkString +
    required(outputFile) +
    optional("--setKey", setKey) +
    (if (rodPriorityList.isEmpty) "" else optional("--rod_priority_list", rodPriorityList.mkString(","))) +
    optional("-genotypeMergeOptions", genotypeMergeOptions)
}
