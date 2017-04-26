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
/*
 * Copyright (c) 2012 The Broad Institute
 * Modifications (c) 2014 Leiden University Medical Center
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * This is a modifed version of org.broadinstitute.gatk.queue.QCommandLine, works without compiling a scala files but used build in class files to skip compile step
 */

package nl.lumc.sasc.biopet.core.workaround

import java.io.{File, FileOutputStream}
import java.util
import java.util.ResourceBundle

import nl.lumc.sasc.biopet.FullVersion
import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.queue.engine.{QGraph, QGraphSettings}
import org.broadinstitute.gatk.queue.util.{
  Logging => GatkLogging,
  ScalaCompoundArgumentTypeDescriptor,
  ClassFieldCache
}
import org.broadinstitute.gatk.queue.{QCommandPlugin, QScript, QScriptManager}
import org.broadinstitute.gatk.utils.classloader.PluginManager
import org.broadinstitute.gatk.utils.commandline._
import org.broadinstitute.gatk.utils.help.ApplicationDetails
import org.broadinstitute.gatk.utils.io.IOUtils
import org.broadinstitute.gatk.utils.text.TextFormattingUtils

import scala.collection.JavaConversions._

/**
  * Entry point of Queue.  Compiles and runs QScripts passed in to the command line.
  */
object BiopetQCommandLine extends GatkLogging {

  /**
    * Main.
    * @param argv Arguments.
    */
  def main(argv: Array[String]) {
    val qCommandLine = new BiopetQCommandLine

    val shutdownHook = new Thread {
      override def run() {
        logger.info("Shutting down jobs. Please wait...")
        qCommandLine.shutdown()
      }
    }

    Runtime.getRuntime.addShutdownHook(shutdownHook)

    CommandLineProgram.start(qCommandLine, argv)
    try {
      Runtime.getRuntime.removeShutdownHook(shutdownHook)
      qCommandLine.shutdown()
    } catch {
      case e: Exception =>
      /* ignore, example 'java.lang.IllegalStateException: Shutdown in progress' */
    }
    if (CommandLineProgram.result != 0)
      System.exit(CommandLineProgram.result)
  }

  val timestamp = System.currentTimeMillis
}

/**
  * Entry point of Queue.  Compiles and runs QScripts passed in to the command line.
  */
class BiopetQCommandLine extends CommandLineProgram with Logging {
  @Input(fullName = "script", shortName = "S", doc = "QScript scala file", required = false)
  @ClassType(classOf[File])
  var scripts: Seq[File] = Nil
  var pipelineName: String = _

  @ArgumentCollection
  val settings = new QGraphSettings

  private val qScriptManager = new QScriptManager
  private val qGraph = new QGraph
  private var qScriptClasses: File = _
  private var shuttingDown = false

  /**
    * we modified this in Biopet to skip compiling and show full stacktrace again
    */
  private lazy val qScriptPluginManager = {
    qScriptClasses = IOUtils.tempDir("Q-Classes-", "", settings.qSettings.tempDirectory)
    for (t <- scripts) {
      val is = getClass.getResourceAsStream(t.getAbsolutePath)
      val os = new FileOutputStream(qScriptClasses.getAbsolutePath + "/" + t.getName)
      org.apache.commons.io.IOUtils.copy(is, os)
      os.close()
      val s =
        if (t.getName.endsWith("/")) t.getName.substring(0, t.getName.length - 1) else t.getName
      pipelineName = s.substring(0, s.lastIndexOf(".")) + "." + BiopetQCommandLine.timestamp
    }

    // override createByType to pass the correct exceptions
    new PluginManager[QScript](qPluginType, List(qScriptClasses.toURI.toURL)) {
      override def createByType(plugintype: Class[_ <: QScript]) = {
        val noArgsConstructor = plugintype.getDeclaredConstructor()
        noArgsConstructor.setAccessible(true)
        noArgsConstructor.newInstance()
      }
    }
  }

  private lazy val qCommandPlugin = {
    new PluginManager[QCommandPlugin](classOf[QCommandPlugin])
  }

  private lazy val allCommandPlugins = qCommandPlugin.createAllTypes()

  private lazy val qPluginType: Class[_ <: QScript] = {
    allCommandPlugins.map(_.qScriptClass).headOption.getOrElse(classOf[QScript])
  }

