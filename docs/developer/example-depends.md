# Developer - Using Biopet as a dependency for your own project 

You can use Biopet as a library for your own Scala project.
This can be useful if you want to make your own pipeline that you don't want to add back upstream.
 
## Prerequisites

At bare minimum you will need:

* Java 8 
* Scala 2.10 or higher 
* SBT or Maven

We highly recommend you to use an IDE such as IntelliJ IDEA for development.

### Maven dependencies

If you decide to use Maven, you should clone the [GATK public github repository](https://github.com/broadgsa/gatk).
You should use GATK 3.6. 
After cloning GATK 3.6, run the following in a terminal

`mvn clean && mvn install`

You should perform the same steps for [Biopet](https://github.com/biopet/biopet). This document assumes you are working with Biopet 0.7 or higher.
  
  
### SBT dependencies 

You can develop biopet pipelines with SBT as well. However, since GATK uses Maven, you will still need to install GATK
into your local Maven repository with `mvn install`.

After this, you can create a regular `build.sbt` file in the project root directory. In addition to the regular
SBT settings, you will also need to make SBT aware of the local GATK Maven installation you just did. This can be done
by adding a new resolver object:

```
resolvers += {
  val repo = new IBiblioResolver
  repo.setM2compatible(true)
  repo.setName("localhost")
  repo.setRoot(s"file://${Path.userHome.absolutePath}/.m2/repository")
  repo.setCheckconsistency(false)
  new RawRepository(repo)
}
```

Having set this, you can then add specific biopet modules as your library dependency. Here is one example that adds
the Flexiprep version 0.7.0 dependency:

```
libraryDependencies ++= Seq(
    "nl.lumc.sasc" % "Flexiprep" % "0.7.0"
)
```

In some cases, there may be a conflict with the `org.reflections` package used (this is a transitive dependency of
GATK). If you encounter this, we recommend forcing the version to 0.9.9-RC1 like so:

```
libraryDependencies ++= Seq(
    "org.reflections" % "reflections" % "0.9.9-RC1" force()
)
```

## Project structure 

You should follow typical Scala folder structure. Ideally your IDE will handles this for you.
An example structure looks like:

```
.
├── pom.xml
├── src
│   ├── main
│   │   ├── resources
│   │   │   └── path
│   │   │       └── to
│   │   │           └── your
│   │   │               └── myProject
│   │   │                   └── a_resource.txt
│   │   └── scala
│   │       └── path
│   │           └── to
│   │               └── your
│   │                   └── myProject
│   │                       └── MyProject.scala
│   └── test
│       ├── resources
│       └── scala
│           └── path
│               └── to
│                   └── your
│                       └── MyProject
│                           └── MyProjectTest.scala

```

## POM 

(skip this section if using SBT)

When using Biopet, your Maven pom.xml file should at minimum contain the following dependency:

```xml
    <dependencies>
        <dependency>
            <groupId>nl.lumc.sasc</groupId>
            <artifactId>BiopetCore</artifactId>
            <version>0.7.0</version>
        </dependency>
    </dependencies>
```

In case you want to use a specific pipeline you want to add this to your dependencies. E.g.

```xml
    <dependencies>
        <dependency>
            <groupId>nl.lumc.sasc</groupId>
            <artifactId>BiopetCore</artifactId>
            <version>0.7.0</version>
        </dependency>
        <dependency>
            <groupId>nl.lumc.sasc</groupId>
            <artifactId>Shiva</artifactId>
            <version>0.7.0</version>
        </dependency>
    </dependencies>
```

For a complete example pom.xml see [here](../examples/pom.xml). 


## SBT build

You can use SBT to build a fat JAR that contains all the required class files in a single JAR file. This can be done
using the [sbt-assembly plugin](https://github.com/sbt/sbt-assembly). Keep in mind that you have to explicitly define a specific merge strategy for conflicting
file names. In our experience, the merge strategy below works quite well:

```
assemblyMergeStrategy in assembly := {
  case "git.properties"      => MergeStrategy.first
  // Discard the GATK's queueJobReport.R and use the one from Biopet
  case PathList("org", "broadinstitute", "gatk", "queue", "util", x) if x.endsWith("queueJobReport.R")
                             => MergeStrategy.first
  case "GATKText.properties" => MergeStrategy.first
  case "dependency_list.txt" => MergeStrategy.discard
  case other                 => MergeStrategy.defaultMergeStrategy(other)
}
```

TODO

## New pipeline
 
To create a new pipeline in your project you need a class that extends from `Qscript` and `SummaryQScript`.

E.g.:

```scala
class MyProject(val root: Configurable) extends Qscript with SummaryQScript {

    def init(): Unit = {}
    
    def biopetScript(): Unit = {}  # pipeline code here
    
    def summarySettings = Map()
    def summaryFiles = Map()
    def summaryStats = Map()
    
    def summaryFile: File = new File()

}
```

To make your pipeline runnable from the command line, you need to add a one line object:

```scala
object MyProject extends PipelineCommand
```

When you build your jar, you cna then simply use:

```
java -jar MyProject.jar -config some_config.yml <other arguments>
```

This jar comes with all standard biopet arguments. 