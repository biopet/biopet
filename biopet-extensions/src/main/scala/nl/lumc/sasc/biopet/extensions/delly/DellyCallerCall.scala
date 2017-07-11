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
package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  *
  * Created by imoustakas on 9-5-17.
  */
class DellyCallerCall(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Version
    with Reference {
  executable = config("exe", default = "delly")

  private lazy val versionexecutable: File = new File(executable)

  override def defaultThreads = 1
  override def defaultCoreMemory = 4.0

  def versionCommand = versionexecutable.getAbsolutePath
  def versionRegex = """D(ELLY|elly) \(Version: (.*)\)""".r
  override def versionExitcode = List(0, 1)
  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference file", required = true)
  var reference: File = _

  @Output(doc = "Delly BCF output")
  var outputbcf: File = _

  @Argument(doc = " SV type: DEL, DUP, INV, BND, INS")
  var analysistype: String = _

  // Rest of available arguments for the call command of delly
  var fileformat: Option[String] = config("fileformat")
  var gsize: Option[Float] = config("gsize")
  var keepdup: Boolean = config("keep-dup", default = false)
  var buffersize: Option[Int] = config("buffer-size")
  var outputdir: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  def cmdLine =
    required(executable) +
      required("call") +
      required("-t", analysistype) + // SV type (DEL, DUP, INV, BND, INS)
      required("-o", outputbcf) + // delly BCF output
      required("-g", reference) + // reference file, on which the reads are aligned
      required(input) // BAM file with the alignments

}
