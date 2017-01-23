# Welcome to Biopet


## Introduction

Biopet (Bio Pipeline Execution Toolkit) is the main pipeline development framework of the LUMC Sequencing Analysis Support Core team. It contains our main pipelines and some of the command line tools we develop in-house. It is meant to be used in the main [SHARK](https://humgenprojects.lumc.nl/trac/shark) computing cluster. While usage outside of SHARK is technically possible, some adjustments may need to be made in order to do so.


## Quick Start

### Running Biopet in the SHARK cluster

Biopet is available as a JAR package in SHARK. The easiest way to start using it is to activate the `biopet` environment module, which sets useful aliases and environment variables:

~~~
$ module load biopet/v0.6.0
~~~

With each Biopet release, an accompanying environment module is also released. The latest release is version 0.6.0, thus `biopet/v0.6.0` is the module you would want to load.

After loading the module, you can access the biopet package by simply typing `biopet`:

~~~
$ biopet
~~~

This will show you a list of tools and pipelines that you can use straight away. You can also execute `biopet pipeline` to show only available pipelines or `biopet tool` to show only the tools. What you should be aware of, is that this is actually a shell function that calls `java` on the system-wide available Biopet JAR file.

~~~
$ java -jar <path/to/current/biopet/release.jar>
~~~

The actual path will vary from version to version, which is controlled by which module you loaded.

Almost all of the pipelines have a common usage pattern with a similar set of flags, for example:

~~~
$ biopet pipeline <pipeline_name> -config <path/to/config.json> -qsub -jobParaEnv BWA -jobQueue all.q -retry 2
~~~

The command above will do a *dry* run of a pipeline using a config file as if the command would be submitted to the SHARK cluster (the `-qsub` flag) to the `BWA` parallel environment (the `-jobParaEnv BWA` flag). The `-jobQueue all.q` flag ensures that the proper Queue is used. We also set the maximum retry of failing jobs to two times (via the `-retry 2` flag). Doing a good run is a good idea to ensure that your real run proceeds smoothly. It may not catch all the errors, but if the dry run fails you can be sure that the real run will never succeed.

If the dry run proceeds without problems, you can then do the real run by using the `-run` flag:

~~~
$ biopet pipeline <pipeline_name> -config <path/to/config.json> -qsub -jobParaEnv BWA -jobQueue all.q -retry 2 -run
~~~

It is usually a good idea to do the real run using `screen` or `nohup` to prevent the job from terminating when you log out of SHARK. In practice, using `biopet` as it is is also fine. What you need to keep in mind, is that each pipeline has their own expected config layout. You can check out more about the general structure of our config files [here](general/config.md). For the specific structure that each pipeline accepts, please consult the respective pipeline page.

### Convention in this documentation

To unify the commands used in the examples, we agree on the following:

Whenever an example command starts with `biopet` as in: 

```
biopet tool ...
```

One can replace the `biopet` command with:

```
java -jar </path/to/biopet.jar> tool
```

The `biopet` shortcut is only available on the SHARK cluster with the `module` environment installed.

### Running Biopet in your own computer

At the moment, we do not provide links to download the Biopet package. If you are interested in trying out Biopet locally, please contact us as [sasc@lumc.nl](mailto:sasc@lumc.nl).


## Contributing to Biopet

Biopet is based on the Queue framework developed by the Broad Institute as part of their Genome Analysis Toolkit (GATK) framework. The current Biopet release is based on the GATK 3.4 release.

We welcome any kind of contribution, be it merge requests on the code base, documentation updates, or any kinds of other fixes! The main language we use is Scala, though the repository also contains a small bit of Python and R. Our main code repository is located at [https://github.com/biopet/biopet](https://github.com/biopet/biopet/issues), along with our issue tracker.

## Local development setup

To develop Biopet, Java 7, Maven 3.2.2, and GATK Queue 3.4 is required. Please consult the Java homepage and Maven homepage for the respective installation instruction. After you have both Java and Maven installed, you would then need to install GATK Queue. However, as the GATK Queue package is not yet available as an artifact in Maven Central, you will need to download, compile, and install GATK Queue first.

~~~
$ git clone https://github.com/broadgsa/gatk-protected
$ cd gatk-protected
$ git checkout 3.4                              # the current release is based on GATK 3.4
$ mvn clean install
~~~

This will install all the required dependencies to your local maven repository. After this is done, you can clone our repository and test if everything builds fine:

~~~
$ git clone https://github.com/biopet/biopet.git
$ cd biopet
$ mvn clean install
~~~

If everything builds fine, you're good to go! Otherwise, don't hesitate to contact us or file an issue at our issue tracker.


## About

Go to the [about page](general/about.md)

## License

See: [License](general/license.md)
