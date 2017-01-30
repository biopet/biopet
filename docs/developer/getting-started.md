# Developer - Getting started

### Requirements
- Maven 3.3
- Java 8
- Installed Gatk to maven local repository (see below)
- Installed Biopet to maven local repository (see below)
- Some knowledge of the programming language [Scala](http://www.scala-lang.org/) (The pipelines are scripted using Scala)
- We encourage users to use an IDE for scripting the pipeline. One that works pretty well for us is: [IntelliJ IDEA](https://www.jetbrains.com/idea/)

To start the development of a biopet pipeline you should have the following tools installed: 

* Gatk 
* Biopet

Make sure both tools are installed in your local maven repository. To do this one should use the commands below.

```bash
# Replace 'mvn' with the location of you maven executable or put it in your PATH with the export command.

git clone --recursive https://github.com/biopet/biopet.git
cd biopet
mvn -DskipTests=true clean install
```

### Basic components

### Qscript (pipeline)
A basic pipeline would look like this. [Extended example](example-pipeline.md)

```scala
package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ Gzip, Cat }
import org.broadinstitute.gatk.queue.QScript

//TODO: Replace class name, must be the same as the name of the pipeline
class SimplePipeline(val root: Configurable) extends QScript with BiopetQScript {
  // A constructor without arguments is needed if this pipeline is a root pipeline
  // Root pipeline = the pipeline one wants to start on the commandline
  def this() = this(null)

  @Input(required = true)
  var inputFile: File = null

  /** This method can be used to initialize some classes where needed */
  def init(): Unit = {
  }

  /** This method is the actual pipeline */
  def biopetScript: Unit = {
    val cat = new Cat(this)
    cat.input :+= inputFile
    cat.output = new File(outputDir, "file.out")
    add(cat)

    val gzip = new Gzip(this)
    gzip.input :+= cat.output
    gzip.output = new File(outputDir, "file.out.gz")
    add(gzip)
  }
}


object SimplePipeline extends PipelineCommand
```

### Extensions (wrappers)
Wrappers have to be written for each tool used inside the pipeline. A basic wrapper (example wraps the linux ```cat``` command) would look like this:
```scala
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for GNU cat
 */
class Cat(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input file", required = true)
  var input: List[File] = Nil

  @Output(doc = "Unzipped file", required = true)
  var output: File = _

  executable = config("exe", default = "cat")

  /** return commandline to execute */
  def cmdLine = required(executable) + repeat(input) + " > " + required(output)
}
```

### Tools (Scala programs)
Within the Biopet framework it is also possible to write your own tools in Scala. 
When a certain functionality or script is not incorporated within the framework one can write a tool that does the job. 
Below you can see an example tool which is written for automatically building sample configs.

[Extended example](example-tool.md)

```scala
package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils._
import nl.lumc.sasc.biopet.utils.ToolCommand
import scala.collection.mutable

import scala.io.Source

/**
 * This tool can convert a tsv to a json file
 */
object SamplesTsvToJson extends ToolCommand {
  case class Args(inputFiles: List[File] = Nil, outputFile: Option[File] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "inputFiles") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputFiles = x :: c.inputFiles)
    } text "Input must be a tsv file, first line is seen as header and must at least have a 'sample' column, 'library' column is optional, multiple files allowed"
    opt[File]('o', "outputFile") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    }
  }

  /** Executes SamplesTsvToJson */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val jsonString = stringFromInputs(commandArgs.inputFiles)
    commandArgs.outputFile match {
      case Some(file) => {
        val writer = new PrintWriter(file)
        writer.println(jsonString)
        writer.close()
      }
      case _ => println(jsonString)
    }
  }

  def mapFromFile(inputFile: File): Map[String, Any] = {
    val reader = Source.fromFile(inputFile)
    val lines = reader.getLines().toList.filter(!_.isEmpty)
    val header = lines.head.split("\t")
    val sampleColumn = header.indexOf("sample")
    val libraryColumn = header.indexOf("library")
    if (sampleColumn == -1) throw new IllegalStateException("Sample column does not exist in: " + inputFile)

    val sampleLibCache: mutable.Set[(String, Option[String])] = mutable.Set()

    val librariesValues: List[Map[String, Any]] = for (tsvLine <- lines.tail) yield {
      val values = tsvLine.split("\t")
      require(header.length == values.length, "Number of columns is not the same as the header")
      val sample = values(sampleColumn)
      val library = if (libraryColumn != -1) Some(values(libraryColumn)) else None

      //FIXME: this is a workaround, should be removed after fixing #180
      if (sample.head.isDigit || library.forall(_.head.isDigit))
        throw new IllegalStateException("Sample or library may not start with a number")

      if (sampleLibCache.contains((sample, library)))
        throw new IllegalStateException(s"Combination of $sample ${library.map("and " + _).getOrElse("")} is found multiple times")
      else sampleLibCache.add((sample, library))
      val valuesMap = (for (
        t <- 0 until values.size if !values(t).isEmpty && t != sampleColumn && t != libraryColumn
      ) yield header(t) -> values(t)).toMap
      library match {
        case Some(lib) => Map("samples" -> Map(sample -> Map("libraries" -> Map(lib -> valuesMap))))
        case _         => Map("samples" -> Map(sample -> valuesMap))
      }
    }
    librariesValues.foldLeft(Map[String, Any]())((acc, kv) => mergeMaps(acc, kv))
  }

  def stringFromInputs(inputs: List[File]): String = {
    val map = inputs.map(f => mapFromFile(f)).foldLeft(Map[String, Any]())((acc, kv) => mergeMaps(acc, kv))
    mapToJson(map).spaces2
  }
}
```