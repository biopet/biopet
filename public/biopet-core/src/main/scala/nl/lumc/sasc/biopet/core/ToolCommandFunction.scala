package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.FullVersion

/**
 * Created by pjvanthof on 11/09/15.
 */
trait ToolCommandFunction extends BiopetJavaCommandLineFunction with Version {
  def toolObject: Object

  def versionCommand = ""
  def versionRegex = "".r

  override def getVersion = Some("Biopet " + FullVersion)

  override def beforeGraph(): Unit = {
    javaMainClass = toolObject.getClass.getName.takeWhile(_ != '$')
    super.beforeGraph()
  }
}
