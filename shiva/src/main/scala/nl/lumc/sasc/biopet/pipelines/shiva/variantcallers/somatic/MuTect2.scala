package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsReheader
import nl.lumc.sasc.biopet.extensions.gatk.{BqsrGather, CombineVariants}
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.utils.IoUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

class MuTect2(val parent: Configurable) extends SomaticVariantCaller {

  def name = "mutect2"

  override val mergeVcfResults: Boolean = false

  def defaultPrio: Int = -1

  lazy val runConEst: Boolean = config("run_contest", default = false)

  def biopetScript(): Unit = {

    val outputFiles = for (pair <- tnPairs) yield {

      val bqsrFile =
        if (inputBqsrFiles.contains(pair.tumorSample) &&
            inputBqsrFiles.contains(pair.normalSample)) {
          val gather = new BqsrGather()
          gather.inputBqsrFiles =
            List(inputBqsrFiles(pair.tumorSample), inputBqsrFiles(pair.normalSample))
          gather.outputBqsrFile =
            new File(outputDir, s"${pair.tumorSample}-${pair.normalSample}.bqsr")
          add(gather)

          Some(gather.outputBqsrFile)
        } else None

      val outputFile =
        new File(outputDir, s"${pair.tumorSample}-${pair.normalSample}.$name.vcf.gz")

      val muTect2 = new gatk.MuTect2(this)
      muTect2.input_file :+= TaggedFile(inputBams(pair.tumorSample), "tumor")
      muTect2.input_file :+= TaggedFile(inputBams(pair.normalSample), "normal")
      muTect2.BQSR = bqsrFile

      if (runConEst) {
        val namePrefix = outputFile.getAbsolutePath.stripSuffix(".vcf.gz")
        val contEstOutput: File = new File(s"$namePrefix.contamination.txt")
        val contEst = gatk.ContEst(this,
                                   inputBams(pair.tumorSample),
                                   inputBams(pair.normalSample),
                                   contEstOutput)
        contEst.BQSR = bqsrFile
        add(contEst)

        val contaminationPerSample: File = new File(s"$namePrefix.contamination.short.txt")
        val awk: Awk = Awk(this, "BEGIN{OFS=\"\\t\"}{if($1 != \"name\") print $1,$4;}")
        awk.input = contEstOutput
        add(awk > contaminationPerSample)

        muTect2.contaminationFile = Some(contaminationPerSample)
      }

      if (inputBqsrFiles.contains(pair.tumorSample) && inputBqsrFiles
            .contains(pair.normalSample)) {
        val gather = new BqsrGather()
        gather.inputBqsrFiles =
          List(inputBqsrFiles(pair.tumorSample), inputBqsrFiles(pair.normalSample))
        gather.outputBqsrFile = new File(swapExt(outputFile, "vcf.gz", "bqsr.merge"))
        add(gather)

        muTect2.BQSR = Some(gather.outputBqsrFile)
      }

      val renameFile = new File(outputDir, s".rename.${pair.tumorSample}-${pair.normalSample}.txt")
      IoUtils.writeLinesToFile(renameFile,
                               List(
                                 s"TUMOR ${pair.tumorSample}",
                                 s"NORMAL ${pair.normalSample}"
                               ))

      val pipe = muTect2 | BcftoolsReheader(this, renameFile) | new Bgzip(this) > outputFile
      pipe.threadsCorrection = -2
      add(pipe)
      add(Tabix(this, outputFile))

      outputFile
    }

    if (outputFiles.size > 1) {
      add(CombineVariants(this, outputFiles, outputFile))
    } else if (outputFiles.nonEmpty) {
      add(Ln(this, outputFiles.head, outputFile))
      add(Ln(this, outputFiles.head + ".tbi", outputFile + ".tbi"))
    }
  }
}
