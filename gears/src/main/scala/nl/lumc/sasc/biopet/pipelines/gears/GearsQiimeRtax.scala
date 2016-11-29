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
package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.{ SampleLibraryTag, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.qiime.{ SplitLibrariesFastq, AssignTaxonomy, PickRepSet, PickOtus }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/4/15.
 */
class GearsQiimeRtax(val root: Configurable) extends QScript with BiopetQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  override def fixedValues = Map(
    "assigntaxonomy" -> Map(
      "assignment_method" -> "rtax"
    )
  )

  override def defaults = Map(
    "pickotus" -> Map(
      "otu_picking_method" -> "uclust_ref",
      "suppress_new_clusters" -> true,
      "enable_rev_strand_match" -> true
    ),
    "splitlibrariesfastq" -> Map(
      "barcode_type" -> "not-barcoded"
    ),
    "assigntaxonomy" -> Map(
      "amplicon_id_regex" -> """(\S+)\s+(\S+?)""",
      "header_id_regex" -> """S+s+(S+?)""",
      "read_id_regex" -> """S+s+(S+)"""
    )
  )

  def init() = {
    require(fastqR1 != null)
  }

  def biopetScript() = {

    val slfR1 = new SplitLibrariesFastq(this)
    slfR1.input :+= fastqR1
    slfR1.outputDir = new File(outputDir, "split_libraries_fastq_R1")
    sampleId.foreach(slfR1.sampleIds :+= _)
    add(slfR1)

    lazy val slfR2 = fastqR2.map { file =>
      val j = new SplitLibrariesFastq(this)
      j.input :+= file
      j.outputDir = new File(outputDir, "split_libraries_fastq_R2")
      sampleId.foreach(j.sampleIds :+= _)
      add(j)
      j
    }

    val pickOtus = new PickOtus(this)
    pickOtus.inputFasta = slfR1.outputSeqs
    pickOtus.outputDir = new File(outputDir, "pick_otus")
    add(pickOtus)

    val pickRepSet = new PickRepSet(this)
    val repSetOutputDir = new File(outputDir, "pick_rep_set")
    pickRepSet.inputFile = pickOtus.otusTxt
    pickRepSet.fastaInput = Some(slfR1.outputSeqs)
    pickRepSet.outputFasta = Some(new File(repSetOutputDir, slfR1.outputSeqs.getName))
    pickRepSet.logFile = Some(new File(repSetOutputDir, slfR1.outputSeqs.getName
      .stripSuffix(".fasta").stripSuffix(".fa").stripSuffix(".fna") + ".log"))
    add(pickRepSet)

    val assignTaxonomy = new AssignTaxonomy(this)
    assignTaxonomy.outputDir = new File(outputDir, "assign_taxonomy")
    assignTaxonomy.jobOutputFile = new File(assignTaxonomy.outputDir, ".assign_taxonomy.out")
    assignTaxonomy.inputFasta = pickRepSet.outputFasta.get
    assignTaxonomy.read1SeqsFp = Some(slfR1.outputSeqs)
    assignTaxonomy.read2SeqsFp = slfR2.map(_.outputSeqs)
    add(assignTaxonomy)
  }
}
