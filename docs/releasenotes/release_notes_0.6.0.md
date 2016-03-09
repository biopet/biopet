# Release notes Biopet version 0.6.0

## General Code changes

* Refactoring Gentrap, It's modules can now be used outside of gentrap also
* Added more unit testing
* Upgrade to Queue 3.5
* MultisampleMapping is now a base for all multisample pipelines with a default alignment step

## Functionality

* [Gears](../pipelines/gears.md): Metagenomics NGS data. Added support for 16S with Kraken and Qiime
* Raise an exception at the beginning of each pipeline when not using absolute paths
* Moved Varscan from Gentrap to Shiva (Varscan can still be used inside Gentrap)
* [Gentrap](../pipelines/gentrap.md): now uses shiva for variantcalling and produce multisample vcf files
* Added Bowtie 2
* Added fastq validator, flexiprep now aborts when a input file is corrupted
* Added optional vcf validator step in shiva
* Added optional Varda step in Toucan
* Added trimming of reverse complement adapters (flexiprep does this automatic)
* Added [Tinycap](../pipelines/tinycap.md) for smallRNA analysis
* [Gentrap](../pipelines/gentrap.md): Refactoring changed the "expression_measures" options

## Infrastructure changes

* Development environment within the LUMC is now tested with Jenkins
    * Added integration tests for Gentrap
    * Added integration tests for Gears
    * Added general MultisampleMapping testing
