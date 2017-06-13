# Introduction

The MultiSampleMapping pipeline was created for handling data from multiple samples at the same time. It extends the functionality of the mapping
pipeline, which is meant to take only single sample data as input. As most experimental setups require data generation from many different samples and 
the alignment of the data to a reference of choice is a very common task for further downstream analyses, 
this pipeline serves also as a first step for the following analysis pipelines bundled within BIOPET:

 *  [Basty](basty.md) - Bacterial typing
 *  [Carp](carp.md) - ChIP-seq analysis
 *  [Gentrap](gentrap.md) - Generic transcriptome analysis pipeline
 *  [Shiva](shiva.md) - Variant calling
 *  [Tinycap](tinycap.md) - smallRNA analysis

Its aim is to align the input data to the reference of interest with the most commonly used aligners 
(for a complete list of supported aligners see [here](../mapping.md)). 

#Setting up

## Reference files

An important step prior to the analysis is the proper generation of all the required index files for the reference, apart from the 
reference sequence file itself.

The index files are created from the supplied reference:

* ```.dict``` (can be produced with <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>)
* ```.fai``` (can be produced with <a href="http://samtools.sourceforge.net/samtools.shtml" target="_blank">Samtools faidx</a> 
* ```.idxSpecificForAligner``` (depending on which aligner is used one should create a suitable index specific for that aligner. 
Each aligner has its own way of creating index files. Therefore the options for creating the index files can be found inside the aligner itself)

## Configuration files

MultiSampleMapping relies on __YML__ (or __JSON__) configuration files to run its analyses. There are two important parts here, the configuration for the samples
(to determine the sample layout of the experiment) and the configuration of the pipeline settings (to determine the different parameters for the
pipeline components).

### Sample config

For a detailed explanation of how the samples configuration file should be created please see [here](../../general/config.md).
As an example for two samples, one with two libraries and one with a single library, a samples config would look like this:

```YAML
samples:
  sample1:
    libraries:
      lib01:
        R1: /full/path/to/R1.fastq.gz
        R2: /full/path/to/R2.fastq.gz
      lib02:
        R1: /full/path/to/R1.fastq.gz
        R2: /full/path/to/R2.fastq.gz
  sample2:
     libraries:
       lib01:
         R1: /full/path/to/R1.fastq.gz
         R2: /full/path/to/R2.fastq.gz
```

### Settings config

As this is an extension of the mapping pipeline a comprehensive list for all the settings affecting the analysis can be found [here](../mapping.md###Config).
Required settings that should be included in this config file are:

## Running multisamplemapping

To run the pipeline (it is recommended to first do a dry run, removing the `-run` option)

```bash
biopet pipeline multisamplemapping -run \
-config /path/to/samples.yml \
-config /path/to/config.yml
```

