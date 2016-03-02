package nl.lumc.sasc.biopet.extensions.samtools

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by sajvanderzeeuw on 19-1-16.
 */
class FixMpileup(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("fix_iupac_mpileup.py", "/nl/lumc/sasc/biopet/extensions/samtools/")
  def cmdLine = getPythonCommand
}
