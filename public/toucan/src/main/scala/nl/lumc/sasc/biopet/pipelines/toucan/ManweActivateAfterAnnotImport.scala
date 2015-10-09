package nl.lumc.sasc.biopet.pipelines.toucan

import nl.lumc.sasc.biopet.extensions.manwe.{ManweAnnotateVcf, ManweSamplesActivate, ManweSamplesImport}
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.io.Source

/**
 * Created by ahbbollen on 9-10-15.
 * Wrapper for manwe activate after importing and annotating
 */
class ManweActivateAfterAnnotImport(root: Configurable,
                                     annotate: ManweAnnotateVcf,
                                     imported: ManweSamplesImport) extends ManweSamplesActivate(root) {

  override def beforeGraph: Unit = {
    super.beforeGraph
    require(annotate != null, "Annotate should be defined")
    require(imported != null, "Imported should be defined")
    this.deps :+= annotate.jobOutputFile
    this.deps :+= imported.jobOutputFile
  }

  override def beforeCmd: Unit = {
    super.beforeCmd

    val reader = Source.fromFile(imported.output)
    this.uri = reader.getLines().toList.head
    reader.close()
  }

}
