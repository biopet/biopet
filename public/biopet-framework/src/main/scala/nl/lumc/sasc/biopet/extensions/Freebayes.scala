package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 3/3/15.
 */
class Freebayes(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(required = true)
  var bamfiles: List[File] = Nil

  @Input(required = true)
  var reference: File = config("reference")

  @Output(required = true)
  var outputVcf: File = null

  var ploidy: Option[Int] = config("ploidy")

  executable = config("exe", default = "freebayes")
  override val versionRegex = """version:  (.*)""".r
  override def versionCommand = executable + " --version"

  def cmdLine = executable +
    required("--fasta-reference", reference) +
    repeat("--bam", bamfiles) +
    optional("--vcf", outputVcf) +
    optional("--ploidy", ploidy)
}
