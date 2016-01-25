package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationRefFlat
import nl.lumc.sasc.biopet.extensions.tools.BaseCounter
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BaseCounts(val root: Configurable) extends QScript with Measurement with AnnotationRefFlat {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 1, fallback = "0")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map { case (id, file) =>
      val baseCounter = new BaseCounter(this)
      baseCounter.bamFile = file
      baseCounter.outputDir = new File(outputDir, id)
      baseCounter.prefix = id
      baseCounter.refFlat = annotationRefFlat
      add(baseCounter)
      id -> baseCounter
    }

    //TODO: merges
    //TODO: heatmaps
  }
}
