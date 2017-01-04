package nl.lumc.sasc.biopet.pipelines.gentrap.template

import java.io.File

import nl.lumc.sasc.biopet.core.TemplateTool
import nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap.{ExpMeasures, StrandProtocol}
import nl.lumc.sasc.biopet.pipelines.mapping.template.MultiSampleMapping
import nl.lumc.sasc.biopet.pipelines.shiva.template.Shiva
import nl.lumc.sasc.biopet.utils._

/**
 * Created by pjvanthof on 17/12/2016.
 */
object Gentrap extends TemplateTool {

  override lazy val sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    val aligner = Question.string("Aligner", posibleValues = MultiSampleMapping.possibleAligners,
      default = Some("gsnap"))

    val mappingConfig = MultiSampleMapping.pipelineMap(map ++ Map("aligner" -> aligner), expert)

    val expressionMeasures = Question.list("ExpressionMeasures",
      posibleValues = ExpMeasures.values.map(x => unCamelize(x.toString)).toList)

    val strandProtocol = Question.list("StrandProtocol",
      posibleValues = StrandProtocol.values.map(x => unCamelize(x.toString)).toList)

    val annotationRefFlat = Question.string("annotationRefFlat",
      validation = List(TemplateTool.isAbsolutePath, TemplateTool.mustExist))

    val annotationGtf = if (expressionMeasures.contains(unCamelize(ExpMeasures.FragmentsPerGene.toString)) ||
      expressionMeasures.exists(_.startsWith("cufflinks")))
      Some(Question.string("annotationGtf",
        validation = List(TemplateTool.isAbsolutePath, TemplateTool.mustExist)))
    else None

    mappingConfig ++ annotationGtf.map("annotation_gtf" -> _) ++ Map(
      "expression_measures" -> expressionMeasures,
      "strand_protocol" -> strandProtocol,
      "annotation_refflat" -> annotationRefFlat
    ) ++ (if (Question.boolean("Call variants")) {
      val variantCallers = Question.list("Variantcallers", posibleValues = Shiva.possibleVariantcallers,
        default = Some(List("varscan_cns_singlesample")))

      Map("call_variants" -> true, "variantcallers" -> variantCallers)
    } else Map("call_variants" -> false))
  }
}
