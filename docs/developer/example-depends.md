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

TODO
  
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

(skip this section if using Maven)

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