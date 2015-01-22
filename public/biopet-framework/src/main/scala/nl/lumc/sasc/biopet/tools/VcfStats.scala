package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, File }

import htsjdk.variant.variantcontext.Genotype
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

  val genotypeOverlap: mutable.Map[String, mutable.Map[String, Int]] = mutable.Map()
  val variantOverlap: mutable.Map[String, mutable.Map[String, Int]] = mutable.Map()

  val genotypeStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map()

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

    // Init
    for (sample1 <- samples) {
      genotypeOverlap(sample1) = mutable.Map()
      variantOverlap(sample1) = mutable.Map()
      genotypeStats(sample1) = mutable.Map()
      for (sample2 <- samples) {
        genotypeOverlap(sample1)(sample2) = 0
        variantOverlap(sample1)(sample2) = 0
      }
    }

    // Reading vcf records
    logger.info("Start reading vcf records")
    for (record <- reader) {
      for (sample1 <- samples) {
        val genotype = record.getGenotype(sample1)
        checkGenotype(genotype)
        for (sample2 <- samples) {
          if (genotype.getAlleles == record.getGenotype(sample2).getAlleles) {
            genotypeOverlap(sample1)(sample2) = genotypeOverlap(sample1)(sample2) + 1
            if (!(genotype.isHomRef || genotype.isNoCall || genotype.isNonInformative))
              variantOverlap(sample1)(sample2) = variantOverlap(sample1)(sample2) + 1
          }
        }
      }
    }
    logger.info("Done reading vcf records")

    writeGenotypeFields(commandArgs.outputDir + "/genotype_", samples)
    writeOverlap(genotypeOverlap, commandArgs.outputDir + "/sample_compare/genotype_overlap", samples)
    writeOverlap(variantOverlap, commandArgs.outputDir + "/sample_compare/variant_overlap", samples)

    logger.info("Done")
  }

  def checkGenotype(genotype: Genotype): Unit = {
    val sample = genotype.getSampleName
    val dp = if (genotype.hasDP) genotype.getDP else "not set"
    if (!genotypeStats(sample).contains("DP")) genotypeStats(sample)("DP") = mutable.Map()
    genotypeStats(sample)("DP")(dp) = genotypeStats(sample)("DP").getOrElse(dp, 0) + 1

    val gq = if (genotype.hasGQ) genotype.getGQ else "not set"
    if (!genotypeStats(sample).contains("GQ")) genotypeStats(sample)("GQ") = mutable.Map()
    genotypeStats(sample)("GQ")(gq) = genotypeStats(sample)("DP").getOrElse(gq, 0) + 1

    //TODO: add AD field
  }

  def writeGenotypeFields(prefix: String, samples: List[String]) {
    val fields = List("DP", "GQ")
    for (field <- fields) {
      val file = new File(prefix + field + ".tsv")
      file.getParentFile.mkdirs()
      val writer = new PrintWriter(file)
      writer.println(samples.mkString("\t", "\t", ""))
      val keySet = (for (sample <- samples) yield genotypeStats(sample)(field).keySet).fold(Set[Any]())(_ ++ _)
      for (key <- keySet.toList.sortWith(sortAnyAny(_, _))) {
        val values = for (sample <- samples) yield genotypeStats(sample)(field).getOrElse(key, 0)
        writer.println(values.mkString(key + "\t", "\t", ""))
      }
      writer.close()
      plotXy(file)
    }
  }

  def sortAnyAny(a: Any, b: Any): Boolean = {
    a match {
      case ai: Int => {
        b match {
          case bi: Int => ai < bi
          case _       => a.toString < b.toString
        }
      }
      case _ => a.toString < b.toString
    }
  }

  def writeOverlap(overlap: mutable.Map[String, mutable.Map[String, Int]], prefix: String, samples: List[String]): Unit = {
    val absFile = new File(prefix + ".abs.tsv")
    val relFile = new File(prefix + ".rel.tsv")

    absFile.getParentFile.mkdirs()

    val absWriter = new PrintWriter(absFile)
    val relWriter = new PrintWriter(relFile)

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

    plotHeatmap(relFile)
  }

  def plotHeatmap(file: File) {
    val executor = new RScriptExecutor
    executor.addScript(new Resource("plotHeatmap.R", getClass))
    executor.addArgs(file, file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.png", file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.clustering.png")
    executor.exec()
  }

  def plotXy(file: File) {
    val executor = new RScriptExecutor
    executor.addScript(new Resource("plotXY.R", getClass))
    executor.addArgs(file, file.getAbsolutePath.stripSuffix(".tsv") + ".xy.png")
    executor.exec()
  }
}
