/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.core

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

import scala.io.Source

/**
  * Created by pjvan_thof on 9/29/15.
  */
class BiopetFifoPipe(val parent: Configurable,
                     protected var commands: List[BiopetCommandLineFunction])
    extends BiopetCommandLineFunction {

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

    for (cmdOutput <- commands;
         cmdInput <- commands if cmdOutput != cmdInput && outputs.contains(cmdOutput);
         outputFile <- outputs(cmdOutput) if inputs.contains(cmdInput);
         inputFile <- inputs(cmdInput) if outputFile == inputFile) yield outputFile
  }

  @Output
  private var outputFiles: List[File] = Nil

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

    outputFiles :::= outputs.values.toList.flatten.filter(!fifoFiles.contains(_))
    outputFiles = outputFiles.distinct

    deps :::= inputs.values.toList.flatten.filter(!fifoFiles.contains(_))
    deps = deps.distinct

    _pipesJobs :::= commands
    _pipesJobs = _pipesJobs.distinct

    analysisName = commands.map(_.analysisName).mkString("_")
  }

  override def beforeCmd(): Unit = {
    commands.foreach { cmd =>
      cmd.beforeGraph()
      cmd.internalBeforeGraph()
      cmd.beforeCmd()
    }
  }

  def cmdLine = {
    this.fifos.filter(_.exists()).map(required("rm", _)).mkString(" \n") +
      this.fifos.map(required("mkfifo", _)).mkString("\n") +
      commands.map(_.commandLine).mkString("\n", " & \n", " & \n")
  }

  override protected def changeScript(file: File): Unit = {
    super.changeScript(file)
    val reader = Source.fromFile(file)
    val lines = reader.getLines().toList
    reader.close()
    val writer = new PrintWriter(file)
    lines.foreach(writer.println)

    BiopetFifoPipe.waitScript
    this.fifos.map(required("rm", _)).mkString("\n\n", " \n", " \n\n")
    BiopetFifoPipe.endScript

    writer.close()
  }

  override def setResources(): Unit = {
    combineResources(commands)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    commands.foreach(_.setupRetry())
    combineResources(commands)
  }

  override def freezeFieldValues(): Unit = {
    super.freezeFieldValues()
    commands.foreach(_.qSettings = qSettings)
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
      |while echo $jobs | grep -e "\w" > /dev/null
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
