package nl.lumc.sasc.biopet.tools

import java.io.File

import ncsa.hdf.`object`.h5.H5File
import nl.lumc.sasc.biopet.tools.GvcfToBed.Args
import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}

/**
  * Created by pjvanthof on 26/01/2017.
  */
object JsonToHdf5 extends ToolCommand {

  case class Args(inputJson: File = null,
                  outputHdf5: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputJson") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputJson = x)
    } text "Input json file"
    opt[File]('o', "outputHdf5") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputHdf5 = x)
    } text "Output hdf5 file"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val jsonMap = ConfigUtils.fileToConfigMap(cmdArgs.inputJson)

    val hdf5 = new H5File(cmdArgs.outputHdf5.getAbsolutePath)

  }

}
