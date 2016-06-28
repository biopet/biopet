# Release notes Biopet version 0.6.0

## General Code changes

* Switch to full public licence
* Upgrade to Queue 3.6
 * Upgrade to java 8
 * Upgrade to PIcard / htsjdk 2.4.1

## Functionality

* [Gears](../pipelines/gears.md): Added open reference module of Qiime(http://qiime.org/)
* Fixed default aligner in [Gentrap](../pipelines/gentrap.md) to gsnap
* Makes sample and library id required in [Flexiprep](../pipelines/flexiprep.md)
* Raise some default memory limits
* Fixed a non-required value in Shiva
* Added an auto detect for MACS2 if sample is single-end or paired-end
* Added a sort by name step when htseq in Gentrap is executed
* Fixed file name of bam files in Carp
* Report now corrects for secondary reads in alignment stats
* VcfWithVcf now checks if chromosomes are in the correct reference
* Added sync stats to flexiprep report
* Added check in BamMetrics to check a given bed file is acording to the reference that is used on init of pipeline