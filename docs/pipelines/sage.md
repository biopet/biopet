# Sage

## Introduction

The Sage pipeline has been created to process SAGE data, which requires a different approach than standard NGS data.

### Modules and Tools

This pipeline uses the following modules and tools:

* [Flexiprep](flexiprep.md)
* [Mapping](mapping.md)
* [SageCountFastq](../tools/sagetools.md)
* [SageCreateLibrary](../tools/sagetools.md)
* [SageCreateTagCounts](../tools/sagetools.md)


## Configuration and flags

Note that one should first create the appropriate [configs](../general/config.md).

Please see the documentation for wrapped pipelines (`Mapping` and `Flexiprep`) for their configuration options and flags.

Specific configuration values for the Sage pipeline are:

| Name | Type | Function |
| ---- | ---- | -------- |
| countbed | Path (required) | Path to count bed file |
| squishedcountbed | Path (optional) | By supplying this file the auto squish job will be skipped |
| transcriptome | Path (required) | Fasta file for transcriptome. Note: Must come from Ensembl! |
| tags_library | Path (optional) | Five-column tab-delimited file (<tag> <firstTag> <AllTags> <FirstAntiTag> <AllAntiTags>). Unsupported option |

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 


## Running Sage

As with other pipelines, you can run the Sage pipeline by invoking the `pipeline` subcommand. There is also a general help available which can be invoked using the `-h` flag:

~~~
$ biopet pipeline sage -h

Arguments for Sage:
 -s,--sample <sample>                  Only Sample
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or
                                       'path:path:key=value'
 -DSC,--disablescatter                 Disable all scatters

~~~

If you are on SHARK, you can also load the `biopet` module and execute `biopet pipeline` instead:

~~~
$ module load biopet/v0.5.0
$ biopet pipeline sage

~~~

To run the pipeline:
~~~
$ biopet pipeline sage -config /path/to/config.json -qsub -jobParaEnv BWA -run
~~~


## Output Files

Below is an example of the output files that you will get after running Sage. Here, we have two samples (`1A` and `1B`) and each sample has two libraries (`run_1` and `run_2`).

~~~
.
├── 1A
│   ├── 1A-2.merge.bai
│   ├── 1A-2.merge.bam
│   ├── 1A.fastq
│   ├── 1A.genome.antisense.counts
│   ├── 1A.genome.antisense.coverage
│   ├── 1A.genome.counts
│   ├── 1A.genome.coverage
│   ├── 1A.genome.sense.counts
│   ├── 1A.genome.sense.coverage
│   ├── 1A.raw.counts
│   ├── 1A.tagcount.all.antisense.counts
│   ├── 1A.tagcount.all.sense.counts
│   ├── 1A.tagcount.antisense.counts
│   ├── 1A.tagcount.sense.counts
│   ├── run_1
│   │   ├── 1A-1.bai
│   │   ├── 1A-1.bam
│   │   ├── flexiprep
│   │   └── metrics
│   └── run_2
│       ├── 1A-2.bai
│       ├── 1A-2.bam
│       ├── flexiprep
│       └── metrics
├── 1B
│   ├── 1B-2.merge.bai
│   ├── 1B-2.merge.bam
│   ├── 1B.fastq
│   ├── 1B.genome.antisense.counts
│   ├── 1B.genome.antisense.coverage
│   ├── 1B.genome.counts
│   ├── 1B.genome.coverage
│   ├── 1B.genome.sense.counts
│   ├── 1B.genome.sense.coverage
│   ├── 1B.raw.counts
│   ├── 1B.tagcount.all.antisense.counts
│   ├── 1B.tagcount.all.sense.counts
│   ├── 1B.tagcount.antisense.counts
│   ├── 1B.tagcount.sense.counts
│   ├── run_1
│   │   ├── 1B-1.bai
│   │   ├── 1B-1.bam
│   │   ├── flexiprep
│   │   └── metrics
│   └── run_2
│       ├── 1B-2.bai
│       ├── 1B-2.bam
│       ├── flexiprep
│       └── metrics
├── ensgene.squish.bed
├── summary-33.tsv
├── taglib
    ├── no_antisense_genes.txt
    ├── no_sense_genes.txt
    └── tag.lib
~~~

## Getting Help

If you have any questions on running SAGE or suggestions on how to improve the overall flow, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
