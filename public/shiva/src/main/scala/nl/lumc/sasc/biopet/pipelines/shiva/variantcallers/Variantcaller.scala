package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.core.{ BiopetQScript, Reference }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 11/19/15.
 */
trait Variantcaller extends QScript with BiopetQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = _

  var inputBams: Map[String, File] = _

  def init() = {}

  /** Prio in merging  in the final file */
  protected def defaultPrio: Int

  /** Prio from the config */
  lazy val prio: Int = config("prio_" + name, default = defaultPrio)

  /** This should add the variantcaller jobs */
  def script

  /** Final output file of this mode */
  def outputFile: File = new File(outputDir, namePrefix + s".$name.vcf.gz")
}

