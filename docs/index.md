# Welcome to Biopet
###### (Bio Pipeline Execution Tool)

## Introduction
### System Requirements
- Java 7 JVM
- Maven 3 (does not need to be on shark)

### Compiling Biopet

1. Clone Biopet with `git clone git@git.lumc.nl:biopet/biopet.git`
2. Go to Biopet directory
3. run mvn_install_queue.sh, this install queue jars into the local maven repository
4. run `mvn verify` to compile and package or do `mvn install` to install the jars also in local maven repository

### Running a pipeline
- Help: `java -jar Biopet(version).jar (pipeline of interest) -h`
- Local: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -run`
- Shark: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -qsub -jobParaEnv BWA -run`
- DryRun: `java -jar Biopet(version).jar (pipeline of interest) (pipeline options)` 
- DryRun(shark): `java -jar Biopet(version).jar (pipeline of interest) (pipeline options) -qsub -jobParaEnv BWA`

    - A dry run can be performed to see if the scheduling and creating of the pipelines jobs performs well. Nothing will be executed only the job commands are created. If this succeeds it's a good indication you actual run will be successful as well.
    - Each pipeline can be found as an options inside the jar file Biopet[version].jar which is located in the target directory and can be started with `java -jar <pipelineJarFile>`

### Running a tool


### Pipelines

- [Flexiprep](https://git.lumc.nl/biopet/biopet/wikis/Flexiprep-Pipeline)
- [Mapping](https://git.lumc.nl/biopet/biopet/wikis/Mapping-Pipeline)
- [Gatk Variantcalling](https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline)
- BamMetrics
- Basty
- GatkBenchmarkGenotyping
- GatkGenotyping
- GatkPipeline
- GatkVariantRecalibration
- GatkVcfSampleCompare
- Gentrap (Under development)
- Sage
- Yamsvp (Under development)

__Note that each pipeline needs a config file written in JSON format__

- More info can be found here: [How To! Config](https://git.lumc.nl/biopet/biopet/wikis/Config)

## About 
Go to the [about page](about)

## License

See: [License](license)
