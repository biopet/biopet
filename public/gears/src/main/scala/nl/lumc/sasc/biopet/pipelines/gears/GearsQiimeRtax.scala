package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.extensions.qiime.{AssignTaxonomy, PickRepSet, PickOtus}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/4/15.
 */
class GearsQiimeRtax(val root: Configurable) extends QScript with BiopetQScript {

  var fastaR1: File = _

  var fastaR2: Option[File] = None

  override def fixedValues = Map(
    "assigntaxonomy" -> Map(
      "assignment_method" -> "rtax"
    )
  )

  override def defaults = Map(
    "pickotus" -> Map(
      "otu_picking_method" -> "uclust_ref",
      "suppress_new_clusters" -> true
      )
    )

  def init() = {
    require(fastaR1 != null)
  }

  def biopetScript() = {
    val pickOtus = new PickOtus(this)
    pickOtus.inputFasta = fastaR1
    pickOtus.outputDir = new File(outputDir, "pick_otus")
    add(pickOtus)

    val pickRepSet = new PickRepSet(this)
    pickRepSet.outputDir = new File(outputDir, "pick_rep_set")
    pickRepSet.inputFile = pickOtus.otusTxt
    pickRepSet.outputFasta = Some(new File(pickRepSet.outputDir, fastaR1.getName))
    pickRepSet.logFile = Some(new File(pickRepSet.outputDir, fastaR1.getName
      .stripSuffix(".fasta").stripSuffix(".fa").stripSuffix(".fna") + ".log"))
    add(pickRepSet)

    val assignTaxonomy = new AssignTaxonomy(this)
    assignTaxonomy.outputDir = new File(outputDir, "assign_taxonomy")
    assignTaxonomy.jobOutputFile = new File(assignTaxonomy.outputDir, ".assign_taxonomy.out")
    assignTaxonomy.inputFasta = pickRepSet.outputFasta.get
    assignTaxonomy.read_1_seqs_fp = Some(fastaR1)
    assignTaxonomy.read_2_seqs_fp = fastaR2
    add(assignTaxonomy)
  }
}
