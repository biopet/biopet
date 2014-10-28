package nl.lumc.sasc.biopet.tools

import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import scala.collection.JavaConversions._

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

  override def commandLine = super.commandLine +
    optional("--inputVcf", inputVcf) +
    optional("--bamFile", bamFile) +
    optional("--outputVariants", outputVariants) +
    conditional(snpsOnly, "--snpsOnly") +
    optional("--sampleName", sampleName) +
    optional("--minAD", minAD)
}

object BastyGenerateFasta extends ToolCommand {
  case class Args(inputVcf: File = null,
                  outputVariants: File = null,
                  bamFile: File = null,
                  snpsOnly: Boolean = false,
                  sampleName: String = null,
                  minAD: Int = 8) extends AbstractArgs

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
    opt[Unit]("snpsOnly") unbounded () action { (x, c) =>
      c.copy(snpsOnly = true)
    }
    opt[String]("sampleName") unbounded () action { (x, c) =>
      c.copy(sampleName = x)
    }
    opt[Int]("minAD") unbounded () action { (x, c) =>
      c.copy(minAD = x)
    }
  }

  var commandArgs: Args = _

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    commandArgs = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (commandArgs.outputVariants != null) writeVariantsOnly()

  }

  def writeVariantsOnly() {
    val writer = new PrintWriter(commandArgs.outputVariants)
    writer.println(">" + commandArgs.sampleName)
    val vcfReader = new VCFFileReader(commandArgs.inputVcf, false)
    for (vcfRecord <- vcfReader if (!commandArgs.snpsOnly || vcfRecord.isSNP)) yield {
      writer.print(getMaxAllele(vcfRecord, commandArgs.sampleName))
    }
    writer.println()
    writer.close
    vcfReader.close
  }

  def getMaxAllele(vcfRecord: VariantContext, sample: String): String = {
    val genotype = vcfRecord.getGenotype(sample)
    val longestAlleleID = vcfRecord.getAlleles.map(_.getBases.length).zipWithIndex.maxBy(_._1)._2
    val maxSize = vcfRecord.getAlleles()(longestAlleleID).getBases.length

    def fill(bases: String) = bases + (Array.fill[Char](maxSize - bases.size)('N')).mkString

    if (genotype == null) return fill(vcfRecord.getReference.getBaseString)
    val AD = genotype.getAD
    if (AD == null) return fill(vcfRecord.getReference.getBaseString)
    val maxADid = AD.zipWithIndex.maxBy(_._1)._2
    if (AD(maxADid) < commandArgs.minAD) return fill("")
    return fill(vcfRecord.getAlleles()(maxADid).getBaseString)
  }
}