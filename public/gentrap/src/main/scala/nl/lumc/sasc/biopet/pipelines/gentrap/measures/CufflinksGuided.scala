package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationGtf
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class CufflinksGuided(val root: Configurable) extends QScript with CufflinksMeasurement with AnnotationGtf {
  override def makeCufflinksJob(id: String, bamFile: File) = {
    val cufflinks = super.makeCufflinksJob(id, bamFile)
    cufflinks.gtfGuide = Some(annotationGtf)
    cufflinks
  }
}
