package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import java.io.File

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.conifer.{ ConiferAnalyze, ConiferCall, ConiferRPKM }
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvanthof on 10/05/16.
 */
class ConiferMethod(val root: Configurable) extends CnvMethod {
  def name = "conifer"

  /** Exon definitions in bed format */
  var probeFile: File = config("probe_file")

  var controlsDir: File = config("controls_dir")

  /**Enable RPKM only mode, generate files for reference db */
  var RPKMonly: Boolean = false

  def biopetScript: Unit = {
    val RPKMdir = new File(outputDir, "rpkm")

    val rpkmFiles: List[File] = inputBams.map {
      case (sampleName, bamFile) =>
        val coniferRPKM = new ConiferRPKM(this)
        coniferRPKM.bamFile = bamFile.getAbsoluteFile
        coniferRPKM.probes = this.probeFile
        coniferRPKM.output = new File(RPKMdir, s"$sampleName.rpkm.txt")
        add(coniferRPKM)
        coniferRPKM.output
    }.toList ++ controlsDir.list.filter(_.toLowerCase.endsWith(".txt")).map { path =>
      val oldFile = new File(path)
      val newFile = new File(RPKMdir, s"control.${oldFile.getName}")
      add(Ln(this, oldFile, newFile))
      newFile
    }

    inputBams.foreach {
      case (sampleName, bamFile) =>
        val sampleDir = new File(outputDir, "samples" + File.separator + sampleName)

        val coniferAnalyze = new ConiferAnalyze(this)
        coniferAnalyze.deps ++= rpkmFiles
        coniferAnalyze.probes = probeFile
        coniferAnalyze.rpkmDir = RPKMdir
        coniferAnalyze.output = new File(sampleDir, s"$sampleName.hdf5")
        add(coniferAnalyze)

        val coniferCall = new ConiferCall(this)
        coniferCall.input = coniferAnalyze.output
        coniferCall.output = new File(sampleDir, s"${sampleName}.calls.txt")
        add(coniferCall)
    }

    addSummaryJobs()
  }

  override def summaryFiles = super.summaryFiles ++ Map("probe_file" -> probeFile)
}
