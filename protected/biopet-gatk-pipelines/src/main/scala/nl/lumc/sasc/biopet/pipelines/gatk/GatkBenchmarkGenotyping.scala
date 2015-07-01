/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import org.broadinstitute.gatk.queue.QScript

import scala.util.Random

class GatkBenchmarkGenotyping(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Sample gvcf file")
  var sampleGvcf: File = _

  @Argument(doc = "SampleName", required = true)
  var sampleName: String = _

  @Input(doc = "Gvcf files", shortName = "I", required = false)
  var gvcfFiles: List[File] = Nil

  var reference: File = config("reference")

  @Argument(doc = "Dbsnp", shortName = "dbsnp", required = false)
  var dbsnp: File = config("dbsnp")

  def init() {
    if (config.contains("gvcffiles")) for (file <- config("gvcffiles").asList)
      gvcfFiles ::= file.toString
  }

  def biopetScript() {
    var todoGvcfs = gvcfFiles
    var gvcfPool: List[File] = Nil
    addGenotypingPipeline(gvcfPool)

    while (todoGvcfs.size > 0) {
      val index = Random.nextInt(todoGvcfs.size)
      gvcfPool ::= todoGvcfs(index)
      addGenotypingPipeline(gvcfPool)
      todoGvcfs = todoGvcfs.filter(b => b != todoGvcfs(index))
    }
  }

  def addGenotypingPipeline(gvcfPool: List[File]) {
    val gatkGenotyping = new GatkGenotyping(this)
    gatkGenotyping.inputGvcfs = sampleGvcf :: gvcfPool
    gatkGenotyping.samples :+= sampleName
    gatkGenotyping.outputDir = new File(outputDir, "samples_" + gvcfPool.size)
    gatkGenotyping.init
    gatkGenotyping.biopetScript
    addAll(gatkGenotyping.functions)
  }
}

object GatkBenchmarkGenotyping extends PipelineCommand
