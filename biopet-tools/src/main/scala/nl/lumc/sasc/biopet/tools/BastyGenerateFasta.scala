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

import java.io.{File, PrintWriter}

import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}
import nl.lumc.sasc.biopet.utils.VcfUtils._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object BastyGenerateFasta extends ToolCommand {
  case class Args(inputVcf: File = null,
                  outputVariants: File = null,
                  outputConsensus: File = null,
                  outputConsensusVariants: File = null,
                  bamFile: File = null,
                  snpsOnly: Boolean = false,
                  sampleName: String = null,
                  outputName: String = null,
                  minAD: Int = 8,
                  minDepth: Int = 8,
                  reference: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('V', "inputVcf") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputVcf = x)
    } text "vcf file, needed for outputVariants and outputConsensusVariants" validate { x =>
      if (x.exists) success else failure("File does not exist: " + x)
    }
    opt[File]("bamFile") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    } text "bam file, needed for outputConsensus and outputConsensusVariants" validate { x =>
      if (x.exists) success else failure("File does not exist: " + x)
    }
    opt[File]("outputVariants") maxOccurs 1 unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputVariants = x)
    } text "fasta with only variants from vcf file"
    opt[File]("outputConsensus") maxOccurs 1 unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputConsensus = x)
    } text "Consensus fasta from bam, always reference bases else 'N'"
    opt[File]("outputConsensusVariants") maxOccurs 1 unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(outputConsensusVariants = x)
    } text "Consensus fasta from bam with variants from vcf file, always reference bases else 'N'"
    opt[Unit]("snpsOnly") unbounded () action { (_, c) =>
      c.copy(snpsOnly = true)
    } text "Only use snps from vcf file"
    opt[String]("sampleName") unbounded () action { (x, c) =>
      c.copy(sampleName = x)
    } text "Sample name in vcf file"
    opt[String]("outputName") required () unbounded () action { (x, c) =>
      c.copy(outputName = x)
    } text "Output name in fasta file header"
    opt[Int]("minAD") unbounded () action { (x, c) =>
      c.copy(minAD = x)
    } text "min AD value in vcf file for sample. Defaults to: 8"
    opt[Int]("minDepth") unbounded () action { (x, c) =>
      c.copy(minDepth = x)
    } text "min depth in bam file. Defaults to: 8"
    opt[File]("reference") unbounded () action { (x, c) =>
      c.copy(reference = x)
    } text "Indexed reference fasta file" validate { x =>
      if (x.exists) success else failure("File does not exist: " + x)
    }

    checkConfig { c =>
      {
        val err: ListBuffer[String] = ListBuffer()
        if (c.outputConsensus != null || c.outputConsensusVariants != null) {
          if (c.reference == null)
            err.add("No reference supplied")
          else {
            val index = new File(c.reference.getAbsolutePath + ".fai")
            if (!index.exists) err.add("Reference does not have index")
          }
          if (c.outputConsensusVariants != null && c.inputVcf == null)
            err.add("To write outputVariants input vcf is required, please use --inputVcf option")
          if (c.sampleName != null && c.bamFile == null)
            err.add("To write Consensus input bam file is required, please use --bamFile option")
        }
        if (c.outputVariants != null && c.inputVcf == null)
          err.add("To write outputVariants input vcf is required, please use --inputVcf option")
        if (c.outputVariants == null && c.outputConsensus == null && c.outputConsensusVariants == null)
          err.add("No output file selected")
        if (err.isEmpty) success else failure(err.mkString("", "\nError: ", "\n"))
      }
    }
  }

  protected implicit var cmdArgs: Args = _
  private val chunkSize = 100000

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (cmdArgs.outputVariants != null) {
      writeVariantsOnly()
    }
    if (cmdArgs.outputConsensus != null || cmdArgs.outputConsensusVariants != null) {
      writeConsensus()
    }

    //FIXME: what to do if outputcConsensus is set, but not outputConsensusVariants (and vice versa)?
  }

  protected def writeConsensus() {
    //FIXME: preferably split this up in functions, so that they can be unit tested
    val referenceFile = new IndexedFastaSequenceFile(cmdArgs.reference)
    val referenceDict = referenceFile.getSequenceDictionary

    for (chr <- referenceDict.getSequences) {
      val chunks = (for (chunk <- (0 to (chr.getSequenceLength / chunkSize)).par) yield {
        val chrName = chr.getSequenceName
        val begin = chunk * chunkSize + 1
        val end = {
          val e = (chunk + 1) * chunkSize
          if (e > chr.getSequenceLength) chr.getSequenceLength else e
        }

        logger.info("begin on: chrName: " + chrName + "  begin: " + begin + "  end: " + end)

        val referenceSequence = referenceFile.getSubsequenceAt(chrName, begin, end)

        val variants: Map[(Int, Int), VariantContext] = if (cmdArgs.inputVcf != null) {
          val reader = new VCFFileReader(cmdArgs.inputVcf, true)
          (for (variant <- reader.query(chrName, begin, end) if !cmdArgs.snpsOnly || variant.isSNP)
            yield {
              (variant.getStart, variant.getEnd) -> variant
            }).toMap
        } else Map()

        val coverage: Array[Int] = Array.fill(end - begin + 1)(0)
        if (cmdArgs.bamFile != null) {
          val inputSam = SamReaderFactory.makeDefault.open(cmdArgs.bamFile)
          for (r <- inputSam.query(chr.getSequenceName, begin, end, false)) {
            val s = if (r.getAlignmentStart < begin) begin else r.getAlignmentStart
            val e = if (r.getAlignmentEnd > end) end else r.getAlignmentEnd
            for (t <- s to e) coverage(t - begin) += 1
          }
        } else {
          for (t <- coverage.indices) coverage(t) = cmdArgs.minDepth
        }

        val consensus = for (t <- coverage.indices) yield {
          if (coverage(t) >= cmdArgs.minDepth) referenceSequence.getBases()(t).toChar
          else 'N'
        }

        val buffer: StringBuilder = new StringBuilder()
        if (cmdArgs.outputConsensusVariants != null) {
          var consensusPos = 0
          while (consensusPos < consensus.size) {
            val genomePos = consensusPos + begin
            val variant = variants.find(a => a._1._1 >= genomePos && a._1._2 <= genomePos)
            if (variant.isDefined) {
              logger.info(variant.get._2)
              val stripPrefix = if (variant.get._1._1 < begin) begin - variant.get._1._1 else 0
              val stripSuffix = if (variant.get._1._2 > end) variant.get._1._2 - end else 0
              val allele = getMaxAllele(variant.get._2)
              consensusPos += variant.get._2.getReference.getBases.length
              buffer.append(allele.substring(stripPrefix, allele.length - stripSuffix))
            } else {
              buffer.append(consensus(consensusPos))
              consensusPos += 1
            }
          }
        }

        chunk -> (consensus.mkString.toUpperCase, buffer.toString().toUpperCase)
      }).toMap
      if (cmdArgs.outputConsensus != null) {
        val writer = new PrintWriter(cmdArgs.outputConsensus)
        writer.println(">" + cmdArgs.outputName)
        for (c <- chunks.keySet.toList.sortWith(_ < _)) {
          writer.print(chunks(c)._1)
        }
        writer.println()
        writer.close()
      }
      if (cmdArgs.outputConsensusVariants != null) {
        val writer = new PrintWriter(cmdArgs.outputConsensusVariants)
        writer.println(">" + cmdArgs.outputName)
        for (c <- chunks.keySet.toList.sortWith(_ < _)) {
          writer.print(chunks(c)._2)
        }
        writer.println()
        writer.close()
      }
    }
  }

  protected[tools] def writeVariantsOnly() {
    val writer = new PrintWriter(cmdArgs.outputVariants)
    writer.println(">" + cmdArgs.outputName)
    val vcfReader = new VCFFileReader(cmdArgs.inputVcf, false)
    for (vcfRecord <- vcfReader if !cmdArgs.snpsOnly || vcfRecord.isSNP) yield {
      writer.print(getMaxAllele(vcfRecord))
    }
    writer.println()
    writer.close()
    vcfReader.close()
  }

  // TODO: what does this do?
  // Seems to me it finds the allele in a sample with the highest AD value
  // if this allele is shorter than the largest allele, it will append '-' to the string
  protected[tools] def getMaxAllele(vcfRecord: VariantContext)(implicit cmdArgs: Args): String = {
    val maxSize = getLongestAllele(vcfRecord).getBases.length

    if (cmdArgs.sampleName == null) {
      return fillAllele(vcfRecord.getReference.getBaseString, maxSize)
    }

    val genotype = vcfRecord.getGenotype(cmdArgs.sampleName)

    if (genotype == null) {
      return fillAllele("", maxSize)
    }

    val AD =
      if (genotype.hasAD) genotype.getAD
      else Array.fill(vcfRecord.getAlleles.size())(cmdArgs.minAD)

    if (AD == null) {
      return fillAllele("", maxSize)
    }

    val maxADid = AD.zipWithIndex.maxBy(_._1)._2

    if (AD(maxADid) < cmdArgs.minAD) {
      return fillAllele("", maxSize)
    }

    fillAllele(vcfRecord.getAlleles()(maxADid).getBaseString, maxSize)
  }
}
