package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.extensions.{Awk, Bgzip, Tabix, gatk}
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsReheader
import nl.lumc.sasc.biopet.extensions.gatk.SelectVariants
import nl.lumc.sasc.biopet.extensions.igvtools.IGVToolsIndex
import nl.lumc.sasc.biopet.utils.IoUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

class MuTect2(val parent: Configurable) extends SomaticVariantcaller {

  def name = "mutect2"

  override val mergeVcfResults: Boolean = false

  // currently not relevant, at the moment only one somatic variant caller exists in Biopet
  // and results from this won't be merged together with the results from other methods
  def defaultPrio = -1

  //val buildPONFromNormals: Boolean = config("build_PON_from_normals", default = false)

  val runConEst = config("run_contest", default="false")

  def biopetScript(): Unit = {
    val samplesDir: File = new File(outputDir, "samples")
    if (!samplesDir.exists()) samplesDir.mkdir()

    var renameSamples: List[String] = List()
    var tumorSamples: List[String] = List()
    var intermResult: File = null

    if (tnPairs.size == 1) {
      val pair = tnPairs.head
      renameSamples = List(s"TUMOR ${pair.tumorSample}")
      tumorSamples = List("TUMOR")
      intermResult = new File(samplesDir, s"${pair.tumorSample}-${pair.normalSample}.$name.vcf")
      addJobForPair(pair, intermResult)

    } else {
      var outputPerSample: List[TaggedFile] = List()
      for (pair <- tnPairs) {
        val pairLabel = s"${pair.tumorSample}-${pair.normalSample}"
        val out: File = new File(samplesDir, s"$pairLabel.$name.vcf")
        renameSamples :+= s"TUMOR.$pairLabel ${pair.tumorSample}"
        tumorSamples :+= s"TUMOR.$pairLabel"
        outputPerSample :+= TaggedFile(out, pairLabel)
        addJobForPair(pair, out)
      }

      var sIndex = outputFile.getAbsolutePath.lastIndexOf(".vcf")
      intermResult = new File((if (sIndex != -1) outputFile.getAbsolutePath.substring(0, sIndex) else outputFile.getAbsolutePath) + ".all_samples.vcf")

      val combineVariants = gatk.CombineVariants(this, outputPerSample, intermResult)
      combineVariants.genotypemergeoption = Some("UNIQUIFY")
      add(combineVariants)
    }

    val selectVariants = new SelectVariants(this)
    selectVariants.variant = intermResult
    selectVariants.sample_name = tumorSamples

    val file: File = new File(config("output_dir").asFile, ".renameSamples.txt")
    file.deleteOnExit()
    IoUtils.writeLinesToFile(file, renameSamples)

    add(selectVariants | BcftoolsReheader(this, file) | new Bgzip(this) > outputFile)

    add(IGVToolsIndex(this, intermResult))
    add(Tabix(this, outputFile))

  }

  def addJobForPair(pair: TumorNormalPair, outFile: File): Unit = {
    val muTect2 = gatk.MuTect2(this, inputBams(pair.tumorSample), inputBams(pair.normalSample), outFile)
    // TODO add also BQSR file?

    if (runConEst) {
      val namePrefix = outFile.getAbsolutePath.stripSuffix(".vcf")
      val contEstOutput: File = new File(s"$namePrefix.contamination.txt")
      val contEst = gatk.ContEst(this, inputBams(pair.tumorSample), inputBams(pair.normalSample), contEstOutput)
      add(contEst)

      val contaminationPerSample: File = new File(s"$namePrefix.contamination.short.txt")
      val awk: Awk = Awk(this, "BEGIN{OFS=\"\\t\"}{if($1 != \"name\") print $1,$4;}")
      awk.input = contEstOutput
      add(awk > contaminationPerSample)

      muTect2.contaminationFile = Some(contaminationPerSample)
    }

    add(muTect2)
  }

}
