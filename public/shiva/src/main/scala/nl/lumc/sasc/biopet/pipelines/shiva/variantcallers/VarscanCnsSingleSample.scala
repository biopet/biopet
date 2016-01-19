package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.PrintWriter

import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.extensions.varscan.{ FixMpileup, VarscanMpileup2cns }
import nl.lumc.sasc.biopet.extensions.{Bgzip, Tabix}
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by sajvanderzeeuw on 15-1-16.
 */
class VarscanCnsSingleSample(val root: Configurable) extends Variantcaller {
  val name = "varscan_cns_singlesample"
  protected def defaultPrio = 25

  override def defaults = Map(
    "samtoolsmpileup" -> Map(
      "disable_baq" -> true,
      "depth" -> 1000000
    ),
    "varscanmpileup2cns" -> Map("strand_filter" -> 0)
  )

  override def fixedValues = Map(
    "samtoolsmpileup" -> Map("output_mapping_quality" -> true),
    "varscanmpileup2cns" -> Map("output_vcf" -> 1)
  )

  def biopetScript: Unit = {
    val sampleVcfs = for ((sample, inputBam) <- inputBams.toList) yield {
      val mpileup = new SamtoolsMpileup(this)
      mpileup.input = List(inputBam)

      val sampleVcf = new File(outputDir, s"${name}_$sample.vcf.gz")

      val sampleFile = new File(outputDir, s"$sample.name.txt")
      sampleFile.getParentFile.mkdirs()
      sampleFile.deleteOnExit()
      val writer = new PrintWriter(sampleFile)
      writer.println(sample)
      writer.close()

      val varscan = new VarscanMpileup2cns(this)
      varscan.vcfSampleList = Some(sampleFile)

      add(mpileup | new FixMpileup(this) | varscan | new Bgzip(this) > sampleVcf)
      add(Tabix(this, sampleVcf))

      sampleVcf
    }

    val cv = new CombineVariants(this)
    cv.inputFiles = sampleVcfs
    cv.outputFile = outputFile
    cv.setKey = "null"
    cv.excludeNonVariants = true
    add(cv)
  }
}
