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

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.variant.variantcontext.Allele
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.AsyncVariantContextWriter
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.variant.vcf.VCFHeader
import java.io.File
import nl.lumc.sasc.biopet.core.{ ToolCommandFuntion, BiopetJavaCommandLineFunction, ToolCommand }
import scala.collection.SortedMap
import scala.collection.mutable.{ Map, Set }
import nl.lumc.sasc.biopet.core.config.Configurable
import scala.collection.JavaConversions._
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

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

  override def beforeGraph {
    super.beforeGraph
    if (output.getName.endsWith(".gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
  }

  override def commandLine = super.commandLine +
    repeat("-I", input) +
    required("-o", output) +
    required("-R", reference)
}

object MergeAlleles extends ToolCommand {
  def apply(root: Configurable, input: List[File], output: File): MergeAlleles = {
    val mergeAlleles = new MergeAlleles(root)
    mergeAlleles.input = input
    mergeAlleles.output = output
    return mergeAlleles
  }

  case class Args(inputFiles: List[File] = Nil, outputFile: File = null, reference: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputVcf") minOccurs (2) required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(inputFiles = x :: c.inputFiles)
    }
    opt[File]('o', "outputVcf") required () unbounded () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('R', "reference") required () unbounded () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(reference = x)
    }
  }

  private val chunkSize = 50000

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val readers = commandArgs.inputFiles.map(new VCFFileReader(_, true))
    val referenceFile = new FastaSequenceFile(commandArgs.reference, true)
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setReferenceDictionary(referenceFile.getSequenceDictionary).
      setOutputFile(commandArgs.outputFile).
      build)
    val header = new VCFHeader
    val referenceDict = referenceFile.getSequenceDictionary
    header.setSequenceDictionary(referenceDict)
    writer.writeHeader(header)

    for (chr <- referenceDict.getSequences; chunk <- (0 to (chr.getSequenceLength / chunkSize))) {
      val output: Map[Int, List[VariantContext]] = Map()

      val chrName = chr.getSequenceName
      val begin = chunk * chunkSize + 1
      val end = {
        val e = (chunk + 1) * chunkSize
        if (e > chr.getSequenceLength) chr.getSequenceLength else e
      }

      for (reader <- readers; variant <- reader.query(chrName, begin, end)) {
        val start = variant.getStart
        if (output.contains(start)) output += variant.getStart -> (variant :: output(start))
        else output += variant.getStart -> List(variant)
      }

      for ((k, v) <- SortedMap(output.toSeq: _*)) {
        writer.add(mergeAlleles(v))
      }
    }
    writer.close
    readers.foreach(_.close)
  }

  def mergeAlleles(records: List[VariantContext]): VariantContext = {
    val longestRef = {
      var l: Array[Byte] = Array()
      for (a <- records.map(_.getReference.getBases) if (a.length > l.size)) l = a
      Allele.create(l, true)
    }
    val alleles: Set[Allele] = Set()
    val builder = new VariantContextBuilder
    builder.chr(records.head.getChr)
    builder.start(records.head.getStart)

    for (record <- records) {
      if (record.getReference == longestRef) alleles ++= record.getAlternateAlleles
      else {
        val suffix = longestRef.getBaseString.stripPrefix(record.getReference.getBaseString)
        for (r <- record.getAlternateAlleles) alleles += Allele.create(r.getBaseString + suffix)
      }
    }
    builder.alleles(longestRef :: alleles.toList)
    builder.computeEndFromAlleles(longestRef :: alleles.toList, records.head.getStart)
    builder.make
  }
}
