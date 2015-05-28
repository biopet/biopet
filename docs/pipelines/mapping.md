# Mapping

## Introduction

The mapping pipeline has been created for NGS users who want to align there data with the most commonly used alignment programs.
The pipeline performs a quality control (QC) on the raw fastq files with our [Flexiprep](flexiprep.md) pipeline. 
After the QC, the pipeline simply maps the reads with the chosen aligner. The resulting BAM files will be sorted on coordinates and indexed, for downstream analysis.

## Tools for this pipeline:

* [Flexiprep](flexiprep.md)
* Alignment programs:
    * <a href="http://bio-bwa.sourceforge.net/bwa.shtml" target="_blank">BWA</a>
    * <a href="http://bowtie-bio.sourceforge.net/index.shtml" target="_blank">Bowtie version 1.1.1</a>
    * <a href="http://www.well.ox.ac.uk/project-stampy" target="_blank">Stampy</a>
    * <a href="https://github.com/alexdobin/STAR" target="_blank">Star</a>
    * <a href="https://github.com/alexdobin/STAR" target="_blank">Star-2pass</a>
* <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>

## Configuration and flags
For technical reasons, single sample pipelines, such as this mapping pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.

Command line flags for the mapping pipeline are:

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -R1 | --input_r1 | Path (**required**) | Path to input fastq file |
| -R2 | --input_r2 | Path (optional) | Path to second read pair fastq file. |
| -sample | --sampleid | String (**required**) | Name of sample |
| -library | --libid | String (**required**) | Name of library |

If `-R2` is given, the pipeline will assume a paired-end setup.

### Config

All other values should be provided in the config. Specific config values towards the mapping pipeline are:

| Name | Type | Function |
| ---- | ---- | -------- |
| aligner | String (optional) | Which aligner to use. Defaults to `bwa`. Choose from [`bwa`, `bwa-aln`, `bowtie`, `gsnap`, `tophat`, `stampy`, `star`, `star-2pass`] |
| skip_flexiprep | Boolean (optional) | Whether to skip the flexiprep QC step (default = False) |
| skip_markduplicates | Boolean (optional) | Whether to skip the Picard Markduplicates step (default = False) |
| skip_metrics | Boolean (optional) | Whether to skip the metrics gathering step (default = False) |
| reference | Path (**required**) | Path to indexed fasta file to be used as reference |
| platform | String (optional) | Read group Platform (defaults to `illumina`)|
| platform_unit | String (**required**) | Read group platform unit |
| readgroup_sequencing_center | String (**required**) | Read group sequencing center |
| readgroup_description | String (**required**) | Read group description |
| predicted_insertsize | Integer (**required**) | Read group predicted insert size |

It is possible to provide any config value as a command line argument as well, using the `-cv` flag.
E.g. `-cv reference=<path/to/reference>` would set value `reference`.

## Example

Note that one should first create the appropriate [settings config](../general/config.md).
Any supplied sample config will be ignored.

### Example config
```json
{
"reference": "<path/to/reference">,
"aligner": "bwa",
"skip_metrics": true,
"platform": "our_platform",
"platform_unit":  "our_unit",
"readgroup_sequencing_center": "our_center",
"readgroup_description": "our_description",
"predicted_insertsize": 300,
"output_dir": "<path/to/output/dir">
}
```


### Running the pipeline

For the help menu:
~~~
java -jar </path/to/biopet.jar> pipeline mapping -h

Arguments for Mapping:
 -R1,--input_r1 <input_r1>             R1 fastq file
 -R2,--input_r2 <input_r2>             R2 fastq file
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or
                                       'path:path:key=value'
 -DSC,--disablescatter                 Disable all scatters

~~~

To run the pipeline:
~~~
java -jar </path/to/biopet.jar> pipeline mapping -run --config mySettings.json \
-R1 myReads1.fastq -R2 myReads2.fastq
~~~
Note that removing -R2 causes the pipeline to assume single end `.fastq` files.

To perform a dry run simply remove `-run` from the commandline call.

----

## Result files
~~~
├── OutDir
    ├── <samplename>-lib_1.dedup.bai
    ├── <samplename>-lib_1.dedup.bam
    ├── <samplename>-lib_1.dedup.metrics
    ├── flexiprep
    └── metrics
~~~
