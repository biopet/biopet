package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.FullVersion

/**
 * Created by pjvanthof on 11/09/15.
 */
trait ToolCommandFuntion extends BiopetJavaCommandLineFunction {
  override def getVersion = Some("Biopet " + FullVersion)
}
