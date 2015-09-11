/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.variant.variantcontext.{ Allele, VariantContext, VariantContextBuilder }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeader }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ ToolCommand, ToolCommandFuntion }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.JavaConversions._
import scala.collection.{ mutable, SortedMap }
import scala.collection.mutable.{ Map, Set }

class MergeAlleles(val root: Configurable) extends ToolCommandFuntion {
  javaMainClass = getClass.getName

  @Input(doc = "Input vcf files", shortName = "input", required = true)
  var input: List[File] = Nil

  @Output(doc = "Output vcf file", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "Output vcf file index", shortName = "output", required = true)
  private var outputIndex: File = _

  var reference: File = config("reference")

  override def defaultCoreMemory = 1.0

  override def beforeGraph() {
    super.beforeGraph()
    if (output.getName.endsWith(".gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
  }

  override def commandLine = super.commandLine +
    repeat("-I", input) +
    required("-o", output) +
    required("-R", reference)
}

object MergeAlleles {
  def apply(root: Configurable, input: List[File], output: File): MergeAlleles = {
    val mergeAlleles = new MergeAlleles(root)
    mergeAlleles.input = input
    mergeAlleles.output = output
    mergeAlleles
  }
}
