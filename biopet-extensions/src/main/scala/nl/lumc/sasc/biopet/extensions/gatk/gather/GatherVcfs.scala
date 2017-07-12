package nl.lumc.sasc.biopet.extensions.gatk.gather

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.extensions.gatk.CommandLineGATK
import nl.lumc.sasc.biopet.extensions.picard
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.queue.function.scattergather.GatherFunction

/**
  * Created by pjvan_thof on 12-7-17.
  */
class GatherVcfs extends picard.GatherVcfs(null) with GatherFunction {
  analysisName = "Gather_CatVariants"

  override val parent = originalFunction match {
    case b: BiopetCommandLineFunction => b
    case _                            => null
  }

  override def freezeFieldValues() {
    val originalGATK = this.originalFunction.asInstanceOf[CommandLineGATK]

    this.input = this.gatherParts.zipWithIndex map { case (input, index) => new TaggedFile(input, "input" + index) }
    this.output = this.originalOutput

    super.freezeFieldValues()
  }

}
