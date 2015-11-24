package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, BiopetQScript}
import nl.lumc.sasc.biopet.extensions.breakdancer.Breakdancer
import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.extensions.delly.Delly
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 23/11/15.
  */
trait SvCaller extends QScript with BiopetQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = _

  var inputBams: Map[String, File] = _

  def init() = {}
}

class Breakdancer(val root: Configurable) extends SvCaller {
  def name = "breakdancer"

  def biopetScript() {
    //TODO: move minipipeline of breakdancer to here
    for ((sample, bamFile) <- inputBams) {
      val breakdancerDir = new File(outputDir, sample)
      val breakdancer = Breakdancer(this, bamFile, breakdancerDir)
      addAll(breakdancer.functions)
    }
  }
}

/** default mode of bcftools */
class Clever(val root: Configurable) extends SvCaller {
  def name = "clever"

  def biopetScript() {
    //TODO: check double directories
    for ((sample, bamFile) <- inputBams) {
      val cleverDir = new File(outputDir, sample)
      val clever = CleverCaller(this, bamFile, cleverDir)
      add(clever)
    }
  }
}

/** Makes a vcf file from a mpileup without statistics */
class Delly(val root: Configurable) extends SvCaller {
  def name = "delly"

  def biopetScript() {
    //TODO: Move mini delly pipeline to here
    for ((sample, bamFile) <- inputBams) {
      val dellyDir = new File(outputDir, sample)
      val delly = Delly(this, bamFile, dellyDir)
      delly.outputName = sample
      addAll(delly.functions)
    }
  }
}
