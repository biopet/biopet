# Developer - Example tool

In this tutorial we explain how to create a tool within the biopet-framework. We provide convient helper methods which can be used in the tool.
We take a line counter as the use case.

### Initial tool code
```scala
package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils._
import nl.lumc.sasc.biopet.utils.ToolCommand
import scala.collection.mutable

import scala.io.Source

/**
 */
object SimpleTool extends ToolCommand {
    /*
    * Main function executes the LineCounter.scala
    */    
    def main(args: Array[String]): Unit = {
        println("This is the SimpleTool");
    }
}
```

This is the minimum setup for having a working tool. (not functional yet)


### Program arguments and environment variables

A basic application/tool usually takes arguments to configure and set parameters to be used within the tool.
In biopet we facilitate an ``AbstractArgs`` case-class which stores the arguments read from commandline.


```scala
  case class Args(inputFile: File = Nil, outputFile: Option[File] = None) extends AbstractArgs
```

The arguments are stored in ``Args``

Then add code that fills the Args.

```scala
  class OptParser extends AbstractOptParser {

    head(
      s"""
         |$commandName - Count lines in a textfile
      """.stripMargin)

    opt[File]('i', "input") required () unbounded () valueName "<inputFile>" action { (x, c) =>
      c.copy(inputFile = x)
    } validate {
      x => if (x.exists) success else failure("Inputfile not found")
    } text "Count lines from this files"

    opt[File]('o', "output") unbounded () valueName "<outputFile>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "File to write output to, if not supplied output go to stdout"

  }
```

In the end your tool would look like the following:

```scala

package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils._
import nl.lumc.sasc.biopet.utils.ToolCommand
import scala.collection.mutable

import scala.io.Source

/**
 */
object SimpleTool extends ToolCommand {
  case class Args(inputFile: File = Nil, outputFile: Option[File] = None) extends AbstractArgs
        
  class OptParser extends AbstractOptParser {
    
    head(
      s"""
         |$commandName - Count lines in a textfile
      """.stripMargin)
    
    opt[File]('i', "input") required () unbounded () valueName "<inputFile>" action { (x, c) =>
      c.copy(inputFile = x)
    } validate {
      x => if (x.exists) success else failure("Inputfile not found")
    } text "Count lines from this files"
    
    opt[File]('o', "output") unbounded () valueName "<outputFile>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "File to write output to, if not supplied output go to stdout"
    
  }

  def countToJSON(inputRaw: File): String = {
    val reader = Source.fromFile(inputRaw)
    val nLines = reader.getLines.size
    
    mapToJson(Map(
        "lines" -> nLines,
        "input" -> inputRaw
    )).spaces2
  }

  /*
   * Main function executes the LineCounter.scala
   */    
  def main(args: Array[String]): Unit = {
    val commandArgs: Args = parseArgs(args)
    
    // use the arguments
    val jsonString: String = countToJSON(commandArgs.input)
    commandArgs.outputJson match {
      case Some(file) =>
        val writer = new PrintWriter(file)
        writer.println(jsonString)
        writer.close()
       case _ => println(jsonString)
    }
  }
}
```

### Running your new tool

### Debugging the tool with IDEA

### Setting up unit tests

### Adding tool-extension for usage in pipeline

When this tool is used in a pipeline in biopet, one has to add a tool wrapper for the tool created.
 
The wrapper would look like:

```scala
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }

/**
 * SimpleTool function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class SimpleTool(val root: Configurable) extends ToolCommandFunction with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.SimpleTool

  @Input(doc = "Input file to count lines from", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = _

  override def defaultCoreMemory = 1.0

  override def cmdLine = super.cmdLine +
    required("-i", input) +
    required("-o", output)

  def summaryStats: Map[String, Any] = {
    ConfigUtils.fileToConfigMap(output)
  }

  def summaryFiles: Map[String, File] = Map(
    "simpletool" -> output
  )

}

object SimpleTool {
  def apply(root: Configurable, input: File, output: File): SimpleTool = {
    val report = new SimpleTool(root)
    report.inputReport = input
    report.output = new File(output, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".simpletool.json")
    report
  }

  def apply(root: Configurable, input: File, outDir: String): SimpleTool = {
    val report = new SimpleTool(root)
    report.inputReport = input
    report.output = new File(outDir, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".simpletool.json")
    report
  }
}
```


### Summary setup (for reporting results to JSON)


