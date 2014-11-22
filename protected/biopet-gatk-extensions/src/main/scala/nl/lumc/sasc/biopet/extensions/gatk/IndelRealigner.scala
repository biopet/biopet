package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class IndelRealigner(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.IndelRealigner with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object IndelRealigner {
  def apply(root: Configurable, input: File, targetIntervals: File, outputDir: String): IndelRealigner = {
    val ir = new IndelRealigner(root)
    ir.input_file :+= input
    ir.targetIntervals = targetIntervals
    ir.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.bam")
    return ir
  }
}