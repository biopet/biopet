package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.extensions.{Bgzip, Tabix, gatk}
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsReheader
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.{ConfigUtils, IoUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

class MuTect2(val parent: Configurable) extends Variantcaller {

  def name = "mutect2"

  override val mergeVcfResults: Boolean = false

  // currently not relevant, at the moment only one somatic variant caller exists in Biopet
  // and results from this won't be merged together with the results from other methods
  def defaultPrio = -1

  private var tnPairs: List[TumorNormalPair] = List()

  var ponFile: Option[File] = config("panel_of_normals")

  //private var buildPONFromNormals: Boolean = config("build_PON_from_normals", default = false)

  override def init(): Unit = {
    loadTnPairsFromConfig()
    validateTnPairs()
  }

  private def loadTnPairsFromConfig(): Unit = {
    this.globalConfig.map.get("tumor_normal_pairs") match {
      case Some(x) => {
        try {
          for (elem <- ConfigUtils.any2list(x)) {
            val pair: Map[String, Any] = ConfigUtils
              .any2map(elem)
              .map({ case (key, sampleName) => key.toUpperCase() -> sampleName })

            tnPairs :+= TumorNormalPair(pair("T").toString, pair("N").toString)
          }
        } catch {
          case e: Exception =>
            Logging.addError(
              "Unable to parse the parameter 'tumor_normal_pairs' from configuration.",
              cause = e)
        }
      }
      case _ =>
        Logging.addError(
          "Parameter 'tumor_normal_pairs' is missing from configuration. When using MuTect2, samples configuration must give the pairs of matching tumor and normal samples.")
    }
  }

  private def validateTnPairs(): Unit = {
    var samplesWithBams = inputBams.keySet
    var tnSamples: List[String] = List()
    tnPairs.foreach(pair => tnSamples ++= List(pair.tumorSample, pair.normalSample))
    tnSamples.foreach(sample => {
      if (!samplesWithBams.contains(sample))
        Logging.addError(
          s"Parameter 'tumor_normal_pairs' contains a sample for which no input files can be found, sample name: $sample")
    })
    if (tnSamples.size != tnSamples.distinct.size)
      Logging.addError(
        "Each sample should appear once in the sample pairs given with the parameter 'tumor_normal_pairs'")
    if (tnSamples.size != samplesWithBams.size)
      Logging.addError(
        "The number of samples given with the parameter 'tumor_normal_pairs' has to match the number of samples for which there are input files defined.")
  }

  def biopetScript(): Unit = {
    val samplesDir: File = new File(outputDir, "samples")
    if (!samplesDir.exists()) samplesDir.mkdir()

    var renameSamples: List[String] = List()
    var intermResult: File = null

    if (tnPairs.size == 1) {
      val pair = tnPairs.head
      renameSamples = List(s"TUMOR ${pair.tumorSample}", s"NORMAL ${pair.normalSample}")
      intermResult = new File(samplesDir, s"${pair.tumorSample}-${pair.normalSample}.$name.vcf")
      addMuTect2(pair, intermResult)

    } else {
      var outputPerSample: List[TaggedFile] = List()
      for (pair <- tnPairs) {
        val pairLabel = s"${pair.tumorSample}-${pair.normalSample}"
        val out: File = new File(samplesDir, s"$pairLabel.$name.vcf")
        renameSamples ++= List(s"TUMOR.$pairLabel ${pair.tumorSample}",
                               s"NORMAL.$pairLabel ${pair.normalSample}")
        outputPerSample :+= TaggedFile(out, pairLabel)
        addMuTect2(pair, out)
      }

      intermResult = new File(outputFile.getAbsolutePath + ".tmp")

      val combineVariants = gatk.CombineVariants(this, outputPerSample, intermResult)
      combineVariants.genotypemergeoption = Some("UNIQUIFY")
      add(combineVariants)
    }

    add(BcftoolsReheader(this, intermResult, IoUtils.writeLinesToFile(renameSamples)) | new Bgzip(this) > outputFile)

    add(Tabix(this, outputFile))

  }

  def addMuTect2(pair: TumorNormalPair, outFile: File): Unit = {
    val muTect2 =
      gatk.MuTect2(this, inputBams(pair.tumorSample), inputBams(pair.normalSample), outFile)
    // TODO add also BQSR file?
    add(muTect2)
  }

}

case class TumorNormalPair(tumorSample: String, normalSample: String)
