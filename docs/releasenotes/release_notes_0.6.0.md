# Release notes Biopet version 0.5.0

## General Code changes

    * Refactoring Gentrap, It's modules can now be used outside of gentrap also
    * Added more unit testing
    * Upgrade to Queue 3.5
    * MultisampleMapping is now a base for all multisample pipelines with a default alignment step

## Functionality

    * Added Gears pipeline to support multisample metagenomics NGS data. It can support WGS with Kraken and 16s with Kraken and Qiime
    * Raise an exception at the beginning of each pipeline when not using absolute paths
    * Moved Varscan from Gentrap to Shiva (Varscan can still be used inside Gentrap)
    * Gentrap now uses shiva for variantcalling and produce multisample vcf files
    * Added bowtie 2
    * Added fastq validator, flexiprep now aborts when a input file is corrupted
    * Added optional vcf validator step in shiva
    * Added optional Varda step in Toucan

## Infrastructure changes

* Development environment within the LUMC is now tested with Jenkins
    * Added integration tests for Gentrap
    * Added integration tests for Gears
    * Added general MultisampleMapping testing
