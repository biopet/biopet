package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.delly.DellyCaller
import nl.lumc.sasc.biopet.extensions.gatk.CatVariants
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caller delly */
class Delly(val root: Configurable) extends SvCaller {
  def name = "delly"

  val del: Boolean = config("DEL", default = true)
  val dup: Boolean = config("DUP", default = true)
  val inv: Boolean = config("INV", default = true)
  val tra: Boolean = config("TRA", default = true)

  def biopetScript() {
    for ((sample, bamFile) <- inputBams) {
      val dellyDir = new File(outputDir, sample)

      val catVariants = new CatVariants(this)
      catVariants.outputFile = new File(dellyDir, sample + ".delly.vcf.gz")

      if (del) {
        val delly = new DellyCaller(this)
        delly.input = bamFile
        delly.analysistype = "DEL"
        delly.outputvcf = new File(dellyDir, sample + ".delly.del.vcf")
        add(delly)
        catVariants.inputFiles :+= delly.outputvcf
      }
      if (dup) {
        val delly = new DellyCaller(this)
        delly.input = bamFile
        delly.analysistype = "DUP"
        delly.outputvcf = new File(dellyDir, sample + ".delly.dup.vcf")
        add(delly)
        catVariants.inputFiles :+= delly.outputvcf
      }
      if (inv) {
        val delly = new DellyCaller(this)
        delly.input = bamFile
        delly.analysistype = "INV"
        delly.outputvcf = new File(dellyDir, sample + ".delly.inv.vcf")
        add(delly)
        catVariants.inputFiles :+= delly.outputvcf
      }
      if (tra) {
        val delly = new DellyCaller(this)
        delly.input = bamFile
        delly.analysistype = "TRA"
        delly.outputvcf = new File(dellyDir, sample + ".delly.tra.vcf")
        catVariants.inputFiles :+= delly.outputvcf
        add(delly)
      }

      require(catVariants.inputFiles.nonEmpty, "Must atleast 1 SV-type be selected for Delly")

      add(catVariants)
      addVCF(sample, catVariants.outputFile)
    }
  }
}
