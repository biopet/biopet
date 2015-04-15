# Flexiprep

## Introduction
Flexiprep is out quality control pipeline. This pipeline checks for possible barcode contamination, clips reads, trims reads and runs
the tool <a href="http://www.bioinformatics.babraham.ac.uk/projects/fastqc/" target="_blank">Fastqc</a>.
The adapter clipping is performed by <a href="https://github.com/marcelm/cutadapt" target="_blank">Cutadapt</a>.
For the quality trimming we use: <a href="https://github.com/najoshi/sickle" target="_blank">Sickle</a>. Flexiprep works on `.fastq` files.


## Example

To get the help menu:
~~~
java -jar Biopet-0.2.0-DEV.jar pipeline Flexiprep -h
Arguments for Flexiprep:
 -R1,--input_r1 <input_r1>                       R1 fastq file (gzipped allowed)
 -sample,--samplename <samplename>               Sample name
 -library,--libraryname <libraryname>            Library name
 -outDir,--output_directory <output_directory>   Output directory
 -R2,--input_r2 <input_r2>                       R2 fastq file (gzipped allowed)
 -skiptrim,--skiptrim                            Skip Trim fastq files
 -skipclip,--skipclip                            Skip Clip fastq files
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters
~~~

As we can see in the above example we provide the options to skip trimming or clipping 
since sometimes you want to have the possibility to not perform these tasks e.g.
if there are no adapters present in your .fastq. Note that the pipeline also works on unpaired reads where one should only provide R1.


To start the pipeline (remove `-run` for a dry run):
~~~bash
java -jar Biopet-0.2.0.jar pipeline Flexiprep -run -outDir myDir \
-R1 myFirstReadPair -R2 mySecondReadPair -sample mySampleName \
-library myLibname -config mySettings.json
~~~

## Result files
The results from this pipeline will be a fastq file which is depending on the options either clipped and trimmed, only clipped,
 only trimmed or no quality control at all. The pipeline also outputs 2 Fastqc runs one before and one after quality control.

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