  /**
    * Takes the QScripts passed in, runs their script() methods, retrieves their generated
    * functions, and then builds and runs a QGraph based on the dependencies.
    */
  def execute = {
    var success = false
    var result = 1
    var functionsAndStatusSize = 0
    try {
      ClassFieldCache.parsingEngine = this.parser

      if (settings.qSettings.runName == null)
        settings.qSettings.runName = pipelineName
      if (IOUtils.isDefaultTempDir(settings.qSettings.tempDirectory))
        settings.qSettings.tempDirectory =
          IOUtils.absolute(settings.qSettings.runDirectory, ".queue/tmp")
      qGraph.initializeWithSettings(settings)

      for (commandPlugin <- allCommandPlugins) {
        loadArgumentsIntoObject(commandPlugin)
      }

      for (commandPlugin <- allCommandPlugins) {
        if (commandPlugin.statusMessenger != null)
          commandPlugin.statusMessenger.started()
      }

      qGraph.messengers =
        allCommandPlugins.filter(_.statusMessenger != null).map(_.statusMessenger).toSeq

      // TODO: Default command plugin argument?
      val remoteFileConverter =
        (for (commandPlugin <- allCommandPlugins if commandPlugin.remoteFileConverter != null)
          yield commandPlugin.remoteFileConverter).headOption.orNull

      if (remoteFileConverter != null)
        loadArgumentsIntoObject(remoteFileConverter)

      val allQScripts = qScriptPluginManager.createAllTypes()
      for (script <- allQScripts) {
        logger.info(
          "Scripting " + qScriptPluginManager.getName(
            script.getClass.asSubclass(classOf[QScript])))
        loadArgumentsIntoObject(script)
        allCommandPlugins.foreach(_.initScript(script))
        // TODO: Pulling inputs can be time/io expensive! Some scripts are using the files to generate functions-- even for dry runs-- so pull it all down for now.
        //if (settings.run)
        script.pullInputs()
        script.qSettings = settings.qSettings
        script.script()

        if (remoteFileConverter != null) {
          if (remoteFileConverter.convertToRemoteEnabled)
            script.mkRemoteOutputs(remoteFileConverter)
        }

        script.functions.foreach(qGraph.add)
        logger.info("Added " + script.functions.size + " functions")
      }
      // Execute the job graph
      qGraph.run()

      val functionsAndStatus = qGraph.getFunctionsAndStatus

      // walk over each script, calling onExecutionDone
      for (script <- allQScripts) {
        val scriptFunctions = functionsAndStatus.filterKeys(f => script.functions.contains(f))
        script.onExecutionDone(scriptFunctions, success)
      }
      functionsAndStatusSize = functionsAndStatus.size

      // write the final complete job report
      logger.info("Writing final jobs report...")
      qGraph.writeJobsReport()

      if (qGraph.success) {
        if (settings.run) {
          allQScripts.foreach(_.pushOutputs())
          for (commandPlugin <- allCommandPlugins)
            if (commandPlugin.statusMessenger != null) {
              val allInputs = allQScripts.map(_.remoteInputs)
              val allOutputs = allQScripts.map(_.remoteOutputs)
              commandPlugin.statusMessenger.done(allInputs, allOutputs)
            }
        }
        success = true
        result = 0
      }
    } finally {
      if (!success) {
        logger.info("Done with errors")
        qGraph.logFailed()
        if (settings.run) {
          for (commandPlugin <- allCommandPlugins)
            if (commandPlugin.statusMessenger != null)
              commandPlugin.statusMessenger.exit(
                "Done with errors: %s".format(qGraph.formattedStatusCounts))
        }
      }
    }
    logger.info(
      "Script %s with %d total jobs".format(if (success) "completed successfully" else "failed",
                                            functionsAndStatusSize))
    result
  }

  /**
    * Returns true as QScripts are located and compiled.
    * @return true
    */
  override def canAddArgumentsDynamically = true

  /**
    * Returns the list of QScripts passed in via -S and other plugins
    * so that their arguments can be inspected before QScript.script is called.
    * @return Array of dynamic sources
    */
  override def getArgumentSources = {
    var plugins = Seq.empty[Class[_]]
    plugins ++= qScriptPluginManager.getPlugins
    plugins ++= qCommandPlugin.getPlugins
    plugins.toArray
  }

  /**
    * Returns the name of a script/plugin
    * @return The name of a script/plugin
    */
  override def getArgumentSourceName(source: Class[_]) = {
    if (classOf[QScript].isAssignableFrom(source))
      qScriptPluginManager.getName(source.asSubclass(classOf[QScript]))
    else if (classOf[QCommandPlugin].isAssignableFrom(source))
      qCommandPlugin.getName(source.asSubclass(classOf[QCommandPlugin]))
    else
      null
  }

  /**
    * Returns a ScalaCompoundArgumentTypeDescriptor that can parse argument sources into scala collections.
    * @return a ScalaCompoundArgumentTypeDescriptor
    */
  override def getArgumentTypeDescriptors =
    util.Arrays.asList(new ScalaCompoundArgumentTypeDescriptor)

  override def getApplicationDetails: ApplicationDetails = {
    new ApplicationDetails(createQueueHeader(),
                           Seq.empty[String],
                           ApplicationDetails.createDefaultRunningInstructions(
                             getClass.asInstanceOf[Class[CommandLineProgram]]),
                           "")
  }

  private def createQueueHeader(): Seq[String] = {
    Seq(
      "Biopet version: " + FullVersion,
      "",
      "Based on GATK Queue",
      //                     String.format("Queue v%s, Compiled %s", getQueueVersion, getBuildTimestamp),
      "Copyright (c) 2012 The Broad Institute",
      "For support and documentation go to http://www.broadinstitute.org/gatk"
    )
  }

  private def getQueueVersion: String = {
    val stingResources: ResourceBundle =
      TextFormattingUtils.loadResourceBundle("StingText", this.getClass)

    if (stingResources.containsKey("org.broadinstitute.sting.queue.QueueVersion.version")) {
      stingResources.getString("org.broadinstitute.sting.queue.QueueVersion.version")
    } else {
      "<unknown>"
    }
  }

  private def getBuildTimestamp: String = {
    val stingResources: ResourceBundle =
      TextFormattingUtils.loadResourceBundle("StingText", this.getClass)

    if (stingResources.containsKey("build.timestamp")) {
      stingResources.getString("build.timestamp")
    } else {
      "<unknown>"
    }
  }

  def shutdown() = {
    shuttingDown = true
    qGraph.shutdown()
    if (qScriptClasses != null) IOUtils.tryDelete(qScriptClasses)
  }
}
