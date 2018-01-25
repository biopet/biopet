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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.sys.process.{Process, ProcessLogger}

/**
  * This class can execute ln as InProcessFunction or used to only generate the ln command
  */
class Ln(val parent: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "Link destination")
  var output: File = _

  @Input(required = false)
  var deps: List[File] = Nil

  var relative: Boolean = true

  /** Generate out file for job */
  override def freezeFieldValues(): Unit = {
    if (output.getAbsolutePath == "/home/pjvan_thof/test/output/samples/sample2/lib_lib1/sample2-lib1.dedup.bam") {
      ""
    }

    val outLog: String = ".%s.%s.out".format(output.getName, analysisName)
    jobOutputFile = new File(output.getAbsoluteFile.getParentFile, outLog)
    super.freezeFieldValues()
  }

  /** return commandline to execute */
  lazy val cmd: String = {
    val inCanonical: String = {
      // need to remove "/~" to correctly expand path with tilde
      input.getAbsolutePath.replace("/~", "")
    }

    val outCanonical: String = output.getAbsolutePath.replace("/~", "")

    if (relative) {
      val inToks: Array[String] = inCanonical.split(File.separator)

      val outToks: Array[String] = outCanonical.split(File.separator)

      val commonPrefixLength: Int = {
        val maxLength = scala.math.min(inToks.length, outToks.length)
        var i: Int = 0
        while (i < maxLength && inToks(i) == outToks(i)) i += 1
        i
      }

      val inUnique = inToks.slice(commonPrefixLength, inToks.length)

      val outUnique = outToks.slice(commonPrefixLength, outToks.length)

      val inRelative: String =
        ((".." + File.separator) * (outUnique.length - 1)) + inUnique.mkString(File.separator)

      // workaround until we have `ln` that works with relative path (i.e. `ln -r`)
      "ln -s " + inRelative + " " + outCanonical
    } else {
      "ln -s " + inCanonical + " " + outCanonical
    }
  }

  override def run() {
    val stdout = new StringBuffer()
    val stderr = new StringBuffer()
    val process = Process(cmd).run(ProcessLogger(stdout append _ + "\n", stderr append _ + "\n"))
    val exitcode = process.exitValue()
    if (exitcode != 0) {
      throw new Exception(
        "Error creating symbolic link, this was the original message: \n" + stderr)
    }
    logger.info("cmd: '" + cmd + "', exitcode: " + exitcode)
  }
}

/** Object for constructors for ln */
object Ln {

  /**
    * Basis constructor
    * @param root root object for config
    * @param input list of files to use
    * @param output output File
    * @param relative make reletive links (default true)
    * @return
    */
  def apply(root: Configurable, input: File, output: File, relative: Boolean = true): Ln = {
    if (output.getAbsolutePath == "/home/pjvan_thof/test/output/samples/sample2/lib_lib1/sample2-lib1.dedup.bam") {
      ""
    }

    val ln = new Ln(root)
    ln.input = input
    ln.output = output
    ln.relative = relative
    ln
  }

  def linkBamFile(root: Configurable,
                  input: File,
                  output: File,
                  index: Boolean = true,
                  relative: Boolean = true): List[Ln] = {
    val bamLn = Ln(root, input, output, relative)
    bamLn :: (if (index) {
                val inputIndex = new File(input.getAbsolutePath.stripSuffix(".bam") + ".bai")
                val outputIndex = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")
                List(Ln(root, inputIndex, outputIndex, relative))
              } else Nil)
  }
}
