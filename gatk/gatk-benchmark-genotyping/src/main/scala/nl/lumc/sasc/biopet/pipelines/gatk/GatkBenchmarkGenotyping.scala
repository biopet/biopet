package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import scala.util.Random

class GatkBenchmarkGenotyping(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Sample gvcf file")
  var sampleGvcf: File = _

  @Argument(doc = "SampleName", required = true)
  var sampleName: String = _

  @Input(doc = "Gvcf files", shortName = "I", required = false)
  var gvcfFiles: List[File] = Nil

  @Argument(doc = "Reference", shortName = "R", required = false)
  var reference: File = _

  @Argument(doc = "Dbsnp", shortName = "dbsnp", required = false)
  var dbsnp: File = _

  def init() {
    if (configContains("gvcffiles")) for (file <- config("gvcffiles").getList) {
      gvcfFiles ::= file.toString
    }
    if (reference == null) reference = config("reference")
    if (dbsnp == null) dbsnp = config("dbsnp")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
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
    gatkGenotyping.outputDir = outputDir + "samples_" + gvcfPool.size + "/"
    gatkGenotyping.init
    gatkGenotyping.biopetScript
    addAll(gatkGenotyping.functions)
  }
}

object GatkBenchmarkGenotyping extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkBenchmarkGenotyping.class"
}
