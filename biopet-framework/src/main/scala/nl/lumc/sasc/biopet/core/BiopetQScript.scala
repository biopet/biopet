package nl.lumc.sasc.biopet.core

//import org.broadinstitute.sting.queue.QScript
import java.io.File
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.sting.commandline._
import org.broadinstitute.sting.queue.function.QFunction

trait BiopetQScript extends Configurable {
  @Argument(doc="Config Json file",shortName="config", required=false)
  val configfiles: List[File] = Nil
  
  @Argument(doc="Output directory", shortName="outputDir", required=true)
  var outputDir: String = _
  
  var outputFiles:Map[String,File] = Map()
  
  def init
  def biopetScript
  
  final def script() {
    init
    biopetScript
    // TODO: Config report
  }
  
  def add(functions: QFunction*) // Gets implemeted at org.broadinstitute.sting.queue.QScript
  def add(function: QFunction, isIntermediate:Boolean = false) {
    function.isIntermediate = isIntermediate
    add(function)
  }
  
  
}
