# Introduction

## <a href="https://git.lumc.nl/biopet/biopet/tree/develop/protected/basty/src/main/scala/nl/lumc/sasc/biopet/pipelines/basty" target="_blank">Basty</a>
A pipeline for aligning bacterial genomes and detect structural variations on the level of SNPs. Basty will output phylogenetic trees.
Which makes it very easy to look at the variations between certain species.

# Tools for this pipeline
* [GATK-pipeline](GATK-pipeline.md)
* [BastyGenerateFasta](../tools/BastyGenerateFasta.md)
* <a href="http://sco.h-its.org/exelixis/software.html" target="_blank">RAxml</a>
* <a href="https://github.com/sanger-pathogens/Gubbins" target="_blank">Gubbins</a>

# Invocation

~~~
java -jar Biopet.0.2.0.jar pipeline basty
~~~


# Example
To run for a specific species, please do not forget to create the proper index files:

* ```.dict``` (can be produced with <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>)
* ```.fai``` (can be produced with <a href="http://samtools.sourceforge.net/samtools.shtml" target="_blank">Samtools faidx</a> 
* ```.idxSpecificForAligner``` (depending on which aligner is used one should create a suitable index specific for that aligner. 
Each aligner has his own way of creating index files. Therefore the options for creating the index files can be found inside the aligner itself)

# Testcase A


# Examine results


## Result files
The output files this pipeline produces are:

* A complete output from [Flexiprep](flexiprep.md)
* BAM files, produced with the mapping pipeline. (either BWA, Bowtie, Stampy, Star and Star 2-pass. default: BWA)
* VCF file from all samples together 
* The output from the tool [BastyGenerateFasta](../tools/BastyGenerateFasta.md)
    * FASTA containing variants only
    * FASTA containing all the consensus sequences based on min. coverage (default:8) but can be modified in the config
* A phylogenetic tree based on the variants called with the GATK-pipeline generated with the tool [BastyGenerateFasta](../tools/BastyGenerateFasta.md)


## Best practice


# References
