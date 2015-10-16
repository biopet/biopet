package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.FullVersion

/**
 * Created by pjvanthof on 11/09/15.
 */
trait ToolCommandFuntion extends BiopetJavaCommandLineFunction {
  def toolObject: Object

  override def getVersion = Some("Biopet " + FullVersion)

  override def beforeGraph(): Unit = {
    javaMainClass = toolObject.getClass.getName.takeWhile(_ != '$')
    super.beforeGraph()
  }

  override def freezeFieldValues(): Unit = {
    javaMainClass = toolObject.getClass.getName.takeWhile(_ != '$')
    super.freezeFieldValues()
  }
}
