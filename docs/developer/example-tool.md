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

This is the minimum setup for having a working tool. We will place some code for line counting in ``main``. Like in other 
higher order programming languages like Java, C++ and .Net, one needs to specify an entry for the program to run. ``def main``
is here the first entry point from the command line into your tool.


### Program arguments and environment variables

A basic application/tool usually takes arguments to configure and set parameters to be used within the tool.
In biopet we facilitate an ``AbstractArgs`` case-class which stores the arguments read from command line.


```scala
  case class Args(inputFile: File = Nil, outputFile: Option[File] = None) extends AbstractArgs
```

The arguments are stored in ``Args``, this is a `Case Class` which acts as a java `HashMap` storing the arguments in an 
object-like fashion.

Consuming and placing values in `Args` works as follows:

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

One has to implement class `OptParser` in order to fill `Args`. In `OptParser` one defines the command line args and how it should be processed.
 In our example, we just copy the values passed on the command line. Further reading: [scala scopt](https://github.com/scopt/scopt)

Let's compile the code into 1 file and test with real functional code:


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

#!TODO: write how to run the tool from a compiled state 


### Debugging the tool with IDEA

### Setting up unit tests

### Adding tool-extension for usage in pipeline

In order to use this tool within biopet, one should write an `extension` for the tool. (as we also do for normal executables like `bwa-mem`)
 
The wrapper would look like this, basically exposing the same command line arguments to biopet in an OOP format.
Note: we also add some functionalities for getting summary data and passing on to biopet.

The concept of having (extension)-wrappers is to create a black-box service model. One should only know how to interact with the tool without necessarily knowing the internals.


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

  // setting the memory for this tool where it starts from.
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


