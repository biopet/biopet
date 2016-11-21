# Gears

## Introduction
Gears is a metagenomics pipeline. (``GE``nome ``A``nnotation of ``R``esidual ``S``equences). One can use this pipeline to identify contamination in sequencing runs on either raw FastQ files or BAM files.
In case of BAM file as input, it will extract the unaligned read(pair) sequences for analysis.

Analysis result is reported in a krona graph, which is visible and navigatable in a webbrowser.

Pipeline analysis components include:
 
 - [Kraken, DerrickWood](https://github.com/DerrickWood/kraken)
 - [Qiime closed reference](http://qiime.org)
 - [Qiime open reference](http://qiime.org)
 - [Qiime rtax](http://qiime.org) (**Experimental**)
 - SeqCount (**Experimental**)

## Gears

This pipeline is used to analyse a group of samples. This pipeline only accepts fastq files. The fastq files first get trimmed and clipped with [Flexiprep](Flexiprep). This can be disabled with the config flags of [Flexiprep](Flexiprep). The samples can be specified with a sample config file, see [Config](../general/Config)

### Config

| Key | Type | default | Function |
| --- | ---- | ------- | -------- |
| gears_use_centrifuge | Boolean | true | Run fastq file with centrifuge |
| gears_use_kraken | Boolean | false | Run fastq file with kraken |
| gears_use_qiime_closed | Boolean | false | Run fastq files with qiime with the closed reference module |
| gears_use_qiime_open | Boolean | false | Run fastq files with qiime with the open reference module |
| gears_use_qiime_rtax | Boolean | false |  Run fastq files with qiime with the rtax module |
| gears_use_seq_count | Boolean | false | Produces raw count files |

### Example

To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline Gears -run  \
-config mySettings.json -config samples.json
```

## GearsSingle

This pipeline can be used to analyse a single sample, this can be fastq files or a bam file. When a bam file is given only the unmapped reads are extracted.

### Example

To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline GearsSingle -run  \
-R1 myFirstReadPair -R2 mySecondReadPair -sample mySampleName \
-library myLibname -config mySettings.json
```

### Commandline flags
For technical reasons, single sample pipelines, such as this pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.

Command line flags for Gears are:

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -R1 | --input_r1 | Path (optional) | Path to input fastq file |
| -R2 | --input_r2 | Path (optional) | Path to second read pair fastq file. |
| -bam | --bamfile | Path (optional) | Path to bam file. |
| -sample | --sampleid | String (**required**) | Name of sample |
| -library | --libid | String (optional) | Name of library |

If `-R2` is given, the pipeline will assume a paired-end setup. `-bam` is mutualy exclusive with the `-R1` and `-R2` flags. Either specify `-bam` or `-R1` and/or `-R2`.

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 

### Config

| Key | Type | default | Function |
| --- | ---- | ------- | -------- |
| gears_use_kraken | Boolean | true | Run fastq file with kraken |
| gears_use_qiime_closed | Boolean | false | Run fastq files with qiime with the closed reference module |
| gears_use_qiime_open | Boolean | false | Run fastq files with qiime with the open reference module |
| gears_use_qiime_rtax | Boolean | false |  Run fastq files with qiime with the rtax module |
| gears_use_seq_count | Boolean | false | Produces raw count files |

### Result files

The results of `GearsSingle` are stored in the following files:

| File suffix | Application | Content | Description |
| ----------- | ----------- | ------- | ----------- |
| *.krkn.raw  | kraken      | tsv     | Annotation per sequence |
| *.krkn.full | kraken-report | tsv | List of all annotation possible with counts filled in for this specific sample|
| *.krkn.json | krakenreport2json| json | JSON representation of the taxonomy report, for postprocessing |

In a seperate `report` folder, one can find the html report displaying the summary and providing a navigation view on the taxonomy graph and (its) result.

## Getting Help
For questions about this pipeline and suggestions, we have a GitHub page where you can submit your ideas and thoughts .[GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
