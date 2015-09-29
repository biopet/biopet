package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvan_thof on 9/29/15.
 */
class BiopetFifoPipe(val root: Configurable,
                     protected var commands: List[BiopetCommandLineFunction]) extends BiopetCommandLineFunction {

  def fifos: List[File] = {
    val outputs: Map[BiopetCommandLineFunction, Seq[File]] = try {
      commands.map(x => x -> x.outputs).toMap
    } catch {
      case e: NullPointerException => Map()
    }

    val inputs: Map[BiopetCommandLineFunction, Seq[File]] = try {
      commands.map(x => x -> x.inputs).toMap
    } catch {
      case e: NullPointerException => Map()
    }

    for (
      cmdOutput <- commands;
      cmdInput <- commands if cmdOutput != cmdInput && outputs.contains(cmdOutput);
      outputFile <- outputs(cmdOutput) if inputs.contains(cmdInput);
      inputFile <- inputs(cmdInput) if outputFile == inputFile
    ) yield outputFile
  }

  override def beforeGraph(): Unit = {
    val outputs: Map[BiopetCommandLineFunction, Seq[File]] = try {
      commands.map(x => x -> x.outputs).toMap
    } catch {
      case e: NullPointerException => Map()
    }

    val inputs: Map[BiopetCommandLineFunction, Seq[File]] = try {
      commands.map(x => x -> x.inputs).toMap
    } catch {
      case e: NullPointerException => Map()
    }

    val fifoFiles = fifos

    outputFiles = outputs.values.toList.flatten.filter(!fifoFiles.contains(_))
    outputFiles = outputFiles.distinct

    deps = inputs.values.toList.flatten.filter(!fifoFiles.contains(_))
    deps = deps.distinct
  }

  override def beforeCmd(): Unit = {
    commands.foreach{ cmd =>
      cmd.beforeGraph()
      cmd.internalBeforeGraph()
      cmd.beforeCmd()
    }
  }

  def cmdLine = {
    val fifosFiles = this.fifos
    fifosFiles.filter(_.exists()).map(required("rm", _)).mkString("\n\n", " \n", " \n\n") +
      fifosFiles.map(required("mkfifo", _)).mkString("\n\n", "\n", "\n\n") +
      commands.map(_.commandLine).mkString("\n\n", " & \n", " & \n\n") +
      BiopetFifoPipe.waitScript +
      fifosFiles.map(required("rm", _)).mkString("\n\n", " \n", " \n\n") +
      BiopetFifoPipe.endScript
  }
}

object BiopetFifoPipe {
  val waitScript =
    """
      |
      |FAIL="0"
      |
      |for job in `jobs -p`
      |do
      |echo $job
      |    wait $job || let "FAIL+=1"
      |done
      |
      |echo $FAIL
      |
    """.stripMargin

  val endScript =
    """
echo $FAIL
      |
      |if [ "$FAIL" == "0" ];
      |then
      |echo "BiopetFifoPipe Done"
      |else
      |echo BiopetFifoPipe "FAIL! ($FAIL)"
      |exit $FAIL
      |fi
      |
      |
    """.stripMargin
}