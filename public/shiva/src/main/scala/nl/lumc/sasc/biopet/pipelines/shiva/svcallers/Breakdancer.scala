package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.breakdancer.{ BreakdancerVCF, BreakdancerCaller, BreakdancerConfig }
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler Breakdancer */
class Breakdancer(val root: Configurable) extends SvCaller {
  def name = "breakdancer"

  def biopetScript() {
    for ((sample, bamFile) <- inputBams) {
      val breakdancerSampleDir = new File(outputDir, sample)

      // read config and set all parameters for the pipeline
      logger.debug("Starting Breakdancer configuration")

      val bdcfg = BreakdancerConfig(this, bamFile, new File(breakdancerSampleDir, sample + ".breakdancer.cfg"))
      val breakdancer = BreakdancerCaller(this, bdcfg.output, new File(breakdancerSampleDir, sample + ".breakdancer.tsv"))
      val bdvcf = BreakdancerVCF(this, breakdancer.output, new File(breakdancerSampleDir, sample + ".breakdancer.vcf"))
      add(bdcfg, breakdancer, bdvcf)

      outputFiles += (sample -> bdvcf.output)
    }
  }
}
