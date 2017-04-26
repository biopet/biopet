/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.core

/**
  * Created by pjvan_thof on 4/26/16.
  */
trait ScatterGatherableFunction
    extends BiopetCommandLineFunction
    with org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction {
  scatterCount = config("scattercount", freeVar = true, default = 1)
}
