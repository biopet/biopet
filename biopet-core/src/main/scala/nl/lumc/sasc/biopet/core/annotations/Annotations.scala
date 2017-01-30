/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core.annotations

import nl.lumc.sasc.biopet.core.{ BiopetQScript, Reference }
import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.utils.LazyCheck
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
trait AnnotationGtf extends BiopetQScript with Reference { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationGtf: File = {
    val file: File = config("annotation_gtf", freeVar = true, extraSubPath = geneAnnotationSubPath)
    inputFiles :+ InputFile(file, config("annotation_gtf_md5", freeVar = true, extraSubPath = geneAnnotationSubPath))
    file
  }
}
trait AnnotationGff extends BiopetQScript with Reference { qscript: QScript =>
  /** GFF reference file in GFF3 format */
  lazy val annotationGff: File = {
    val file: File = config("annotation_gff", freeVar = true, extraSubPath = geneAnnotationSubPath)
    inputFiles :+ InputFile(file, config("annotation_gff_md5", freeVar = true, extraSubPath = geneAnnotationSubPath))
    file
  }
}

trait AnnotationRefFlat extends BiopetQScript with Reference { qscript: QScript =>
  /** GTF reference file */
  lazy val annotationRefFlat = new LazyCheck({
    val file: File = config("annotation_refflat", freeVar = true, extraSubPath = geneAnnotationSubPath)
    inputFiles :+ InputFile(file, config("annotation_refflat_md5", freeVar = true, extraSubPath = geneAnnotationSubPath))
    file
  })
}

trait RibosomalRefFlat extends BiopetQScript with Reference { qscript: QScript =>
  /** GTF reference file */
  lazy val ribosomalRefFlat = new LazyCheck({
    val file: Option[File] = config("ribosome_refflat", freeVar = true, extraSubPath = geneAnnotationSubPath)
    file match {
      case Some(f) => inputFiles :+ InputFile(f, config("ribosome_refflat_md5", freeVar = true, extraSubPath = geneAnnotationSubPath))
      case _       =>
    }
    file
  })
}
