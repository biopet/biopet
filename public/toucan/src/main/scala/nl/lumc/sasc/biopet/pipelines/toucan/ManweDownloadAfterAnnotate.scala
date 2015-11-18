package nl.lumc.sasc.biopet.pipelines.toucan

import java.io.File

import nl.lumc.sasc.biopet.extensions.manwe.{ ManweAnnotateVcf, ManweDataSourcesDownload }
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.io.Source

/**
 * Created by ahbbollen on 9-10-15.
 */
class ManweDownloadAfterAnnotate(root: Configurable,
                                 annotate: ManweAnnotateVcf) extends ManweDataSourcesDownload(root) {

  override def beforeGraph: Unit = {
    super.beforeGraph
    require(annotate != null, "Annotate should be defined")
    this.deps :+= annotate.jobOutputFile
  }

  override def beforeCmd: Unit = {
    super.beforeCmd

    if (annotate.output.exists()) {
      val reader = Source.fromFile(annotate.output)
      this.uri = reader.getLines().toList.head.split(' ').last
      reader.close()
    } else {
      this.uri = ""
    }
  }

}
