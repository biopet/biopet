package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, File }

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.core.ToolCommand
import org.broadinstitute.gatk.utils.R.RScriptExecutor
import org.broadinstitute.gatk.utils.io.Resource
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by pjvan_thof on 1/10/15.
 */
class VcfStats {
  //TODO: add Queue wrapper
}

object VcfStats extends ToolCommand {
  case class Args(inputFile: File = null, outputDir: String = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[String]('o', "outputDir") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputDir = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    logger.info("Started")
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder.toList

    val genotypeOverlap: mutable.Map[String, mutable.Map[String, Int]] = mutable.Map()
    for (record <- reader) {
      for (sample1 <- samples) {
        for (sample2 <- samples) {
          if (record.getGenotype(sample1).getAlleles == record.getGenotype(sample2).getAlleles) {
            if (!genotypeOverlap.contains(sample1)) genotypeOverlap(sample1) = mutable.Map()
            val current = genotypeOverlap(sample1).getOrElse(sample2, 0)
            genotypeOverlap(sample1)(sample2) = current + 1
          }
        }
      }
    }

    writeOverlap(genotypeOverlap, commandArgs.outputDir + "/sample_genotype_overlap", samples)

    plot(new File(commandArgs.outputDir + "/sample_genotype_overlap.rel.tsv"))

    logger.info("Done")
  }

  def writeOverlap(overlap: mutable.Map[String, mutable.Map[String, Int]], prefix: String, samples: List[String]): Unit = {
    val absWriter = new PrintWriter(new File(prefix + ".abs.tsv"))
    val relWriter = new PrintWriter(new File(prefix + ".rel.tsv"))

    absWriter.println(samples.mkString("\t", "\t", ""))
    relWriter.println(samples.mkString("\t", "\t", ""))
    for (sample1 <- samples) {
      val values = for (sample2 <- samples) yield overlap.getOrElse(sample1, mutable.Map()).getOrElse(sample2, 0)
      absWriter.println(values.mkString(sample1 + "\t", "\t", ""))

      val total = overlap.getOrElse(sample1, mutable.Map()).getOrElse(sample1, 0)
      relWriter.println(values.map(_.toFloat / total).mkString(sample1 + "\t", "\t", ""))
    }
    absWriter.close()
    relWriter.close()
  }

  def plot(file: File) {
    val executor = new RScriptExecutor
    executor.addScript(new Resource("plotHeatmap.R", getClass))
    executor.addArgs(file, file.getAbsolutePath.stripSuffix(".tsv") + ".png", file.getAbsolutePath.stripSuffix(".tsv") + ".clustering.png")
    executor.exec()
  }
}
