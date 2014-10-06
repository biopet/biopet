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

  val pipelines: Map[String, PipelineCommand] = Map(
      "flexiprep" -> Flexiprep,
      "mapping" -> Mapping,
      "gentrap" -> Gentrap,
      "bam-metrics" -> BamMetrics,
      "gatk-benchmark-genotyping" -> GatkBenchmarkGenotyping,
      "gatk-genotyping" -> GatkGenotyping,
      "gatk-variantcalling" -> GatkVariantcalling,
      "gatk-pipeline" -> GatkPipeline,
      "gatk-variant-recalibration" -> GatkVariantRecalibration,
      "gatk-vcf-sample-compare" -> GatkVcfSampleCompare,
      "sage" -> Sage,
      "basty" -> Basty
    )

  val tools: Map[String, ToolCommand] = Map(
      "WipeReads" -> WipeReads
    )

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    def toBulletedList(m: Map[String, Any], kind: String = "", bullet: String = "-") =
      "Available %s:\n  ".format(kind) + bullet + " " +
        m.keys.toVector.sorted.mkString("\n  " + bullet + " ")

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

    args match {
      case Array("pipeline", pipelineName, pipelineArgs @ _*) =>
        if (pipelines.contains(pipelineName))
          if (pipelineArgs.isEmpty) {
            pipelines(pipelineName).main(Array("--help"))
            System.exit(1)
          }
          else {
            pipelines(pipelineName).main(pipelineArgs.toArray)
            System.exit(0)
          }
        else {
          System.err.println(s"ERROR: pipeline '$pipelineName' does not exist")
          System.err.println(pipelineUsage)
          System.exit(1)
        }
      case Array("pipeline") =>
        System.err.println(pipelineUsage)
        System.exit(1)
      case Array("tool", toolName, toolArgs @ _*) =>
        if (tools.contains(toolName)) {
          tools(toolName).main(toolArgs.toArray)
          System.exit(0)
        }
        else {
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
