# Gears

## Introduction
Gears is a metagenomics pipeline. (``GE``nome ``A``nnotation of ``R``esidual ``S``equences). One can use this pipeline to identify contamination in sequencing runs on either raw FastQ files or BAM files.
In case of BAM file as input, it will extract the unaligned read(pair) sequences for analysis.

Analysis result is reported in a sunburst graph, which is visible and navigatable in a webbrowser.

Pipeline analysis components include:
 
 - Kraken, DerrickWood [GitHub](https://github.com/DerrickWood/kraken)


## Example

To get the help menu:

``` bash
biopet pipeline Gears -h

... default config ...

Arguments for Gears:
 -R1,--fastqr1 <fastqr1>               R1 reads in FastQ format
 -R2,--fastqr2 <fastqr2>               R2 reads in FastQ format
 -bam,--bamfile <bamfile>              All unmapped reads will be extracted from this bam for analysis
 --outputname <outputname>             Undocumented option
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or 
                                       'path:path:key=value'
 -DSC,--disablescatter                 Disable all scatters

```

Note that the pipeline also works on unpaired reads where one should only provide R1.


To start the pipeline (remove `-run` for a dry run):

``` bash
biopet pipeline Gears -run  \
-R1 myFirstReadPair -R2 mySecondReadPair -sample mySampleName \
-library myLibname -config mySettings.json
```


## Configuration and flags
For technical reasons, single sample pipelines, such as this pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.

Command line flags for Gears are:

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -R1 | --input_r1 | Path (optional) | Path to input fastq file |
| -R2 | --input_r2 | Path (optional) | Path to second read pair fastq file. |
| -bam | --bamfile | Path (optional) | Path to bam file. |
| -sample | --sampleid | String (**required**) | Name of sample |
| -library | --libid | String (**required**) | Name of library |

If `-R2` is given, the pipeline will assume a paired-end setup. `-bam` is mutualy exclusive with the `-R1` and `-R2` flags. Either specify `-bam` or `-R1` and/or `-R2`.

### Config



## Result files

The results of `Gears` are stored in the following files:

| File suffix | Application | Content | Description |
| ----------- | ----------- | ------- | ----------- |
| *.krkn.raw  | kraken      | tsv     | Annotation per sequence |
| *.krkn.full | kraken-report | tsv | List of all annotation possible with counts filled in for this specific sample|
| *.krkn.json | krakenreport2json| json | JSON representation of the taxonomy report, for postprocessing |

In a seperate `report` folder, one can find the html report displaying the summary and providing a navigation view on the taxonomy graph and (its) result.

## Getting Help
For questions about this pipeline and suggestions, we have a GitHub page where you can submit your ideas and thoughts .[GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
