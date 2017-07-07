package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.extensions.gatk.BqsrGather
import nl.lumc.sasc.biopet.extensions.{Awk, Tabix, gatk}
import nl.lumc.sasc.biopet.utils.config.Configurable

class MuTect2(val parent: Configurable) extends SomaticVariantCaller {

  def name = "mutect2"

  override val mergeVcfResults: Boolean = false

  // currently not relevant, at the moment only one somatic variant caller exists in Biopet
  // and results from this won't be merged together with the results from other methods
  def defaultPrio = -1

  val runConEst = config("run_contest", default = false)

  def biopetScript(): Unit = {

    for (pair <- tnPairs)
      addJobsForPair(pair, new File(outputDir, s"${pair.tumorSample}-${pair.normalSample}.$name.vcf.gz"))
  }


  def addJobsForPair(pair: TumorNormalPair, outFile: File): Unit = {
    val muTect2 = {
      gatk.MuTect2(this, inputBams(pair.tumorSample), inputBams(pair.normalSample), outFile)
    }

    if (runConEst) {
      val namePrefix = outFile.getAbsolutePath.stripSuffix(".vcf.gz")
      val contEstOutput: File = new File(s"$namePrefix.contamination.txt")
      val contEst = gatk.ContEst(this,
        inputBams(pair.tumorSample),
        inputBams(pair.normalSample),
        contEstOutput)
      add(contEst)

      val contaminationPerSample: File = new File(s"$namePrefix.contamination.short.txt")
      val awk: Awk = Awk(this, "BEGIN{OFS=\"\\t\"}{if($1 != \"name\") print $1,$4;}")
      awk.input = contEstOutput
      add(awk > contaminationPerSample)

      muTect2.contaminationFile = Some(contaminationPerSample)
    }

    if (inputBqsrFiles.contains(pair.tumorSample) && inputBqsrFiles.contains(pair.normalSample)) {
      val gather = new BqsrGather()
      gather.inputBqsrFiles = List(inputBqsrFiles(pair.tumorSample), inputBqsrFiles(pair.normalSample))
      gather.outputBqsrFile =  new File(swapExt(outFile, "vcf.gz","bqsr.merge"))
      add(gather)

      muTect2.BQSR = Some(gather.outputBqsrFile)
    }

    add(muTect2)
    add(Tabix(this, outFile))
  }
}