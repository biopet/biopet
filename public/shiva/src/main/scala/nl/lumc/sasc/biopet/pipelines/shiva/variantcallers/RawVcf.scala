package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.File

import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.extensions.tools.{ VcfFilter, MpileupToVcf }
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Makes a vcf file from a mpileup without statistics */
class RawVcf(val root: Configurable) extends Variantcaller {
  val name = "raw"

  // This caller is designed as fallback when other variantcallers fails to report
  protected def defaultPrio = Int.MaxValue

  def biopetScript {
    val rawFiles = inputBams.map {
      case (sample, bamFile) =>
        val mp = new SamtoolsMpileup(this) {
          override def configName = "samtoolsmpileup"
          override def defaults = Map("samtoolsmpileup" -> Map("disable_baq" -> true, "min_map_quality" -> 1))
        }
        mp.input :+= bamFile

        val m2v = new MpileupToVcf(this)
        m2v.inputBam = bamFile
        m2v.output = new File(outputDir, sample + ".raw.vcf")
        add(mp | m2v)

        val vcfFilter = new VcfFilter(this) {
          override def configName = "vcffilter"
          override def defaults = Map("min_sample_depth" -> 8,
            "min_alternate_depth" -> 2,
            "min_samples_pass" -> 1,
            "filter_ref_calls" -> true
          )
        }
        vcfFilter.inputVcf = m2v.output
        vcfFilter.outputVcf = new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.filter.vcf.gz")
        add(vcfFilter)
        vcfFilter.outputVcf
    }

    val cv = new CombineVariants(this)
    cv.inputFiles = rawFiles.toList
    cv.outputFile = outputFile
    cv.setKey = "null"
    cv.excludeNonVariants = true
    add(cv)
  }
}
