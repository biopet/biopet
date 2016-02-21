# Release notes Biopet version 0.5.0

## General Code changes

    * Refactoring Gentrap, not the modules can be used outside of gentrap also
    * Added more unit testing
    * Upgrade to Queue 3.5
    * MultisampleMapping is now a base for all multisample pipeline with a default alignment step

## Functionality
    * Added Gears pipeline to support multisample metagenomis NGS data. It can support WGS with kraken and 16s with kraken and Qiime
    * Raise expection at beginning of pipeline when not using absolute paths
    * Added Varscan to shiva
    * Gentrap now uses shiva for variantcalling and prodice multisample vcf files
    * Added bowtie 2
    * Added fastq validator, flexiprep now aborts when a input file is corrupt
    * Adding optional vcf validator step in shiva
    * Adding optional Varda step in Toucan

## Infrastructure changes

* Development environment within the LUMC now get tested with Jenkins
    * Added integration tests Gentrap
    * Added integration tests Gears
    * Adding general multisample mapping testing
