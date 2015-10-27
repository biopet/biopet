package nl.lumc.sasc.biopet/pipelines.mypipeline

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Fastqc
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

class HelloPipeline(val root: Configurable) extends QScript with SummaryQScript {
  def this() = this(null)

  /** Only required when using [[SummaryQScript]] */
  def summaryFile = new File(outputDir, "hello.summary.json")

  /** Only required when using [[SummaryQScript]] */
  def summaryFiles: Map[String, File] = Map()

  /** Only required when using [[SummaryQScript]] */
  def summarySettings = Map()

  // This method can be used to initialize some classes where needed
  def init(): Unit = {
  }

  // This method is the actual pipeline
  def biopetScript: Unit = {

    // Executing a tool like FastQC, calling the extension in `nl.lumc.sasc.biopet.extensions.Fastqc`

    val fastqc = new Fastqc(this)
    fastqc.fastqfile = config("fastqc_input")
    fastqc.output = new File(outputDir,

    /* Only required when using [[SummaryQScript]] */
    addSummaryQScript(shiva)

    // From here you can use the output files of shiva as input file of other jobs
  }
}

//TODO: Replace object Name, must be the same as the class of the pipeline
object HelloPipeline extends PipelineCommand