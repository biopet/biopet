/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.R.RScriptExecutor
import org.broadinstitute.gatk.utils.commandline.{ Output, Argument }
import scala.io.Source
import org.broadinstitute.gatk.utils.R.{ RScriptLibrary, RScriptExecutor }
import org.broadinstitute.gatk.utils.io.Resource
import scala.collection.mutable.Map
import scala.math._

class SampleCompareMetrics(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Argument(doc = "Sample Dir", shortName = "sampleDir", required = true)
  var sampleDir: String = _

  @Argument(doc = "Samples", shortName = "sample", required = true)
  var samples: List[String] = Nil

  @Argument(doc = "File sufix", shortName = "sufix", required = false)
  var fileSufix: String = _

  @Output(doc = "snpRelFile", shortName = "snpRelFile", required = true)
  var snpRelFile: File = _

  @Output(doc = "snpAbsFile", shortName = "snpAbsFile", required = true)
  var snpAbsFile: File = _

  @Output(doc = "indelRelFile", shortName = "indelRelFile", required = true)
  var indelRelFile: File = _

  @Output(doc = "indelAbsFile", shortName = "indelAbsFile", required = true)
  var indelAbsFile: File = _

  @Output(doc = "totalFile", shortName = "totalFile", required = true)
  var totalFile: File = _

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)

  override def commandLine = super.commandLine +
    required("-sampleDir", sampleDir) +
    repeat("-sample", samples) +
    optional("-fileSufix", fileSufix) +
    required("-snpRelFile", snpRelFile) +
    required("-snpAbsFile", snpAbsFile) +
    required("-indelRelFile", indelRelFile) +
    required("-indelAbsFile", indelAbsFile) +
    required("-totalFile", totalFile)
}

object SampleCompareMetrics {
  var sampleDir: String = _
  var samples: List[String] = Nil
  var fileSufix: String = ".eval.txt"
  var snpRelFile: File = _
  var snpAbsFile: File = _
  var indelRelFile: File = _
  var indelAbsFile: File = _
  var totalFile: File = _
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    for (t <- 0 until args.size) {
      args(t) match {
        case "-sample"       => samples +:= args(t + 1)
        case "-sampleDir"    => sampleDir = args(t + 1)
        case "-fileSufix"    => fileSufix = args(t + 1)
        case "-snpRelFile"   => snpRelFile = new File(args(t + 1))
        case "-snpAbsFile"   => snpAbsFile = new File(args(t + 1))
        case "-indelRelFile" => indelRelFile = new File(args(t + 1))
        case "-indelAbsFile" => indelAbsFile = new File(args(t + 1))
        case "-totalFile"    => totalFile = new File(args(t + 1))
        case _               =>
      }
    }
    if (sampleDir == null) throw new IllegalStateException("No sampleDir, use -sampleDir")
    else if (!sampleDir.endsWith("/")) sampleDir += "/"

    val regex = """\W+""".r
    val snpsOverlap: Map[(String, String), Int] = Map()
    val indelsOverlap: Map[(String, String), Int] = Map()
    val snpsTotal: Map[String, Int] = Map()
    val indelsTotal: Map[String, Int] = Map()
    for (sample1 <- samples; sample2 <- samples) {
      val reader = Source.fromFile(new File(sampleDir + sample1 + "/" + sample1 + "-" + sample2 + fileSufix))
      for (line <- reader.getLines) {
        regex.split(line) match {
          case Array(_, _, _, varType, all, novel, overlap, rate, _*) => {
            varType match {
              case "SNP" => {
                snpsOverlap += (sample1, sample2) -> overlap.toInt
                snpsTotal += sample1 -> all.toInt
              }
              case "INDEL" => {
                indelsOverlap += (sample1, sample2) -> overlap.toInt
                indelsTotal += sample1 -> all.toInt
              }
              case _ =>
            }
          }
          case _ =>
        }
      }
      reader.close()
    }

    val snpRelWritter = new PrintWriter(snpRelFile)
    val snpAbsWritter = new PrintWriter(snpAbsFile)
    val indelRelWritter = new PrintWriter(indelRelFile)
    val indelAbsWritter = new PrintWriter(indelAbsFile)

    val allWritters = List(snpRelWritter, snpAbsWritter, indelRelWritter, indelAbsWritter)
    for (writter <- allWritters) writter.println(samples.mkString("\t", "\t", ""))
    for (sample1 <- samples) {
      for (writter <- allWritters) writter.print(sample1)
      for (sample2 <- samples) {
        snpRelWritter.print("\t" + (round((snpsOverlap(sample1, sample2).toDouble / snpsTotal(sample1) * 10000.0)) / 10000.0))
        snpAbsWritter.print("\t" + snpsOverlap(sample1, sample2))
        indelRelWritter.print("\t" + (round((indelsOverlap(sample1, sample2).toDouble / indelsTotal(sample1) * 10000.0)) / 10000.0))
        indelAbsWritter.print("\t" + indelsOverlap(sample1, sample2))
      }
      for (writter <- allWritters) writter.println()
    }
    for (writter <- allWritters) writter.close()

    val totalWritter = new PrintWriter(totalFile)
    totalWritter.println("Sample\tSNPs\tIndels")
    for (sample <- samples)
      totalWritter.println(sample + "\t" + snpsTotal(sample) + "\t" + indelsTotal(sample))
    totalWritter.close()

    def plot(file: File) {
      val executor = new RScriptExecutor
      executor.addScript(new Resource("plotHeatmap.R", getClass))
      executor.addArgs(file, file.getAbsolutePath.stripSuffix(".tsv") + ".png", file.getAbsolutePath.stripSuffix(".tsv") + ".clustering.png")
      executor.exec()
    }
    plot(snpRelFile)
    plot(indelRelFile)
  }
}