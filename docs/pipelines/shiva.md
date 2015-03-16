# Shiva

## Introduction

This pipeline is build for variant calling on NGS data (preferably Illumina data).
It is based on the <a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms of there approach to variant calling.
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

To get the help menu:
~~~
java -jar </path/to/biopet.jar> pipeline shiva -h

Arguments for Shiva:
 -sample,--onlysample <onlysample>               Only Sample
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters

~~~

To run the pipeline:
~~~
java -jar </path/to/biopet.jar> pipeline shiva -run -config MySamples.json -config MySettings.json
~~~

To perform a dry run simply remove `-run` from the commandline call. 

----

## Variantcaller
At this moment the following variantcallers modes can be used

* haplotypecaller
* haplotypecaller_gvcf
* haplotypecaller_allele
* unifiedgenotyper
* unifiedgenotyper_allele
* bcftools
* freebayes
* raw

----

## Multisample and Singlesample
### Multisample
With <a href="https://www.broadinstitute.org/gatk/guide/tagged?tag=multi-sample">multisample</a>
 one can perform variantcalling with all samples combined for more statistical power and accuracy.


### Singlesample
If one prefers single sample variantcalling (which is the default) there is no need of setting the joint_variantcalling inside the config.
The single sample variantcalling has 2 modes as well:


----

## Config options

To view all possible config options please navigate to our Gitlab wiki page
<a href="https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline" target="_blank">Config</a>

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
    "variantcallers": [ "haplotypecaller", "unifiedgenotyper" ],
    "output_dir": "<output directory>"
}
```

## References
