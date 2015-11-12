# Developer - Example pipeline

This document/tutorial will show you how to add a new pipeline to biopet. The minimum requirement is having:

 - A clean biopet checkout from git
 - Texteditor or IntelliJ IDEA
 
### Adding pipeline folder

Via commandline:

```
cd biopet/public/
mkdir -p mypipeline/src/main/scala/nl/lumc/sasc/biopet/pipelines/mypipeline
```

### Adding maven project

Adding a `pom.xml` to `biopet/public/mypipeline` folder. The example below is the minimum required POM definition

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>Biopet</artifactId>
        <groupId>nl.lumc.sasc</groupId>
        <version>0.5.0-SNAPSHOT</version>
        <relativePath>../</relativePath>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <inceptionYear>2015</inceptionYear>
    <artifactId>MyPipeline</artifactId>
    <name>MyPipeline</name>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>nl.lumc.sasc</groupId>
            <artifactId>BiopetCore</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>nl.lumc.sasc</groupId>
            <artifactId>BiopetToolsExtensions</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>6.8</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest_2.10</artifactId>
            <version>2.2.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### Initial pipeline code

In `biopet/public/mypipeline/src/main/scala/nl/lumc/sasc/biopet/pipelines/mypipeline` create a file named `HelloPipeline.scala` with the following contents:

```scala
package nl.lumc.sasc.biopet/pipelines.mypipeline

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import org.broadinstitute.gatk.queue.QScript

class HelloPipeline(val root: Configurable) extends QScript with SummaryQScript {
  def this() = this(null)

  /** Only required when using [[SummaryQScript]] */
  def summaryFile = new File(outputDir, "hello.summary.json")

  /** Only required when using [[SummaryQScript]] */
  def summaryFiles: Map[String, File] = Map()

  /** Only required when using [[SummaryQScript]] */
  def summarySettings = Map()

  // This method can be used to initialize some classes where needed
  def init(): Unit = {
  }

  // This method is the actual pipeline
  def biopetScript: Unit = {
    // Executing a tool like FastQC, calling the extension in `nl.lumc.sasc.biopet.extensions.Fastqc`

    val fastqc = new Fastqc(this)
    fastqc.fastqfile = config("fastqc_input")
    fastqc.output = new File(outputDir, "fastqc.txt")
    add(fastqc)

  }
}

object HelloPipeline extends PipelineCommand

```

Looking at the pipeline, you can see that it inherits from `QScript`. `QScript` is the fundamental class which gives access to the Queue scheduling system. In addition `SummaryQScript` (trait) will add another layer of functions which provides functions to handle and create summary files from pipeline output.
`class HelloPipeline(val root: Configurable`, our pipeline is called HelloPipeline and is taking a `root` with configuration options passed down to Biopet via a JSON specified on the commandline (--config).

```
  def biopetScript: Unit = {
  }
```

One can start adding pipeline components in `biopetScript`, this is the programmatically equivalent to the `main` method in most popular programming languages. For example, adding a QC tool to the pipeline like `FastQC`. Look at the example shown above.
Setting up the pipeline is done within the pipeline itself, fine-tuning is always possible by overriding in the following way:
 
```
    val fastqc = new Fastqc(this)
    fastqc.fastqfile = config("fastqc_input")
    fastqc.output = new File(outputDir, "fastqc.txt")
    
    // change kmers settings to 9, wrap with `Some()` because `fastqc.kmers` is a `Option` value.
    fastqc.kmers = Some(9)
    
    add(fastqc)

```




### Config setup

### Test pipeline

### Summary output

### Reporting output (opt)