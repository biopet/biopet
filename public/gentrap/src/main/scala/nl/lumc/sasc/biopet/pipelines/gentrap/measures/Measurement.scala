package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 1/12/16.
  */
trait Measurement extends SummaryQScript with Reference { qscript : QScript =>
  private var bamFiles: Map[String, File] = Map()

  def addBamfile(id: String, file: File): Unit = {
    require(!bamFiles.contains(id), s"'$id' already exist")
    bamFiles += id -> file
  }
}
