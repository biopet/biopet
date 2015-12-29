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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.File

import nl.lumc.sasc.biopet.extensions.{ Cufflinks, Ln }

/** General trait for containing cufflinks results */
trait CufflinksProducer {

  import Gentrap.ExpMeasures._
  import Gentrap.StrandProtocol._
  import Gentrap._

  //TODO: move vars that are used in gentrep
  protected def sampleDir: File
  protected def sampleId: String
  protected def pipeline: Gentrap
  protected def alnFile: File

  /** Valid cufflink measure types */
  protected val cufflinksMeasures = Set(CufflinksStrict, CufflinksGuided, CufflinksBlind)

  /** Cufflink's terms for strand specificity */
  lazy val strandedness: String = {
    //require(pipeline.config.contains("strand_protocol"))
    pipeline.strandProtocol match {
      case NonSpecific => "fr-unstranded"
      case Dutp        => "fr-firststrand"
      case otherwise   => throw new IllegalStateException("Unexpected strand type for cufflinks: " + otherwise.toString)
    }
  }

  /** Case class for containing cufflinks + its output symlink jobs */
  protected case class CufflinksJobSet(cuffType: ExpMeasures.Value) {

    require(cufflinksMeasures.contains(cuffType),
      "Cufflinks measurement type is either " + cufflinksMeasures.mkString(", ") + s"; not $cuffType")

    /** Base name for output file extensions and config path */
    lazy val name: String = cuffType match {
      case CufflinksStrict => "cufflinks_strict"
      case CufflinksGuided => "cufflinks_guided"
      case CufflinksBlind  => "cufflinks_blind"
      case otherwise       => throw new IllegalStateException("Unexpected cufflinks type: " + otherwise.toString)
    }

    /** Container for all jobs in this job set */
    def jobs = Seq(cufflinksJob, geneFpkmJob, isoformFpkmJob)

    /** The main cufflinks job */
    lazy val cufflinksJob: Cufflinks = {
      val job = new Cufflinks(pipeline) {
        override def configName = "cufflinks"
        override def configPath: List[String] = super.configPath ::: name :: Nil
      }
      job.input = alnFile
      job.library_type = Option(strandedness)
      job.output_dir = new File(sampleDir, name)
      job.GTF = cuffType match {
        case CufflinksStrict => pipeline.annotationGtf
        case otherwise       => None
      }
      job.GTF_guide = cuffType match {
        case CufflinksGuided => pipeline.annotationGtf
        case otherwise       => None
      }
      job
    }

    /** Job for symlinking gene FPKM results so that it contains a standard filename (with the sample ID) */
    lazy val geneFpkmJob: Ln = {
      val job = new Ln(pipeline)
      job.input = cufflinksJob.outputGenesFpkm
      job.output = new File(cufflinksJob.output_dir, s"$sampleId.genes_fpkm_$name")
      job
    }

    /** Job for symlinking isoforms FPKM results so that it contains a standard filename (with the sample ID) */
    lazy val isoformFpkmJob: Ln = {
      val job = new Ln(pipeline)
      job.input = cufflinksJob.outputIsoformsFpkm
      job.output = new File(cufflinksJob.output_dir, s"$sampleId.isoforms_fpkm_$name")
      job
    }
  }
}
