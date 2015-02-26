package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad.HaplotypeCaller
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcallingTrait
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaVariantcallingGatk(val root: Configurable) extends QScript with ShivaVariantcallingTrait {
  qscript =>
  def this() = this(null)

  override def callers = new Haplotypecaller :: super.callers

  class Haplotypecaller extends Variantcaller {
    val name = "haplotypecaller"
    protected val defaultPrio = 1
    protected val defaultUse = true

    def outputFile = new File(outputDir, namePrefix + "haplotypecaller.vcf.gz")

    def addJobs() {
      val hc = new HaplotypeCaller(qscript)
      hc.input_file = inputBams
      hc.out = outputFile
      add(hc)
    }
  }
}

object ShivaVariantcallingGatk extends PipelineCommand