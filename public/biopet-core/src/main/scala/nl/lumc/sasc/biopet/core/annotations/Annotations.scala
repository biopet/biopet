package nl.lumc.sasc.biopet.core.annotations

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 1/12/16.
  */
trait AnnotationGtf extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationGtf: File = {
    val file: File = config("annotation_gtf")
    inputFiles :+ InputFile(file, config("annotation_gtf_md5"))
    file
  }
}

trait AnnotationBed extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationBed: File = {
    val file: File = config("annotation_bed")
    inputFiles :+ InputFile(file, config("annotation_bed_md5"))
    file
  }
}

trait AnnotationRefFlat extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationRefFlat: File = {
    val file: File = config("annotation_refflat")
    inputFiles :+ InputFile(file, config("annotation_refflat_md5"))
    file
  }
}

trait RibosomalRefFlat extends BiopetQScript { qscript: QScript =>
  /** GTF reference file */
  lazy val ribosomalRefFlat: File = {
    val file: File = config("ribosome_refflat")
    inputFiles :+ InputFile(file, config("ribosome_refflat_md5"))
    file
  }
}
