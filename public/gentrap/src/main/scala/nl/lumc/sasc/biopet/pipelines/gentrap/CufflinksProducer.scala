package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.File
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.extensions.{ Cufflinks, Ln }

/** General trait for containing cufflinks results */
trait CufflinksProducer { this: Gentrap#Sample =>

  import Gentrap._
  import Gentrap.ExpMeasures._
  import Gentrap.StrandProtocol._

  /** Valid cufflink measure types */
  protected val cufflinksMeasures = Set(CufflinksStrict, CufflinksGuided, CufflinksStrict)

  /** Cufflink's terms for strand specificity */
  lazy val strandedness: String = {
    require(pipeline.config.contains("strand_protocol"))
    pipeline.strandProtocol match {
      case NonSpecific => "fr-unstranded"
      case Dutp        => "fr-firststrand"
      case otherwise   => throw new IllegalStateException("Unexpected strand type for cufflinks: " + otherwise.toString)
    }
  }

  /** Case class for containing cufflinks + its output symlink jobs */
  protected case class CufflinksJobSet(cuffType: ExpMeasures.Value) {

    require(cufflinksMeasures.contains(cuffType), "Cufflinks measurement type is either strict, guided, or blind")

    /** Base name for output file extensions and config path */
    lazy val name: String = cuffType match {
      case CufflinksStrict => "cufflinks_strict"
      case CufflinksGuided => "cufflinks_guided"
      case CufflinksBlind  => "cufflinks_blin"
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
      job.output = new File(cufflinksJob.output_dir, s"$sampleId.isoformss_fpkm_$name")
      job
    }
  }
}
