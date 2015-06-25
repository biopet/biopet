# Flexiprep

## Introduction
Flexiprep is our quality control pipeline. This pipeline checks for possible barcode contamination, clips reads, trims reads and runs
the <a href="http://www.bioinformatics.babraham.ac.uk/projects/fastqc/" target="_blank">Fastqc</a> tool.
Adapter clipping is performed by <a href="https://github.com/marcelm/cutadapt" target="_blank">Cutadapt</a>.
For quality trimming we use <a href="https://github.com/najoshi/sickle" target="_blank">Sickle</a>.
Flexiprep works on `.fastq` files.


## Example

To get the help menu:
~~~
java -jar </path/to/biopet.jar> pipeline Flexiprep -h

Arguments for Flexiprep:
 -R1,--input_r1 <input_r1>             R1 fastq file (gzipped allowed)
 -R2,--input_r2 <input_r2>             R2 fastq file (gzipped allowed)
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON config file(s)
 -DSC,--disablescatter                 Disable all scatters
~~~

Note that the pipeline also works on unpaired reads where one should only provide R1.


To start the pipeline (remove `-run` for a dry run):
~~~bash
java -jar Biopet-0.2.0.jar pipeline Flexiprep -run -outDir myDir \
-R1 myFirstReadPair -R2 mySecondReadPair -sample mySampleName \
-library myLibname -config mySettings.json
~~~


## Configuration and flags
For technical reasons, single sample pipelines, such as this pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.

Command line flags for Flexiprep are:

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
| skiptrim | Boolean | Skip the trimming step |
| skipclip | Boolean | Skip the clipping step |

## Result files
The results from this pipeline will be a fastq file.
The pipeline also outputs 2 Fastqc runs one before and one after quality control.

### Example output

~~~
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
└── mySample_01.R2.qc.fastq.gz.md5
~~~
