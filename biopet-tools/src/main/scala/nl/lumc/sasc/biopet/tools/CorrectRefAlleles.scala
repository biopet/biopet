package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.variant.variantcontext.{Allele, VariantContext, VariantContextBuilder}
import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.collection.JavaConversions._

object CorrectRefAlleles extends ToolCommand {
  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  referenceFasta: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "input") required () unbounded () valueName "<vcf file>" action {
      (x, c) =>
        c.copy(inputFile = x)
    } text "input vcf file"
    opt[File]('o', "output") required () unbounded () valueName "<vcf file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output vcf file"
    opt[File]('R', "referenceFasta") required () unbounded () valueName "<fasta file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    } text "Reference fasta file"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val referenceFile = new IndexedFastaSequenceFile(cmdArgs.referenceFasta)

    val reader = new VCFFileReader(cmdArgs.inputFile, false)
    val header = reader.getFileHeader
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(cmdArgs.outputFile)
        .setReferenceDictionary(header.getSequenceDictionary)
        .build)
    writer.writeHeader(header)

    for (record <- reader) {
      val ref = referenceFile.getSubsequenceAt(record.getContig, record.getStart, record.getEnd).getBaseString
      val correct = record.getAlleles.forall { allele =>
        if (allele.isReference) allele.getBaseString == ref
        else allele.getBaseString != ref
      }
      if (correct) writer.add(record)
      else {
        val alleles = record.getAlleles.map { a =>
          val bases = a.getBaseString
          Allele.create(bases, bases == ref)
        }
        val newRecord = new VariantContextBuilder(record)
          .alleles(alleles)
          .genotypes(record.getGenotypes).make()
        writer.add(newRecord)
      }
    }

    referenceFile.close()
    writer.close()
    reader.close()
  }
}
