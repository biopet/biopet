package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.gatk
import nl.lumc.sasc.biopet.extensions.gatk.CombineGVCFs
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 26-4-17.
 */
class GenotypeGvcfs(val parent: Configurable) extends QScript
  with BiopetQScript
  with Reference { qscript =>

  def this() = this(null)

  @Input(required = true, shortName = "V")
  var inputGvcfs: List[File] = Nil

  val namePrefix = config("name_prefix", default = "multisample")

  def finalGvcfFile = new File(outputDir, s"$namePrefix.gvcf.vcf")
  def finalVcfFile = new File(outputDir, s"$namePrefix.vcf")

  /** Init for pipeline */
  def init(): Unit = {}

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = getCombineGvcfs(inputGvcfs, finalGvcfFile, isIntermediate = false)
    jobs.foreach(add(_))

    val genotype = new gatk.GenotypeGVCFs(this)
    genotype.variant = List(finalGvcfFile)
    genotype.out = finalVcfFile
    add(genotype)
  }

  private def groupDir(group: List[Int]): File = {
    val rootGroupDir = new File(outputDir, ".group")
    new File(rootGroupDir, group.mkString(File.separator))
  }

  def getCombineGvcfs(inputFiles: List[File],
                      outputFile: File,
                      isIntermediate: Boolean = true,
                      group: List[Int] = Nil): List[CombineGVCFs] = {
    if (inputFiles.size > 10) {
      inputFiles.size

      val bla = (for ((list, i) <- inputFiles.grouped(10).zipWithIndex) yield {
        val groupedOutputFile = new File(groupDir(group), outputFile.getName)
        val job = getCombineGvcfs(list, groupedOutputFile)
        (job, groupedOutputFile)
      }).toList
      getCombineGvcfs(bla.map(_._2), outputFile) ::: bla.flatMap(_._1)
    } else {
      val cg = new CombineGVCFs(this)
      cg.variant = inputFiles
      cg.out = outputFile
      cg.isIntermediate = isIntermediate
      List(cg)
    }
  }
}

object GenotypeGvcfs extends PipelineCommand
