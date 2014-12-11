# Introduction
The mapping pipeline has been created for NGS users who want to align there data with the most commonly used alignment programs.
The pipeline performs a quality control (QC) on the raw fastq files with our [Flexiprep](flexiprep.md) pipeline. 
After the QC, the pipeline simply maps the reads with the chosen aligner. The resulting BAM files will be sorted on coordinates and indexed, for downstream analysis.

----

## Tools for this pipeline:

* [Flexiprep](flexiprep.md)
* Alignment programs:
    * <a href="http://bio-bwa.sourceforge.net/bwa.shtml" target="_blank">BWA</a>
    * <a href="http://bowtie-bio.sourceforge.net/index.shtml" target="_blank">Bowtie version 1.1.1</a>
    * <a href="http://www.well.ox.ac.uk/project-stampy" target="_blank">Stampy</a>
    * <a href="https://github.com/alexdobin/STAR" target="_blank">Star</a>
    * <a href="https://github.com/alexdobin/STAR" target="_blank">Star-2pass</a>
* <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>

----

## Example
Note that one should first create the appropriate [configs](../general/config.md).

For the help menu:
~~~
java -jar Biopet-0.2.0.jar pipeline mapping -h

Arguments for Mapping:
 -R1,--input_r1 <input_r1>                       R1 fastq file
 -outDir,--output_directory <output_directory>   Output directory
 -R2,--input_r2 <input_r2>                       R2 fastq file
 -outputName,--outputname <outputname>           Output name
 -skipflexiprep,--skipflexiprep                  Skip flexiprep
 -skipmarkduplicates,--skipmarkduplicates        Skip mark duplicates
 -skipmetrics,--skipmetrics                      Skip metrics
 -ALN,--aligner <aligner>                        Aligner
 -R,--reference <reference>                      Reference
 -chunking,--chunking                            Chunking
 -numberChunks,--numberchunks <numberchunks>     Number of chunks, if not defined pipeline will automatically calculate the number of chunks
 -RGID,--rgid <rgid>                             Readgroup ID
 -RGLB,--rglb <rglb>                             Readgroup Library
 -RGPL,--rgpl <rgpl>                             Readgroup Platform
 -RGPU,--rgpu <rgpu>                             Readgroup platform unit
 -RGSM,--rgsm <rgsm>                             Readgroup sample
 -RGCN,--rgcn <rgcn>                             Readgroup sequencing center
 -RGDS,--rgds <rgds>                             Readgroup description
 -RGDT,--rgdt <rgdt>                             Readgroup sequencing date
 -RGPI,--rgpi <rgpi>                             Readgroup predicted insert size
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters
~~~

To run the pipeline:
~~~
java -jar Biopet.0.2.0.jar pipeline mapping -run --config mySettings.json \
-R1 myReads1.fastq -R2 myReads2.fastq -outDir myOutDir -OutputName myReadsOutput \
-R hg19.fasta -RGSM mySampleName -RGLB myLib1
~~~
Note that removing -R2 causes the pipeline to be able of handlind single end `.fastq` files.

To perform a dry run simply remove `-run` from the commandline call.

----

## Examine results

## Result files
~~~
├── OutDir
    ├── <samplename>-lib_1.dedup.bai
    ├── <samplename>-lib_1.dedup.bam
    ├── <samplename>-lib_1.dedup.metrics
    ├── flexiprep
    └── metrics
~~~


## Best practice

## References