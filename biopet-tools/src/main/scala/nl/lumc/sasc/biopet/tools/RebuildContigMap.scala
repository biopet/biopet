package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, FastaUtils, ToolCommand}

/**
  * Created by pjvanthof on 30/05/2017.
  */
object RebuildContigMap extends ToolCommand {

  case class Args(inputContigMap: File = null,
                  outputContigMap: File = null,
                  referenceFasta: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputContigMap") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputContigMap = x)
    } text "Input contig map"
    opt[File]('o', "outputContigMap") required () unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(outputContigMap = x)
    } text "output contig map"
    opt[File]('R', "referenceFasta") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    } text "Reference fasta file"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val newMap = FastaUtils.rebuildContigMap(cmdargs.inputContigMap, cmdargs.referenceFasta)

    val writer = new PrintWriter(cmdargs.outputContigMap)
    writer.println("#Name_in_fasta\tAlternative_names")
    for ((contigName, alternitiveNames) <- newMap) {
      writer.println(contigName + "\t" + alternitiveNames.mkString(";"))
    }
    writer.close()
    logger.info("Done")
  }
}
