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

import org.broadinstitute.gatk.queue.util.{ Logging => GatkLogging }
import java.io.File
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.core.workaround.BiopetQCommandLine

trait PipelineCommand extends MainCommand with GatkLogging {

  def pipeline = "/" + getClass.getName.stripSuffix("$").replaceAll("\\.", "/") + ".class"

  def main(args: Array[String]): Unit = {
    val argsSize = args.size
    for (t <- 0 until argsSize) {
      if (args(t) == "-config" || args(t) == "--config_file") {
        if (t >= argsSize) throw new IllegalStateException("-config needs a value")
        Config.global.loadConfigFile(new File(args(t + 1)))
      }
    }

    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}