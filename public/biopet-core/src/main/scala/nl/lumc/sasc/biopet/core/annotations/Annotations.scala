package nl.lumc.sasc.biopet.core.annotations

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.utils.LazyCheck
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
trait AnnotationGtf extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationGtf: File = {
    val file: File = config("annotation_gtf", freeVar = true)
    inputFiles :+ InputFile(file, config("annotation_gtf_md5", freeVar = true))
    file
  }
}
trait AnnotationGff extends BiopetQScript { qscript: QScript =>
  /** GFF reference file in GFF3 format */
  lazy val annotationGff: File = {
    val file: File = config("annotation_gff", freeVar = true)
    inputFiles :+ InputFile(file, config("annotation_gff_md5", freeVar = true))
    file
  }
}

trait AnnotationRefFlat extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationRefFlat = new LazyCheck({
    val file: File = config("annotation_refflat", freeVar = true)
    inputFiles :+ InputFile(file, config("annotation_refflat_md5", freeVar = true))
    file
  })
}

trait RibosomalRefFlat extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val ribosomalRefFlat = new LazyCheck({
    val file: Option[File] = config("ribosome_refflat", freeVar = true)
    file match {
      case Some(f) => inputFiles :+ InputFile(f, config("ribosome_refflat_md5", freeVar = true))
      case _       =>
    }
    file
  })
}
