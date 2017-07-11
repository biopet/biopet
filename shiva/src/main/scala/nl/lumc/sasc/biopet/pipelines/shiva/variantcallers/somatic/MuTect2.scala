package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsReheader
import nl.lumc.sasc.biopet.extensions.gatk.{BqsrGather, CombineVariants}
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.utils.{IoUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

class MuTect2(val parent: Configurable) extends SomaticVariantCaller {

  def name = "mutect2"

  override val mergeVcfResults: Boolean = false

  def defaultPrio: Int = -1

  lazy val runConEst: Boolean = config("run_contest", default = false)

  def biopetScript(): Unit = {

    if (tnPairs.isEmpty) Logging.addError("No tumor-normal found in config")
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
      inputBams.get(pair.tumorSample).foreach(muTect2.input_file :+= TaggedFile(_, "tumor"))
      inputBams.get(pair.normalSample).foreach(muTect2.input_file :+= TaggedFile(_, "normal"))
      muTect2.BQSR = bqsrFile

      if (runConEst) {
        val namePrefix = outputFile.getAbsolutePath.stripSuffix(".vcf.gz")
        val contEst = new gatk.ContEst(this)
        inputBams.get(pair.tumorSample).foreach(contEst.input_file :+= TaggedFile(_, "eval"))
        inputBams.get(pair.normalSample).foreach(contEst.input_file :+= TaggedFile(_, "genotype"))
        contEst.output = new File(s"$namePrefix.contamination.txt")
        contEst.BQSR = bqsrFile
        add(contEst)

        val contaminationPerSample: File = new File(s"$namePrefix.contamination.short.txt")
        val awk: Awk = Awk(this, "BEGIN{OFS=\"\\t\"}{if($1 != \"name\") print $1,$4;}")
        awk.input = contEst.output
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

      outputDir.mkdirs()
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
