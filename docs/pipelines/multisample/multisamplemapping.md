# Introduction

The MultiSampleMapping pipeline was created for handling data from multiple samples at the same time. It extends the functionality of the mapping
pipeline, which is meant to take only single sample data as input. As most experimental setups require data generation from many different samples and 
the alignment of the data to a reference of choice is a very common task for further downstream anlyses, 
this pipeline serves also as a first step for many of the other analysis pipelines bundled within BIOPET. 

Its aim is to align the input data to the reference of interest with the most commonly used aligners 
(for a complete list of supported aligners see [here](../mapping.md)). 

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

### Running multisamplemapping