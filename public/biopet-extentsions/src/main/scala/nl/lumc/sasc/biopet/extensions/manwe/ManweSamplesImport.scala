package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesImport(val root: Configurable) extends Manwe {

  /**
   * Creates sample and imports vcf and bed files immediately
   */

  @Argument(doc = "name of sample")
  var name: Option[String] = _

  @Argument(doc = "Group uris")
  var group: List[String] = Nil

  @Argument(doc = "Vcf files to upload")
  var vcfs: List[File] = Nil

  @Argument(doc = "BED files to upload")
  var beds: List[File] = Nil

  @Argument(doc = "flag for data already uploaded")
  var alreadyUploaded: Boolean = false

  @Argument(doc = "flag to mark sample as public")
  var public: Boolean = false

  @Argument(doc = "flag if sample has no coverage profile")
  var noCoverage: Boolean = false

  @Argument(doc = "Prefer genotypes derived from likelihood (PL) fields in stead of GT field")
  var preferLikelihood: Boolean = false

  @Argument(doc = "Pool size")
  var poolSize : Option[Int] = _

  def subCommand = {
    required("samples") + required("import") + required(name) +
    repeat("-g", group) + repeat("--vcf", vcfs) + repeat("--bed", beds) +
    optional("-s", poolSize) + conditional(alreadyUploaded, "-u") +
    conditional(public, "-p") + conditional(preferLikelihood, "-l") +
    conditional(noCoverage, "--no-coverage-profile")
  }





}
