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

    // Executing a tool like FastQC
    val shiva = new Shiva(this)
    shiva.init()
    shiva.biopetScript()
    addAll(shiva.functions)

    /* Only required when using [[SummaryQScript]] */
    addSummaryQScript(shiva)

    // From here you can use the output files of shiva as input file of other jobs
  }
}

//TODO: Replace object Name, must be the same as the class of the pipeline
object HelloPipeline extends PipelineCommand

```





### Config setup

### Test pipeline

### Summary output

### Reporting output (opt)