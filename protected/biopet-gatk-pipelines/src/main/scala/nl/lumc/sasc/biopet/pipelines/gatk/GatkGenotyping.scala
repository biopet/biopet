/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.gatk.broad.{ GenotypeGVCFs, SelectVariants }
import org.broadinstitute.gatk.queue.QScript

class GatkGenotyping(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Gvcf files", shortName = "I")
  var inputGvcfs: List[File] = Nil

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = config("reference")

  @Argument(doc = "Dbsnp", shortName = "dbsnp", required = false)
  var dbsnp: File = config("dbsnp")

  @Argument(doc = "OutputName", required = false)
  var outputName: String = "genotype"

  @Output(doc = "OutputFile", shortName = "O", required = false)
  var outputFile: File = _

  @Argument(doc = "Samples", shortName = "sample", required = false)
  var samples: List[String] = Nil

  def init() {
    require(outputName != null, "Outputname is null")
    if (outputFile == null) outputFile = new File(outputDir, outputName + ".vcf.gz")
  }

  def biopetScript() {
    addGenotypeGVCFs(inputGvcfs, outputFile)
    if (samples.nonEmpty) {
      if (samples.size > 1) addSelectVariants(outputFile, samples, new File(outputDir, "samples/"), "all")
      for (sample <- samples) addSelectVariants(outputFile, List(sample), new File(outputDir, "samples/"), sample)
    }
  }

  def addGenotypeGVCFs(gvcfFiles: List[File], outputFile: File): File = {
    val genotypeGVCFs = GenotypeGVCFs(this, gvcfFiles, outputFile)
    add(genotypeGVCFs)
    genotypeGVCFs.out
  }

  def addSelectVariants(inputFile: File, samples: List[String], outputDir: File, name: String) {
    val selectVariants = SelectVariants(this, inputFile, new File(outputDir, name + ".vcf.gz"))
    selectVariants.excludeNonVariants = true
    for (sample <- samples) selectVariants.sample_name :+= sample
    add(selectVariants)
  }
}

object GatkGenotyping extends PipelineCommand
