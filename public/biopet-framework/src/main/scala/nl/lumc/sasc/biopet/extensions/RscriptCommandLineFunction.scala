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
package nl.lumc.sasc.biopet.extensions

import java.io.{ FileOutputStream, File }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import scala.sys.process._

/**
 * Created by wyleung on 17-2-15.
 */
trait RscriptCommandLineFunction extends BiopetCommandLineFunction {

  protected var script: File

  executable = config("exe", default = "Rscript", submodule = "Rscript")

  override def beforeGraph: Unit = {
    checkScript()
  }

  def checkScript(local: Boolean = false): Unit = {
    if (script.exists()) {
      script = script.getAbsoluteFile
    } else {
      val rScript: File = {
        if (local) File.createTempFile(script.getName, ".R")
        else new File(".queue/tmp/" + script)
      }
      if (!rScript.getParentFile.exists) rScript.getParentFile.mkdirs

      val is = getClass.getResourceAsStream(script.getPath)
      val os = new FileOutputStream(rScript)

      org.apache.commons.io.IOUtils.copy(is, os)
      os.close()

      script = rScript
    }
  }

  def runLocal(logger: ProcessLogger): Unit = {
    checkScript(local = true)

    Process(cmdLine).run(logger)
  }

  def cmdLine: String = {
    required(executable) +
      required(script)
  }
}
