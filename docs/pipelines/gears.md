# Gears

## Introduction
Gears is a metagenomics pipeline. (``GE``nome ``A``nnotation of ``R``esidual ``S``equences). It can be used to identify contamination in sequencing runs on either raw FastQ files or BAM files.
In case of BAM file as input, it will extract the unaligned read(pair) sequences for analysis.
It can also be used to analyse sequencing data obtained from metagenomics samples, containing a mix of different organisms. Taxonomic labels will be assigned to the input reads and these will be reported.

The result of the analysis is reported in a [Krona graph](https://github.com/marbl/Krona/wiki), which is visible in an interactive way in a web browser.

Pipeline analysis components include:

 - [Centrifuge](https://github.com/infphilo/centrifuge)
 - [Kraken, DerrickWood](https://github.com/DerrickWood/kraken)
 - [Qiime closed reference](http://qiime.org)
 - [Qiime open reference](http://qiime.org)
 - [Qiime rtax](http://qiime.org) (**Experimental**)
 - SeqCount (**Experimental**)

## Gears

This pipeline is used to analyse a group of samples and only accepts fastq files. The fastq files first get trimmed and clipped with [Flexiprep](flexiprep).
This can be disabled with the config flags of [Flexiprep](flexiprep). The samples can be specified with a sample config file, see [Config](../general/config).
`Gears` uses centrifuge by default as its classification engine. An indexed database, created with ```centrifuge-index``` is required to be specified by including ```centrifuge_index: /path/to/index``` in the [config](../general/config) file.
More information on how to build centrifuge databases and indexes can be found [here](https://github.com/infphilo/centrifuge/blob/master/MANUAL.markdown#database-download-and-index-building).
On LUMC's SHARK the NCBI non-redundant database (nt) is used as the default database against which taxonomic assignments for the short input reads are made.
If you would like to use another classifier you should specify so in the [config](../general/config) file. Multiple classifiers can be used in one run, if you wish to have multiple outputs for comparison.
Note that the Centrifuge and Kraken systems can be used for any kind of input sequences (e.g. shotgun or WGS), while the QIIME system is optimized for 16S-based analyses.

### Config

| Key | Type | default | Function |
| --- | ---- | ------- | -------- |
| gears_use_centrifuge | Boolean | true | Run fastq files with centrifuge |
| gears_use_kraken | Boolean | false | Run fastq files with kraken |
| gears_use_qiime_closed | Boolean | false | Run fastq files with qiime with the closed reference module |
| gears_use_qiime_open | Boolean | false | Run fastq files with qiime with the open reference module |
| gears_use_qiime_rtax | Boolean | false |  Run fastq files with qiime with the rtax module |
| gears_use_seq_count | Boolean | false | Produces raw count files |

### Example

To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline Gears -run  \
-config /path/to/mySettings.json -config /path/to/samples.json
```

Note: If you are using the LUMC High performance Computer cluster (aka SHARK) make sure to include the ```-qsub -jobParaEnv BWA -jobQueue all.q``` flags when invoking the command.

## GearsSingle

This pipeline can be used to analyse a single sample, be it fastq files or a bam file. When a bam file is provided as input only the unmapped reads are extracted and further analysed.

### Example

To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline GearsSingle -run  \
-R1 /path/to/myFirstReadPair -R2 /path/to/mySecondReadPair -sample mySampleName \
-library myLibname -config /path/to/mySettings.json
```
Note: If you are using the LUMC High performance Computer cluster (aka SHARK) make sure to include the ```-qsub -jobParaEnv BWA -jobQueue all.q``` flags when invoking the command.

### Command line flags
For technical reasons, single sample pipelines, such as this pipeline do **not** take a sample config.
Input files are instead given on the command line as a flag.

Command line flags for Gears are:

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -R1 | --input_r1 | Path (optional) | Path to input fastq file |
| -R2 | --input_r2 | Path (optional) | Path to second read pair fastq file. |
| -bam | --bamfile | Path (optional) | Path to bam file. |
| -sample | --sampleid | String (**required**) | Name of sample |
| -library | --libid | String (optional) | Name of library |

If `-R2` is given, the pipeline will assume a paired-end setup. `-bam` is mutually exclusive with the `-R1` and `-R2` flags. Either specify `-bam` or `-R1` and/or `-R2`.

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 

### Config

| Key | Type | default | Function |
| --- | ---- | ------- | -------- |
| gears_use_centrifuge | Boolean | true | Run fastq file with centrifuge |
| gears_use_kraken | Boolean | false | Run fastq file with kraken |
| gears_use_qiime_closed | Boolean | false | Run fastq files with qiime with the closed reference module |
| gears_use_qiime_open | Boolean | false | Run fastq files with qiime with the open reference module |
| gears_use_qiime_rtax | Boolean | false |  Run fastq files with qiime with the rtax module |
| gears_use_seq_count | Boolean | false | Produces raw count files |

### Result files

The results of a `Gears` run are organised in two folders: `report` and `samples`.
In the `report` folder, one can find the html report (index.html) displaying the summarised results over all samples and providing a navigation view on the taxonomy graph and its result, per sample.
In the `samples` folder, one can find a separate folder for each sample. The individual folders follow the input samples naming and contain the results for each analysis run per sample.

##Example

~~~
OutDir
+-- report
|   +-- index.html
+-- samples
|   +-- <sample_name>
|       +-- centrifuge
|           +-- <sample_name>.centrifuge.gz
|           +-- <sample_name>.centrifuge.kreport
|           +-- <sample_name>.krkn.json
|           +-- <sample_name>.centrifuge_unique.greport
|           +-- <sample_name>.centrifuge_unique.krkn.json
|       +-- kraken
|           +-- <sample_name>.krkn.raw
|           +-- <sample_name>.krkn.full
|           +-- <sample_name>.krkn.json
~~~

The `Gears`-specific results are contained in a folder named after each tool that was used (by default `Gears` uses centrifuge). 
They are stored in the following files:


| File suffix | Application | Content | Description |
| ----------- | ----------- | ------- | ----------- |
| *.centrifuge.gz | centrifuge | tsv | Annotation per sequence (compressed) |
| *.centrifuge.kreport | centrifuge-kreport | tsv | Kraken-style report of the centrifuge output including taxonomy information |
| *.centrifuge.krkn.json | krakenReportToJson | json | JSON representation of the the taxonomy report |
| *.centrifuge_unique.kreport | centrifuge-kreport | tsv | Kraken-style report of the centrifuge output including taxonomy information for the reads with unique taxonomic assignment |
| *.centrifuge_unique.krkn.json | krakenReportToJson | json | JSON represeantation of the taxonomy report for the uniquely mapped reads |

Kraken specific output

| File suffix | Application | Content | Description |
| ----------- | ----------- | ------- | ----------- |
| *.krkn.raw  | kraken      | tsv     | Annotation per sequence |
| *.krkn.full | kraken-report | tsv | List of all annotation possible with counts filled in for this specific sample|
| *.krkn.json | krakenReportToJson | json | JSON representation of the taxonomy report, for postprocessing |

QIIME specific output

| File suffix | Application | Content | Description |
| ----------- | ----------- | ------- | ----------- |
| *.otu_table.biom | qiime | biom | Biom file containing counts for OTUs identified in the input |
| *.otu_map.txt | qiime | tsv | Tab-separated file containing information about which samples a taxon has been identified in |


## Getting Help
For questions about this pipeline and suggestions, we have a GitHub page where you can submit your ideas and thoughts .[GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
