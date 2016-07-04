# Release notes Biopet version 0.6.0

## General Code changes

* Switch to full public licence
* Upgrade to Queue 3.6
 * Upgrade to java 8
 * Upgrade to Picard / htsjdk 2.4.1

## Functionality

* [Gears](../pipelines/gears.md): Added `pick_open_reference_otus` reference module of [Qiime](http://qiime.org/)
* Fixed default aligner in [Gentrap](../pipelines/gentrap.md) to gsnap
* Make `sample` and `library id` required in [Flexiprep](../pipelines/flexiprep.md) when started from the `CLI`
* Raise some default memory limits ([#356](https://git.lumc.nl/biopet/biopet/issues/356))
* [Carp](../pipelines/carp.md): Our MACS2 wrapper now auto-detects whether a sample is single-end or paired-end
* Added a `sort by name` step when htseq in Gentrap is executed
* Fixed file name of bam files in Carp
* VcfWithVcf now checks if chromosomes are in the correct reference
* Added sync stats to flexiprep report
* Added check in BamMetrics to check a given bed file is according to the reference that is used on init of pipeline
* [TinyCap](../pipelines/tinycap.md) has now validated settings for miRNA runs. Some parameters changed for alignment.
* [Flexiprep](../pipelines/flexiprep.md) has the option to provide custom adapters sequences and ignoring adapters found by `FastQC`. 
* Utils - BamUtils is now estimating insertsize by sampling the bam-file randomly.
* Fix in VCF filter (#370)[https://git.lumc.nl/biopet/biopet/merge_requests/370]
* Fix Star wrapper - added full functionality in wrapper.

## Reporting

* Report now corrects for secondary reads in alignment stats
* ShivaSVcalling, fixing the headers in the output files for Clever, Breakdancer and Pindel.
* [Flexiprep](../pipelines/flexiprep.md) now reports the histogram of sizes from clipped adapters. (raw json output)
* Fix reported mapping percentage in case of allowing multimapping (in RNAseq aligners)

## Backward incompatible changes

* Changing `namespace` in config values. [!348](https://git.lumc.nl/biopet/biopet/merge_requests/348)
