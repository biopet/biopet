# Release notes Biopet version 0.7.0

## General Code changes

* Switched to full public licence
* Upgraded to Queue 3.6
 * Upgraded to java 8
 * Upgraded to Picard / htsjdk 2.4.1

## Functionality

* [Gears](../pipelines/gears.md): Added `pick_open_reference_otus` reference module of [Qiime](http://qiime.org/)
* Fixed default aligner in [Gentrap](../pipelines/gentrap.md) to gsnap
* Make `sample` and `library id` required in [Flexiprep](../pipelines/flexiprep.md) when started from the `CLI`
* [Core] Raised some default memory limits ([#356](https://git.lumc.nl/biopet/biopet/issues/356))
* [Carp](../pipelines/carp.md): Our MACS2 wrapper now auto-detects whether a sample is single-end or paired-end
* Added a `sort by name` step when htseq in Gentrap is executed
* Fixed file name of bam files in Carp
* VcfWithVcf now checks if chromosomes are in the correct reference
* Added sync stats to flexiprep report
* Added check in BamMetrics to check whether contigs a given bed file are defined in the used reference-genome.
* [TinyCap](../pipelines/tinycap.md) now has validated settings for miRNA runs. Some parameters changed for alignment.
* [Flexiprep](../pipelines/flexiprep.md) now has the option to provide custom adapters sequences and ignoring adapters found by `FastQC`. 
* Utils - BamUtils is now estimating insert size by sampling the bam-file taking all parts of the available contigs.
* Fix in VCF filter (#370)[https://git.lumc.nl/biopet/biopet/merge_requests/370]
* Fix Star wrapper - added full functionality in wrapper.
* Added a new tools: BamStats

## Reporting

* Report now corrects for secondary reads in alignment stats
* ShivaSVcalling, fixed the headers in the output vcf-files for Clever, Breakdancer and Pindel.
* [Flexiprep](../pipelines/flexiprep.md) now reports the histogram of sizes from clipped adapters. (raw json output)
* Fix reported mapping percentage in case of allowing multimapping (in RNAseq aligners)

## Backward incompatible changes

* Changing `namespace` in config values. [!348](https://git.lumc.nl/biopet/biopet/merge_requests/348)
  The nomenclature regarding namespaces in configuration files and options has now been harmonized. Whereas this was previously variously called "submodule", "level", or "name(space)", it is now called "namespace" everywhere. This means the config function used for accessing configuration values changed argument name submodule to namespace. Any projects depending on Biopet will therefore have to refactor their usage of this function.
