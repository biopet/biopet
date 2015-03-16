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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core

/**
 * This class is for commandline programs where the executable is a non JVM based program
 */
abstract class BiopetCommandLineFunction extends BiopetCommandLineFunctionTrait {
  /**
   * This function needs to be implemented to define the command that is executed
   * @return Command to run
   */
  protected def cmdLine: String

  /**
   * implementing a final version of the commandLine from org.broadinstitute.gatk.queue.function.CommandLineFunction
   * User needs to implement cmdLine instead
   * @return Command to run
   */
  final def commandLine: String = {
    preCmdInternal
    val cmd = cmdLine
    addJobReportBinding("command", cmd)
    return cmd
  }
}
