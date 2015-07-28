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

## Config options

To view all possible config options please navigate to our Gitlab wiki page
<a href="https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline" target="_blank">Config</a>

### Required settings
| Namespace | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| - | output_dir | String |  | Path to output directory |
| Shiva | variantcallers | List[String] | | Which variant callers to use |


### Config options

| Namespace | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| shiva | reference_fasta | String |  | reference to align to |
| shiva | dbsnp | String |  | vcf file of dbsnp records |
| shiva | variantcallers | List[String] |  | variantcaller to use, see list |
| shiva | use_indel_realigner | Boolean | true | Realign indels |
| shiva | use_base_recalibration | Boolean | true | Base recalibrate |
| shiva | use_analyze_covariates | Boolean | false | Analyze covariates during base recalibration step |
| shiva | bam_to_fastq | Boolean | false | Convert bam files to fastq files |
| shiva | correct_readgroups | Boolean | false | Attempt to correct read groups |
| shiva | amplicon_bed | Path | Path to target bed file |
| shiva | regions_of_interest | Array of paths | Array of paths to region of interest (e.g. gene panels) bed files |
| vcffilter | min_sample_depth | Integer | 8 | Filter variants with at least x coverage |
| vcffilter | min_alternate_depth | Integer | 2 | Filter variants with at least x depth on the alternate allele |
| vcffilter | min_samples_pass | Integer | 1 | Minimum amount of samples which pass custom filter (requires additional flags) |
| vcffilter | filter_ref_calls | Boolean | true | Remove reference calls |
| vcfstats | reference | String | Path to reference to be used by `vcfstats` |

Since Shiva uses the [Mapping](mapping.md) pipeline internally, mapping config values can be specified as well.
For all the options, please see the corresponding documentation for the mapping pipeline.

### Modes

Shiva furthermore supports three modes. The default and recommended option is `multisample_variantcalling`.
During this mode, all bam files will be simultaneously called in one big VCF file. It will work with any number of samples.

On top of that, Shiva provides two separate modes that only work with a single sample.
Those are not recommend, but may be useful to those who need to validate replicates.

Mode `single_sample_variantcalling` calls a single sample as a merged bam file.
I.e., it will merge all libraries in one bam file, then calls on that.

The other mode, `library_variantcalling`, will call simultaneously call all library bam files.

The config for these therefore is:

| Namespace | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| shiva | multisample_variantcalling | Boolean | true | Default, multisample calling |
| shiva | single_sample_variantcalling | Boolean | false | Not-recommended, single sample, merged bam |
| shiva | library_variantcalling | Boolean | false | Not-recommended, single sample, per library |


**Config example**

```json
{ 
    "samples": {
	    "SampleID": {
		    "libraries": { 
			    "lib_id_1": { "bam": "YourBam.bam" },
			    "lib_id_2": { "R1": "file_R1.fq.gz", "R2": "file_R2.fq.gz" }
	        }
	    }
    },
    "shiva": {
        "reference": "<location of fasta of reference>",
        "variantcallers": [ "haplotypecaller", "unifiedgenotyper" ],
        "dbsnp": "</path/to/dbsnp.vcf>",
        "vcffilter": {
            "min_alternate_depth": 1
        }
    },
    "output_dir": "<output directory>"
}
```

## References
