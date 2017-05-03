package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.{BiopetQScript, PipelineCommand, Reference}
import nl.lumc.sasc.biopet.extensions.{Ln, gatk}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable.ListBuffer

/**
  * Created by pjvan_thof on 26-4-17.
  */
class GenotypeGvcfs(val parent: Configurable) extends QScript with BiopetQScript with Reference {
  qscript =>

  def this() = this(null)

  @Input(required = true, shortName = "V")
  var inputGvcfs: List[File] = Nil

  val namePrefix: String = config("name_prefix", default = "multisample")

  val maxNumberOfFiles: Int = config("max_number_of_files", default = 10)

  def finalGvcfFile = new File(outputDir, s"$namePrefix.gvcf.vcf.gz")
  def finalVcfFile = new File(outputDir, s"$namePrefix.vcf.gz")

  /** Init for pipeline */
  def init(): Unit = {}

  /** Pipeline itself */
  def biopetScript(): Unit = {
    inputGvcfs.foreach(inputFiles :+= InputFile(_))

    if (inputGvcfs.size > 1) {
      val combineJob = new CombineJob(finalGvcfFile, outputDir, inputGvcfs)
      combineJob.allJobs.foreach(add(_))
    } else
      inputGvcfs.headOption.foreach { file =>
        add(Ln(this, file, finalGvcfFile))
        add(Ln(this, file + ".tbi", finalGvcfFile + ".tbi"))
      }

    val genotype = new gatk.GenotypeGVCFs(this)
    genotype.variant = List(finalGvcfFile)
    genotype.out = finalVcfFile
    add(genotype)
  }

  class CombineJob(outputFile: File, outputDir: File, allInput: List[File], group: List[Int] = Nil) {
    val job: gatk.CombineGVCFs = new gatk.CombineGVCFs(qscript)
    job.out = outputFile
    job.isIntermediate = group.nonEmpty
    val subJobs: ListBuffer[CombineJob] = ListBuffer()
    val groupedInput = makeEqualGroups(allInput)
    if (groupedInput.size == 1) job.variant = groupedInput.head
    else {
      for ((list, i) <- groupedInput.zipWithIndex) {
        val tempFile = new File(outputDir,
                                ".grouped" + File.separator + group
                                  .mkString(File.separator) + File.separator + s"$i.g.vcf.gz")
        subJobs += new CombineJob(tempFile, outputDir, list, group ::: i :: Nil)
        job.variant :+= tempFile
      }
    }

    def makeEqualGroups(files: List[File]): List[List[File]] = {
      val groupSize = if (files.size > (maxNumberOfFiles * maxNumberOfFiles)) {
        files.size / maxNumberOfFiles
      } else if (files.size < maxNumberOfFiles) files.size
      else files.size / (files.size / maxNumberOfFiles)
      files.grouped(groupSize).toList
    }

    def allJobs: List[gatk.CombineGVCFs] = {
      job :: subJobs.flatMap(_.allJobs).toList
    }
  }
}

object GenotypeGvcfs extends PipelineCommand
