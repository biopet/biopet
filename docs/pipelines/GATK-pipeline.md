# GATK-pipeline

## Introduction

The GATK-pipeline is build for variant calling on NGS data (preferably Illumina data).
It uses the <a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms of there approach to variant calling.
The pipeline accepts ```.fastq & .bam``` files as input.

## Tools for this pipeline

* <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>
* [Flexiprep](flexiprep.md)
* <a href="https://www.broadinstitute.org/gatk/" target="_blank">GATK tools</a>:
    * Realignertargetcreator
    * Indelrealigner
    * Baserecalibrator
    * Printreads
    * Splitncigarreads
    * Haplotypecaller
    * Variantrecalibrator
    * Applyrecalibration
    * Genotypegvcfs
    * Variantannotator

## Example

Note that one should first create the appropriate [configs](../config.md).

To get the help menu:
~~~
java -jar Biopet.0.2.0.jar pipeline gatkPipeline -h

Arguments for GatkPipeline:
 -outDir,--output_directory <output_directory>   Output directory
 -sample,--onlysample <onlysample>               Only Sample
 -skipgenotyping,--skipgenotyping                Skip Genotyping step
 -mergegvcfs,--mergegvcfs                        Merge gvcfs
 -jointVariantCalling,--jointvariantcalling      Joint variantcalling
 -jointGenotyping,--jointgenotyping              Joint genotyping
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters

~~~

To run the pipeline:
~~~
java -jar Biopet.0.2.0.jar pipeline gatkPipeline -run -config MySamples.json -config MySettings.json -outDir myOutDir
~~~

For LUMC/researchSHARK users there is a module available that sets all your environment settings and default executables/settings.
~~~
module load Biopet/0.2.0
biopet pipeline gatkPipeline -run -config MySamples.json -config MySettings.json -outDir myOutDir
~~~

## Examine results

### Result files

### Best practice

## References