# Flexiprep

## Introduction
Flexiprep is a quality control pipeline. This pipeline checks for possible barcode contamination, clips reads, trims reads and
 runs [FASTQC](http://www.bioinformatics.babraham.ac.uk/projects/fastqc/).
Adapter clipping is performed by [Cutadapt](https://github.com/marcelm/cutadapt).
For quality trimming we use [Sickle](https://github.com/najoshi/sickle).
Flexiprep only works on `.fastq` files.


## Example

To get the help menu:

``` bash
biopet pipeline Flexiprep -h

Arguments for Flexiprep:
 -R1,--input_r1 <input_r1>             R1 fastq file (gzipped allowed)
 -R2,--input_r2 <input_r2>             R2 fastq file (gzipped allowed)
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON config file(s)
 -DSC,--disablescatter                 Disable all scatters
```

Note that the pipeline also works on unpaired reads where one should only provide R1.


To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline Flexiprep -run -outDir myDir \
-R1 myFirstReadPair -R2 mySecondReadPair -sample mySampleName \
-library myLibname -config mySettings.json
```


## Configuration and flags
For technical reasons, single sample pipelines, such as this pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.

Command line flags for Flexiprep are:

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -R1 | --inputR1 | Path (**required**) | Path to input fastq file |
| -R2 | --inputR2 | Path (optional) | Path to second read pair fastq file. |
| -sample | --sampleid | String (**required**) | Name of sample |
| -library | --libid | String (**required**) | Name of library |

If `-R2` is given, the pipeline will assume a paired-end setup.

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 

### Config

All other values should be provided in the config. Specific config values towards the mapping pipeline are:

| Name | Type | Function |
| ---- | ---- | -------- |
| skip_trim | Boolean | Default false, if true the trimming step is skipped |
| skip_clip | Boolean | Default false, if true the clipping step is skipped |

## Result files
The results from this pipeline will be a fastq file.
The pipeline also outputs 2 Fastqc runs one before and one after quality control.

### Example output

~~~ bash
.
├── mySample_01.qc.summary.json
├── mySample_01.qc.summary.json.out
├── mySample_01.R1.contams.txt
├── mySample_01.R1.fastqc
│   ├── mySample_01.R1_fastqc
│   │   ├── fastqc_data.txt
│   │   ├── fastqc_report.html
│   │   ├── Icons
│   │   │   ├── error.png
│   │   │   ├── fastqc_icon.png
│   │   │   ├── tick.png
│   │   │   └── warning.png
│   │   ├── Images
│   │   │   └── warning.png
│   │   ├── Images
│   │   │   ├── duplication_levels.png
│   │   │   ├── kmer_profiles.png
│   │   │   ├── per_base_gc_content.png
│   │   │   ├── per_base_n_content.png
│   │   │   ├── per_base_quality.png
│   │   │   ├── per_base_sequence_content.png
│   │   │   ├── per_sequence_gc_content.png
│   │   │   ├── per_sequence_quality.png
│   │   │   └── sequence_length_distribution.png
│   │   └── summary.txt
│   └── mySample_01.R1.qc_fastqc.zip
├── mySample_01.R1.qc.fastq.gz
├── mySample_01.R1.qc.fastq.gz.md5
├── mySample_01.R2.contams.txt
├── mySample_01.R2.fastqc
│   ├── mySample_01.R2_fastqc
│   │   ├── fastqc_data.txt
│   │   ├── fastqc_report.html
│   │   ├── Icons
│   │   │   ├── error.png
│   │   │   ├── fastqc_icon.png
│   │   │   ├── tick.png
│   │   │   └── warning.png
│   │   ├── Images
│   │   │   ├── duplication_levels.png
│   │   │   ├── kmer_profiles.png
│   │   │   ├── per_base_gc_content.png
│   │   │   ├── per_base_n_content.png
│   │   │   ├── per_base_quality.png
│   │   │   ├── per_base_sequence_content.png
│   │   │   ├── per_sequence_gc_content.png
│   │   │   ├── per_sequence_quality.png
│   │   │   └── sequence_length_distribution.png
│   │   └── summary.txt
│   └── mySample_01.R2_fastqc.zip
├── mySample_01.R2.fastq.md5
├── mySample_01.R2.qc.fastqc
│   ├── mySample_01.R2.qc_fastqc
│   │   ├── fastqc_data.txt
│   │   ├── fastqc_report.html
│   │   ├── Icons
│   │   │   ├── error.png
│   │   │   ├── fastqc_icon.png
│   │   │   ├── tick.png
│   │   │   └── warning.png
│   │   ├── Images
│   │   │   ├── duplication_levels.png
│   │   │   ├── kmer_profiles.png
│   │   │   ├── per_base_gc_content.png
│   │   │   ├── per_base_n_content.png
│   │   │   ├── per_base_quality.png
│   │   │   ├── per_base_sequence_content.png
│   │   │   ├── per_sequence_gc_content.png
│   │   │   ├── per_sequence_quality.png
│   │   │   └── sequence_length_distribution.png
│   │   └── summary.txt
│   └── mySample_01.R2.qc_fastqc.zip
├── mySample_01.R2.qc.fastq.gz
├── mySample_01.R2.qc.fastq.gz.md5
└── report

~~~

## Getting Help

If you have any questions on running Flexiprep, suggestions on how to improve the overall flow, or requests for your favorite 
Quality Control (QC) related program to be added, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
