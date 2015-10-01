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
    commands.foreach { cmd =>
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

  override def setResources(): Unit = {
    combineResources(commands)
  }
}

object BiopetFifoPipe {
  val waitScript =
    """
      |
      |allJobs=`jobs -p`
      |jobs=$allJobs
      |
      |echo [`date`] pids: $jobs
      |
      |FAIL="0"
      |
      |while echo $jobs | grep -e "\d" > /dev/null
      |do
      |	for job in $jobs
      |	do
      |		if ps | grep "$job " | grep -v grep > /dev/null
      |		then
      |		    echo [`date`] $job still running > /dev/null
      |		else
      |		    jobs=`echo $jobs | sed "s/${job}//"`
      |			wait $job || FAIL=$?
      |			if echo $FAIL | grep -ve "^0$" > /dev/null
      |			then
      |			    echo [`date`] $job fails with exitcode: $FAIL
      |				break
      |			fi
      |			echo [`date`] $job done
      |		fi
      |	done
      |	if echo $FAIL | grep -ve "^0$" > /dev/null
      |    then
      |        break
      |    fi
      |	sleep 1
      |done
      |
      |if echo $FAIL | grep -ve "^0$" > /dev/null
      |then
      |    echo [`date`] kill other pids: $jobs
      |    kill $jobs
      |fi
      |
      |echo [`date`] Done
      |
      |
    """.stripMargin

  val endScript =
    """
      |
      |if [ "$FAIL" == "0" ];
      |then
      |echo [`date`] "BiopetFifoPipe Done"
      |else
      |echo [`date`] BiopetFifoPipe "FAIL! ($FAIL)"
      |exit $FAIL
      |fi
      |
      |
    """.stripMargin
}