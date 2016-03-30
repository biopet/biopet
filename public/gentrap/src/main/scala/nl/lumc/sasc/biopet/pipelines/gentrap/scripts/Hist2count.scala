package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvan_thof on 3/30/16.
 */
class Hist2count(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("hist2count.py", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")

  def cmdLine = getPythonCommand + optional("-c", "3")
}
