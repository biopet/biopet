# GATK-pipeline

## Introduction

The GATK-pipeline is build for variant calling on NGS data (preferably Illumina data).
It is based on the <a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms of there approach to variant calling.
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
To check if your pipeline can create all the jobs (dry run) remove the `-run`:
~~~
java -jar Biopet.0.2.0.jar pipeline gatkPipeline -config MySamples.json -config MySettings.json -outDir myOutDir
~~~

## Results

### Result files
~~~
.
└── samples
    ├── my_sample1
    │   ├── run_lib1
    │   │   ├── chunks
    │   │   │   ├── 1
    │   │   │       └── flexiprep
    │   │   │
    │   │   │
    │   │   │
    │   │   ├── flexiprep
    │   │   │   ├── input.R1.fastqc
    │   │   │   │   └── input.R1_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   ├── input.R1.qc.fastqc
    │   │   │   │   └── input.R1.qc_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   ├── input.R2.fastqc
    │   │   │   │   └── input.R2_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   └── input.R2.qc.fastqc
    │   │   │       └── input.R2.qc_fastqc
    │   │   │           ├── Icons
    │   │   │           └── Images
    │   │   └── metrics
    │   ├── run_lib2
    │   │   ├── chunks
    │   │   │   ├── 1
    │   │   │       └── flexiprep
    │   │   │
    │   │   ├── flexiprep
    │   │   │   ├── input.R1.fastqc
    │   │   │   │   └── input.R1_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   ├── input.R1.qc.fastqc
    │   │   │   │   └── input.R1.qc_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   ├── input.R2.fastqc
    │   │   │   │   └── input.R2_fastqc
    │   │   │   │       ├── Icons
    │   │   │   │       └── Images
    │   │   │   └── input.R2.qc.fastqc
    │   │   │       └── input.R2.qc_fastqc
    │   │   │           ├── Icons
    │   │   │           └── Images
    │   │   └── metrics
    │   └── variantcalling
~~~


### Best practice

## References