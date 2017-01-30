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
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.variantcontext.{ Allele, VariantContext, VariantContextBuilder }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeader }
import nl.lumc.sasc.biopet.utils.{ FastaUtils, ToolCommand }
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.collection.JavaConversions._
import scala.collection.{ SortedMap, mutable }

object MergeAlleles extends ToolCommand {

  case class Args(inputFiles: List[File] = Nil, outputFile: File = null, reference: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputVcf") minOccurs 2 required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputFiles = x :: c.inputFiles)
    }
    opt[File]('o', "outputVcf") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('R', "reference") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(reference = x)
    }
  }

  private val chunkSize = 50000

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val readers = commandArgs.inputFiles.map(new VCFFileReader(_, true))
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setReferenceDictionary(FastaUtils.getCachedDict(commandArgs.reference)).
      setOutputFile(commandArgs.outputFile).
      build)
    val header = new VCFHeader
    val referenceDict = FastaUtils.getCachedDict(commandArgs.reference)
    header.setSequenceDictionary(referenceDict)
    writer.writeHeader(header)

    for (chr <- referenceDict.getSequences; chunk <- 0 to (chr.getSequenceLength / chunkSize)) {
      val output: mutable.Map[Int, List[VariantContext]] = mutable.Map()

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
    writer.close()
    readers.foreach(_.close)
  }

  def mergeAlleles(records: List[VariantContext]): VariantContext = {
    val longestRef = {
      var l: Array[Byte] = Array()
      for (a <- records.map(_.getReference.getBases) if a.length > l.length) l = a
      Allele.create(l, true)
    }
    val alleles: mutable.Set[Allele] = mutable.Set()
    val builder = new VariantContextBuilder
    builder.chr(records.head.getContig)
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
