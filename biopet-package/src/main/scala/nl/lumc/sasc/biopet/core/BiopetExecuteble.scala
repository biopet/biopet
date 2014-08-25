package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping
import nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping
import nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline
import nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling
import nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare
import nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

object BiopetExecuteble {

  val pipelines: Map[String,PipelineCommand] = Map(
      "flexiprep" -> Flexiprep,
      "mapping" -> Mapping,
      "gentrap" -> Gentrap,
      "bam-metrics" -> BamMetrics,
      "gatk-benchmark-genotyping" -> GatkBenchmarkGenotyping,
      "gatk-genotyping" -> GatkGenotyping,
      "gatk-variantcalling" -> GatkVariantcalling,
      "gatk-pipeline" -> GatkPipeline,
      "gatk-vcf-sample-compare" -> GatkVcfSampleCompare
    )
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) println(pipelineList)
    else if (pipelines.contains(args.head)) pipelines(args.head).main(args.tail)
    else {
      println("Pipeline '" + args.head + "' does not exist")
      println(pipelineList)
    }
    
    def pipelineList: String = {
      val pipelinesArray = for ((k,v) <- pipelines) yield k
      "Available pipelines:" + pipelinesArray.mkString("\n- ", "\n- ", "\n") + "please suply a pipeline"
    }
  }
}
