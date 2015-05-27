# Shiva

## Introduction

This pipeline is build for variant calling on NGS data (preferably Illumina data).
It is based on the <a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms of their approach to variant calling.
The pipeline accepts ```.fastq & .bam``` files as input.

----

## Tools for this pipeline

* <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>
* [Flexiprep](flexiprep.md)
* <a href="https://www.broadinstitute.org/gatk/" target="_blank">GATK tools</a>:
    * GATK
    * Freebayes
    * Bcftools
    * Samtools

----

## Example

Note that one should first create the appropriate [configs](../general/config.md).

### Full pipeline

The full pipeline can start from fastq or from bam file. This pipeline will include pre-process steps for the bam files.

To view the help menu, execute:
~~~
java -jar </path/to/biopet.jar> pipeline shiva -h

Arguments for Shiva:
 -sample,--onlysample <onlysample>               Only Sample
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters

~~~

To run the pipeline:
~~~
java -jar </path/to/biopet.jar> pipeline shiva -config MySamples.json -config MySettings.json -run
~~~

A dry run can be performed by simply removing the `-run` flag from the command line call.

### Only variant calling

It is possible to run Shiva while only performing its variant calling steps.
This has been separated in its own pipeline named `shivavariantcalling`.
As this calling pipeline starts from BAM files, it will naturally not perform any pre-processing steps.

To view the help menu, execute:
~~~
java -jar </path/to/biopet.jar> pipeline shivavariantcalling -h

Arguments for ShivaVariantcalling:
 -BAM,--inputbams <inputbams>          Bam files (should be deduped bams)
 -sample,--sampleid <sampleid>         Sample ID (only effects summary and not required)
 -library,--libid <libid>              Library ID (only effects summary and not required)
 -config,--config_file <config_file>   JSON config file(s)
 -DSC,--disablescatter                 Disable all scatters

~~~

To run the pipeline:
~~~
java -jar </path/to/biopet.jar> pipeline shivavariantcalling -config MySettings.json -run
~~~

A dry run can be performed by simply removing the `-run` flag from the command line call.


----

## Variant caller
At this moment the following variant callers can be used

`TODO: explain them briefly`

* haplotypecaller
* haplotypecaller_gvcf
* haplotypecaller_allele
* unifiedgenotyper
* unifiedgenotyper_allele
* bcftools
* freebayes
* raw

----

## Multi-sample and single sample
### Multi-sample
With <a href="https://www.broadinstitute.org/gatk/guide/tagged?tag=multi-sample">multi-sample</a>
 one can perform variant calling with all samples combined for more statistical power and accuracy.


### Single sample
If one prefers single sample variant calling (which is the default) there is no need of setting the joint variant calling inside the config.
The single sample variant calling has 2 modes as well:

`TODO: WHICH MODES THEN?`

----

## Config options

To view all possible config options please navigate to our Gitlab wiki page
<a href="https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline" target="_blank">Config</a>

### Required settings
| Config Name | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| ???? | output_dir | String |  | Path to output directory |
| Shiva | variantcallers | List[String] | | Which variant callers to use |


### Config options

| Config Name | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| shiva | reference | String |  | reference to align to |
| shiva | dbsnp | String |  | vcf file of dbsnp records |
| shiva | variantcallers | List[String] |  | variantcaller to use, see list |
| shiva | multisample_sample_variantcalling | Boolean | true |  |
| shiva | single_sample_variantcalling | Boolean | false |  |
| shiva | library_variantcalling | Boolean | false |  |


**Config example**

```json
{ 
    "samples": {
	    "SampleID": {
		    "libraries": { 
			    "lib_id_1": { "bam": "YoureBam.bam" },
			    "lib_id_2": { "R1": "file_R1.fq.gz", "R2": "file_R2.fq.gz" }
	        }
	    }
    },
    "reference": "<location of fasta of reference>",
    "variantcallers": [ "haplotypecaller", "unifiedgenotyper" ],
    "output_dir": "<output directory>"
}
```

## References
