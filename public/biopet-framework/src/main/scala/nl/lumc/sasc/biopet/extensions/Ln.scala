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

import java.io.File
import scala.sys.process.{ Process, ProcessLogger }
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.config.Configurable

class Ln(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "Input file")
  var in: File = _

  @Output(doc = "Link destination")
  var out: File = _

  var relative: Boolean = true

  override def freezeFieldValues(): Unit = {
    val outLog: String = ".%s.%s.out".format(out.getName, analysisName)
    jobOutputFile = new File(out.getAbsoluteFile.getParentFile, outLog)
    super.freezeFieldValues()
  }

  lazy val cmd: String = {
    lazy val inCanonical: String = {
      // need to remove "/~" to correctly expand path with tilde
      in.getCanonicalPath().replace("/~", "")
    }

    lazy val outCanonical: String = {
      out.getCanonicalPath().replace("/~", "")
    }

    lazy val inToks: Array[String] = {
      inCanonical.split(File.separator)
    }

    lazy val outToks: Array[String] = {
      outCanonical.split(File.separator)
    }

    lazy val commonPrefixLength: Int = {
      val maxLength = scala.math.min(inToks.length, outToks.length)
      var i: Int = 0;
      while (i < maxLength && inToks(i) == outToks(i)) i += 1;
      i
    }

    lazy val inUnique: String = {
      inToks.slice(commonPrefixLength, inToks.length).mkString(File.separator)
    }

    lazy val outUnique: String = {
      outToks.slice(commonPrefixLength, outToks.length).mkString(File.separator)
    }

    lazy val inRelative: String = {
      // calculate 'distance' from output directory to input
      // which is the number of directory walks required to get to the inUnique directory from outDir
      val dist =
        // relative path differs depending on which of the input or target is in the 'higher' directory
        if (inToks.length > outToks.length)
          scala.math.max(0, inUnique.split(File.separator).length - 1)
        else
          scala.math.max(0, outUnique.split(File.separator).length - 1)

      val result =
        if (dist == 0 || inToks.length > outToks.length)
          inUnique
        else
          ((".." + File.separator) * dist) + inUnique

      result
    }

    if (relative) {
      // workaround until we have `ln` that works with relative path (i.e. `ln -r`)
      "ln -s " + inRelative + " " + outCanonical
    } else {
      "ln -s " + inCanonical + " " + outCanonical
    }
  }

  override def run {
    val stdout = new StringBuffer()
    val stderr = new StringBuffer()
    val process = Process(cmd).run(ProcessLogger(stdout append _ + "\n", stderr append _ + "\n"))
    val exitcode = process.exitValue
    if (exitcode != 0) {
      throw new Exception("Error creating symbolic link, this was the original message: \n" + stderr)
    }
    logger.info("cmd: '" + cmd + "', exitcode: " + exitcode)
  }
}

object Ln {
  def apply(root: Configurable, input: File, output: File, relative: Boolean = true): Ln = {
    val ln = new Ln(root)
    ln.in = input
    ln.out = output
    ln.relative = relative
    return ln
  }
}
