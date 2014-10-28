package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class SamToFastq(val root: Configurable) extends Picard {
  javaMainClass = "picard.sam.SamToFastq"

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "R1", required = true)
  var fastqR1: File = _
  
  @Output(doc = "R1", required = false)
  var fastqR2: File = _
  
  @Output(doc = "Unpaired", required = false)
  var fastqUnpaired: File = _
  
  @Argument(doc = "Output per readgroup", required = false)
  var outputPerRg: Boolean = config("outputPerRg", default = false)
  
  @Argument(doc = "Output dir", required = false)
  var outputDir: String = config("outputDir")
  
  @Argument(doc = "re reverse", required = false)
  var reReverse: Boolean = config("reReverse", default = false)
  
  @Argument(doc = "The output file to bam file to", required = false)
  var interleave: Boolean = config("interleave", default = false)
  
  @Argument(doc = "includeNonPjReads", required = false)
  var includeNonPjReads: Boolean = config("includeNonPjReads", default = false)
  
  @Argument(doc = "clippingAtribute", required = false)
  var clippingAtribute: String = config("clippingAtribute")
  
  @Argument(doc = "clippingAction", required = false)
  var clippingAction: String = config("clippingAction")
  
  @Argument(doc = "read1Trim", required = false)
  var read1Trim: Option[Int] = config("read1Trim")
  
  @Argument(doc = "read1MaxBasesToWrite", required = false)
  var read1MaxBasesToWrite: Option[Int] = config("read1MaxBasesToWrite")
  
  @Argument(doc = "read2Trim", required = false)
  var read2Trim: Option[Int] = config("read2Trim")
  
  @Argument(doc = "read2MaxBasesToWrite", required = false)
  var read2MaxBasesToWrite: Option[Int] = config("read2MaxBasesToWrite")
  
  @Argument(doc = "includeNonPrimaryAlignments", required = false)
  var includeNonPrimaryAlignments: Boolean = config("includeNonPrimaryAlignments", default = false)

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("FASTQ=", fastqR1, spaceSeparated = false) +
    optional("SECOND_END_FASTQ=", fastqR2, spaceSeparated = false) +
    optional("UNPAIRED_FASTQ=", fastqUnpaired, spaceSeparated = false) +
    conditional(outputPerRg, "OUTPUT_PER_RG=TRUE") +
    optional("OUTPUT_DIR=", outputDir, spaceSeparated = false) +
    conditional(reReverse, "RE_REVERSE=TRUE") +
    conditional(interleave, "INTERLEAVE=TRUE") +
    conditional(includeNonPjReads, "INCLUDE_NON_PF_READS=TRUE") +
    optional("CLIPPING_ATTRIBUTE=", clippingAtribute, spaceSeparated = false) +
    optional("CLIPPING_ACTION=", clippingAction, spaceSeparated = false) +
    optional("READ1_TRIM=", read1Trim, spaceSeparated = false) +
    optional("READ1_MAX_BASES_TO_WRITE=", read1MaxBasesToWrite, spaceSeparated = false) +
    optional("READ2_TRIM=", read2Trim, spaceSeparated = false) +
    optional("READ2_MAX_BASES_TO_WRITE=", read2MaxBasesToWrite, spaceSeparated = false) +
    conditional(includeNonPrimaryAlignments, "INCLUDE_NON_PRIMARY_ALIGNMENTS=TRUE")
}

object SamToFastq {
  def apply(root: Configurable, input: File, fastqR1: File, fastqR2: File = null): SamToFastq = {
    val samToFastq = new SamToFastq(root)
    samToFastq.input = input
    samToFastq.fastqR1 = fastqR1
    samToFastq.fastqR2 = fastqR2
    return samToFastq
  }
}