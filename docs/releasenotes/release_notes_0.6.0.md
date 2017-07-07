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
* [Gentrap](../pipelines/multisample/gentrap.md): now uses shiva for variantcalling and produce multisample vcf files
* Added Bowtie 2
* Added fastq validator, flexiprep now aborts when a input file is corrupted
* Added optional vcf validator step in shiva
* Added optional Varda step in Toucan
* Added trimming of reverse complement adapters (flexiprep does this automatic)
* Added [Tinycap](../pipelines/multisample/tinycap.md) for smallRNA analysis
* [Gentrap](../pipelines/multisample/gentrap.md): Refactoring changed the "expression_measures" options
* Fixed biopet logging
* Added sample tagging
* Seqstat now reports histogram of read lengths
* Fixed bug in seqstat when having multiple sizes exists in the fastq file
* Added variant plots for targets to report of Shiva
* Adapter feed to cutadapt now use only that parts that are reported by fastqc and not the full sequence
* Added a reference selector when fasta file can't be found. User now get a list of available species and genomes in the config
* Fixed bcftools with IUPAC symbols

## Infrastructure changes

* Development environment within the LUMC is now tested with Jenkins
    * Added integration tests for Gentrap
    * Added integration tests for Gears
    * Added general MultisampleMapping testing
