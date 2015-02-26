package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcallingTrait
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaVariantcallingGatk(val root: Configurable) extends QScript with ShivaVariantcallingTrait {
  qscript =>
  def this() = this(null)

  override def callers = {
    new HaplotypeCallerAllele ::
      new UnifiedGenotyperAllele ::
      new UnifiedGenotyper ::
      new HaplotypeCaller ::
      super.callers
  }

  class HaplotypeCaller extends Variantcaller {
    val name = "haplotypecaller"
    protected val defaultPrio = 1
    protected val defaultUse = true

    def outputFile = new File(outputDir, namePrefix + "haplotypecaller.vcf.gz")

    def addJobs() {
      val hc = new nl.lumc.sasc.biopet.extensions.gatk.broad.HaplotypeCaller(qscript)
      hc.input_file = inputBams
      hc.out = outputFile
      add(hc)
    }
  }

  class UnifiedGenotyper extends Variantcaller {
    val name = "unifiedgenotyper"
    protected val defaultPrio = 20
    protected val defaultUse = false

    def outputFile = new File(outputDir, namePrefix + "unifiedgenotyper.vcf.gz")

    def addJobs() {
      val ug = new nl.lumc.sasc.biopet.extensions.gatk.broad.UnifiedGenotyper(qscript)
      ug.input_file = inputBams
      ug.out = outputFile
      add(ug)
    }
  }

  class HaplotypeCallerAllele extends Variantcaller {
    val name = "haplotypecaller_allele"
    protected val defaultPrio = 5
    protected val defaultUse = false

    def outputFile = new File(outputDir, namePrefix + "haplotypecaller_allele.vcf.gz")

    def addJobs() {
      val hc = new nl.lumc.sasc.biopet.extensions.gatk.broad.HaplotypeCaller(qscript)
      hc.input_file = inputBams
      hc.out = outputFile
      hc.alleles = config("input_alleles")
      hc.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
      add(hc)
    }
  }

  class UnifiedGenotyperAllele extends Variantcaller {
    val name = "unifiedgenotyper_allele"
    protected val defaultPrio = 6
    protected val defaultUse = false

    def outputFile = new File(outputDir, namePrefix + "unifiedgenotyper_allele.vcf.gz")

    def addJobs() {
      val ug = new nl.lumc.sasc.biopet.extensions.gatk.broad.UnifiedGenotyper(qscript)
      ug.input_file = inputBams
      ug.out = outputFile
      ug.alleles = config("input_alleles")
      ug.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
      add(ug)
    }
  }
}

object ShivaVariantcallingGatk extends PipelineCommand