package nl.lumc.sasc.biopet.tools

import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import scala.collection.JavaConversions._
import nl.lumc.sasc.biopet.util.VcfUtils._

class BastyGenerateFasta(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input vcf file", required = false)
  var inputVcf: File = _

  @Input(doc = "Bam File", required = false)
  var bamFile: File = _

  @Output(doc = "Output fasta, variants only", required = false)
  var outputVariants: File = _

  @Argument(doc = "Output interval list", required = false)
  var snpsOnly: Boolean = config("snps_only", default = false)

  @Argument(doc = "Sample name", required = false)
  var sampleName: String = _

  @Argument(doc = "minAD", required = false)
  var minAD: Int = config("min_ad", default = 8)

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
  var reference = false

  override def commandLine = super.commandLine +
    optional("--inputVcf", inputVcf) +
    optional("--bamFile", bamFile) +
    optional("--outputVariants", outputVariants) +
    conditional(snpsOnly, "--snpsOnly") +
    optional("--sampleName", sampleName) +
    optional("--minAD", minAD) +
    conditional(reference, "--reference")
}

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
                  reference: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('V', "inputVcf") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(inputVcf = x)
    }
    opt[File]("bamFile") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(bamFile = x)
    }
    opt[File]("outputVariants") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputVariants = x)
    }
    opt[File]("outputConsensus") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputConsensus = x)
    }
    opt[File]("outputConsensusVariants") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputConsensusVariants = x)
    }
    opt[Unit]("snpsOnly") unbounded () action { (x, c) =>
      c.copy(snpsOnly = true)
    }
    opt[String]("sampleName") unbounded () action { (x, c) =>
      c.copy(sampleName = x)
    }
    opt[String]("outputName") required () unbounded () action { (x, c) =>
      c.copy(outputName = x)
    }
    opt[Int]("minAD") unbounded () action { (x, c) =>
      c.copy(minAD = x)
    }
    opt[Int]("minDepth") unbounded () action { (x, c) =>
      c.copy(minDepth = x)
    }
    opt[File]("reference") unbounded () action { (x, c) =>
      c.copy(reference = x)
    }
  }

  protected var cmdArgs: Args = _
  private val chunkSize = 100000

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    cmdArgs = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (cmdArgs.outputVariants != null) writeVariantsOnly()
    if (cmdArgs.outputConsensus != null || cmdArgs.outputConsensusVariants != null) writeConsensus()
  }

  protected def writeConsensus() {
    if (cmdArgs.reference == null)
      throw new IllegalStateException("No reference suplied")
    if (cmdArgs.outputConsensusVariants != null) {
      if (cmdArgs.inputVcf == null)
        throw new IllegalStateException("To write outputVariants input vcf is required, please use --inputVcf option")
      if (!cmdArgs.inputVcf.exists)
        throw new IllegalStateException("File does not exist: " + cmdArgs.inputVcf)
    }
    if (cmdArgs.sampleName != null) {
      if (cmdArgs.bamFile == null)
        throw new IllegalStateException("To write Consensus input bam file is required, please use --bamFile option")
      if (!cmdArgs.bamFile.exists)
        throw new IllegalStateException("File does not exist: " + cmdArgs.bamFile)
    }

    logger.info(cmdArgs.reference)

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
          (for (variant <- reader.query(chrName, begin, end) if (!cmdArgs.snpsOnly || variant.isSNP)) yield {
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
          for (t <- 0 until coverage.length) coverage(t) = cmdArgs.minDepth
        }

        val consensus = for (t <- 0 until coverage.length) yield {
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
              val stripSufix = if (variant.get._1._2 > end) variant.get._1._2 - end else 0
              val allele = getMaxAllele(variant.get._2)
              consensusPos += variant.get._2.getReference.getBases.length
              buffer.append(allele.substring(stripPrefix, allele.size - stripSufix))
            } else {
              buffer.append(consensus(consensusPos))
              consensusPos += 1
            }
          }
        }

        (chunk -> (consensus.mkString.toUpperCase, buffer.toString.toUpperCase))
      }).toMap
      if (cmdArgs.outputConsensus != null) {
        val writer = new PrintWriter(cmdArgs.outputConsensus)
        writer.println(">" + cmdArgs.outputName)
        for (c <- chunks.keySet.toList.sortWith(_ < _)) {
          writer.print(chunks(c)._1)
        }
        writer.close
      }
      if (cmdArgs.outputConsensusVariants != null) {
        val writer = new PrintWriter(cmdArgs.outputConsensusVariants)
        writer.println(">" + cmdArgs.outputName)
        for (c <- chunks.keySet.toList.sortWith(_ < _)) {
          writer.print(chunks(c)._2)
        }
        writer.close
      }
    }
  }

  protected def writeVariantsOnly() {
    if (cmdArgs.inputVcf == null)
      throw new IllegalStateException("To write outputVariants input vcf is required, please use --inputVcf option")
    if (!cmdArgs.inputVcf.exists)
      throw new IllegalStateException("File does not exist: " + cmdArgs.inputVcf)
    val writer = new PrintWriter(cmdArgs.outputVariants)
    writer.println(">" + cmdArgs.sampleName)
    val vcfReader = new VCFFileReader(cmdArgs.inputVcf, false)
    for (vcfRecord <- vcfReader if (!cmdArgs.snpsOnly || vcfRecord.isSNP)) yield {
      writer.print(getMaxAllele(vcfRecord))
    }
    writer.println()
    writer.close
    vcfReader.close
  }

  protected def getMaxAllele(vcfRecord: VariantContext): String = {
    val maxSize = getLongestAllele(vcfRecord).getBases.length

    if (cmdArgs.sampleName == null) return fillAllele(vcfRecord.getReference.getBaseString, maxSize)

    val genotype = vcfRecord.getGenotype(cmdArgs.sampleName)
    if (genotype == null) return fillAllele("", maxSize)
    val AD = genotype.getAD
    if (AD == null) return fillAllele("", maxSize)
    val maxADid = AD.zipWithIndex.maxBy(_._1)._2
    if (AD(maxADid) < cmdArgs.minAD) return fillAllele("", maxSize)
    return fillAllele(vcfRecord.getAlleles()(maxADid).getBaseString, maxSize)
  }
}