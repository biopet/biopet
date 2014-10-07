package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.core.apps.WipeReads
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.basty.Basty
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping
import nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping
import nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline
import nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration
import nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling
import nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare
import nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.pipelines.sage.Sage

object BiopetExecutable {

  val pipelines: List[MainCommand] = List(
    Flexiprep,
    Mapping,
    Gentrap,
    BamMetrics,
    GatkBenchmarkGenotyping,
    GatkGenotyping,
    GatkVariantcalling,
    GatkPipeline,
    GatkVariantRecalibration,
    GatkVcfSampleCompare,
    Sage,
    Basty)

  val tools: List[MainCommand] = List(
    WipeReads)

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    def toBulletedList(m: List[MainCommand], kind: String = "", bullet: String = "-") =
      "Available %s:\n  ".format(kind) + bullet + " " + m.map(x => x.name).sorted.mkString("\n  " + bullet + " ")

    lazy val pipelineList: String = toBulletedList(pipelines, "pipelines")

    lazy val toolList: String = toBulletedList(tools, "tools")

    lazy val addendum: String =
      """Questions or comments? Email sasc@lumc.nl or check out the project page at https://git.lumc.nl/biopet/biopet.git""".stripMargin

    lazy val baseUsage: String =
      """
        |Usage: java -jar BiopetFramework.jar {pipeline,tool} {pipeline/tool name} {pipeline/tool-specific options}
        |
        |%s
        |
        |%s
      """.stripMargin.format("%s", addendum)

    lazy val mainUsage: String =
      baseUsage.format(pipelineList + "\n\n" + toolList)

    lazy val pipelineUsage: String = baseUsage
      .replaceFirst("""\{pipeline,tool\}""", "pipeline")
      .replace("""pipeline/tool""", "pipeline")
      .format(pipelineList)

    lazy val toolUsage: String = baseUsage
      .replaceFirst("""\{pipeline,tool\}""", "tool")
      .replace("""pipeline/tool""", "tool")
      .format(toolList)

    if (args.isEmpty) {
      System.err.println(mainUsage)
      System.exit(1)
    }

    def retrieveCommand(q: String, cl: List[MainCommand]): Option[MainCommand] = {
      for (mc <- cl) {
        if (q == mc.name.toLowerCase)
          return Some(mc)
      }
      None
    }

    args match {
      case Array("pipeline", pipelineName, pipelineArgs @ _*) =>
        retrieveCommand(pipelineName.toLowerCase, pipelines) match {
          case Some(pipeline)  =>
            pipeline.main(pipelineArgs.toArray)
            System.exit(0)
          case None            =>
            System.err.println(s"ERROR: pipeline '$pipelineName' does not exist")
            System.err.println(pipelineUsage)
            System.exit(1)
        }
      case Array("pipeline") =>
        System.err.println(pipelineUsage)
        System.exit(1)
      case Array("tool", toolName, toolArgs @ _*) =>
        retrieveCommand(toolName.toLowerCase, tools) match {
          case Some(tool)  =>
            tool.main(toolArgs.toArray)
            System.exit(0)
          case None            =>
            System.err.println(s"ERROR: tool '$toolName' does not exist")
            System.err.println(toolUsage)
            System.exit(1)
        }
      case Array("tool") =>
        System.err.println(toolUsage)
        System.exit(1)
      case _ =>
        println(mainUsage)
        System.exit(1)
    }
  }
}
