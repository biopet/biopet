/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

class IndelRealigner(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.IndelRealigner with GatkGeneral {
  @Output
  protected var bamIndex: File = _

  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object IndelRealigner {
  def apply(root: Configurable, input: File, targetIntervals: File, outputDir: File): IndelRealigner = {
    val ir = new IndelRealigner(root)
    ir.input_file :+= input
    ir.targetIntervals = targetIntervals
    ir.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.bam")
    ir.bamIndex = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.bai")
    ir
  }
}