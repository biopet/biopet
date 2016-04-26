package nl.lumc.sasc.biopet.core

/**
 * Created by pjvan_thof on 4/26/16.
 */
trait ScatterGatherableFunction extends BiopetCommandLineFunction
  with org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction {
  scatterCount = config("scatter_count", freeVar = true, default = 1)
}
