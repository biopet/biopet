# Welcome to Biopet
###### (Bio Pipeline Execution Tool)

## Introduction

Biopet is an abbreviation of ( Bio Pipeline Execution Tool ) and packages several functionalities:

 1. Tools for working on sequencing data
 1. Pipelines to do analysis on sequencing data
 1. Running analysis on a computing cluster ( Open Grid Engine )
 1. Running analysis on your local desktop computer

### System Requirements

Biopet is build on top of GATK Queue, which requires having `java` installed on the analysis machine(s).

For end-users:

 * Java 7 JVM
 * Minimum 2 GB RAM, more when analysis is also run on this machine.
 * [Cran R 2.15.3](http://cran.r-project.org/)

For developers:

 * OpenJDK 7 or Oracle-Java JDK 7
 * Minimum of 4 GB RAM {todo: provide more accurate estimation on building}
 * [Cran R 2.15.3](http://cran.r-project.org/)
 * Maven 3
 * [GATK + Queue](https://www.broadinstitute.org/gatk/download)
 * IntelliJ or Netbeans 8.0 for development

## How to use

### Running a pipeline

- Help: `java -jar Biopet(version).jar (pipeline of interest) -h`
- Local: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -run`
- Cluster: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -qsub -jobParaEnv BWA -run`
- DryRun: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options)` 
- DryRun (shark): `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -qsub -jobParaEnv BWA`

    - A dry run can be performed to see if the scheduling and creating of the pipelines jobs performs well. Nothing will be executed only the job commands are created. If this succeeds it's a good indication you actual run will be successful as well.
    - Each pipeline can be found as an options inside the jar file Biopet[version].jar which is located in the target directory and can be started with `java -jar <pipelineJarFile>`

### Shark Compute Cluster specific

In the SHARK compute cluster, a module is available to load the necessary dependencies.

    $ module load biopet/v0.2.0

Using this option, the `java -jar Biopet-<version>.jar` can be omnited and `biopet` can be started using:

    $ biopet



### Running pipelines

    $ biopet pipeline <pipeline_name>


- [Flexiprep](pipelines/flexiprep)
- [Mapping](pipelines/mapping)
- [Gatk Variantcalling](https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline)
- BamMetrics
- Basty
- GatkBenchmarkGenotyping
- GatkGenotyping
- GatkPipeline
- GatkVariantRecalibration
- GatkVcfSampleCompare
- [Gentrap](pipelines/gentrap)
- [Sage](pipelines/sage)
- Yamsvp (Under development)

__Note that each pipeline needs a config file written in JSON format__


### Running a tool

    $ biopet tool <tool_name>

  - BedToInterval
  - BedtoolsCoverageToCounts
  - BiopetFlagstat
  - CheckAllelesVcfInBam
  - ExtractAlignedFastq
  - FastqSplitter
  - FindRepeatsPacBio
  - MpileupToVcf
  - SageCountFastq
  - SageCreateLibrary
  - SageCreateTagCounts
  - VcfFilter
  - VcfToTsv
  - WipeReads


- More info can be found here: [How To! Config](https://git.lumc.nl/biopet/biopet/wikis/Config)

## Developers

### Compiling Biopet

1. Clone biopet with `git clone git@git.lumc.nl:biopet/biopet.git biopet`
2. Go to biopet directory
3. run mvn_install_queue.sh, this install queue jars into the local maven repository
3. alternatively download the `queue.jar` from the GATK website
4. run `mvn verify` to compile and package or do `mvn install` to install the jars also in local maven repository



## About 
Go to the [about page](about)

## License

See: [License](license)
