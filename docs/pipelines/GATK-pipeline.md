# GATK-pipeline

## Introduction

The GATK-pipeline is build for variant calling on NGS data (preferably Illumina data).
It is based on the <a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms of there approach to variant calling.
The pipeline accepts ```.fastq & .bam``` files as <samplename>.

----

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

----

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

To perform a dry run simply remove `-run` from the commandline call. 

----

## Multisample and Singlesample
### Multisample
With <a href="https://www.broadinstitute.org/gatk/guide/tagged?tag=multi-sample">multisample</a>
 one can perform variantcalling with all samples combined for more statistical power and accuracy.
To Enable this option one should enable the following option `"joint_variantcalling":true` in the settings config file. 


### Singlesample
If one prefers single sample variantcalling (which is the default) there is no need of setting the joint_variantcalling inside the config.
The single sample variantcalling has 2 modes as well:

* "single_sample_calling":true (default)
* "single_sample_calling":false which will give the user only the raw VCF, produced with [MpileupToVcf](../tools/MpileupToVcf.md)


----

## Config options

To view all possible config options please navigate to our Gitlab wiki page
<a href="https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline" target="_blank">Config</a>

### Config options

| Config Name | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| gatk | referenceFile | String |  |  |
| gatk | dbsnp | String |  |  |
| gatk | <samplename>type | String | DNA |  |
| gatk | gvcfFiles | Array[String] |  |  |


**Sample config**

| Config Name | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| samples | ---- | String | ---- | ---- |
| SampleID | ---- | String | ---- | ---- |
| libraries | ---- | String | ---- | specify samples within the same library |
| lib_id | ---- | String | ---- | fill in you're library id |

```
{ "samples": {
	"SampleID": {
		"libraries": { 
			"lib_id": {"bam": "YoureBam.bam"},
			"lib_id": {"bam": "YoureBam.bam"}
	}}
}}
```


**Run config**


| Config Name | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| run->RunID | ID | String |  | Automatic filled by sample json layout |
| run->RunID | R1 | String |  |  |
| run->RunID | R2 | String |  |  |

---

### sub Module options


This can be used in the root of the config or within the gatk, within mapping got prio over the root value. Mapping can also be nested in gatk. For options for mapping see: https://git.lumc.nl/biopet/biopet/wikis/Flexiprep-Pipeline

| Config Name | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| realignertargetcreator | scattercount | Int |  |  |
| indelrealigner | scattercount | Int |  |  |
| baserecalibrator | scattercount | Int | 2 |  |
| baserecalibrator | threads | Int |  |  |
| printreads | scattercount | Int |  |  |
| splitncigarreads | scattercount | Int |  |  |
| haplotypecaller | scattercount | Int |  |  |
| haplotypecaller | threads | Int | 3 |  |
| variantrecalibrator | threads | Int | 4 |  |
| variantrecalibrator | minnumbadvariants | Int | 1000 |  |
| variantrecalibrator | maxgaussians | Int | 4 |  |
| variantrecalibrator | mills | String |  |  |
| variantrecalibrator | hapmap | String |  |  |
| variantrecalibrator | omni | String |  |  |
| variantrecalibrator | 1000G | String |  |  |
| variantrecalibrator | dbsnp | String |  |  |
| applyrecalibration | ts_filter_level | Double | 99.5(for SNPs) or 99.0(for indels) |  |
| applyrecalibration | scattercount | Int |  |  |
| applyrecalibration | threads | Int | 3 |  |
| genotypegvcfs | scattercount | Int |  |  |
| variantannotator | scattercount | Int |  |  |
| variantannotator | dbsnp | String  |  |

----

## Results

The main output file from this pipeline is the final.vcf which is a combined VCF of the raw and discovery VCF.

   - Raw VCF: VCF file created from the mpileup file with our own tool called: [MpileupToVcf](../tools/MpileupToVcf.md)
   - Discovery VCF: Default VCF produced by the haplotypecaller

### Result files
~~~bash
├─ samples
   ├── <samplename>
   │   ├── run_lib_1
   │   │   ├── <samplename>-lib_1.dedup.bai
   │   │   ├── <samplename>-lib_1.dedup.bam
   │   │   ├── <samplename>-lib_1.dedup.metrics
   │   │   ├── <samplename>-lib_1.dedup.realign.baserecal
   │   │   ├── <samplename>-lib_1.dedup.realign.baserecal.bai
   │   │   ├── <samplename>-lib_1.dedup.realign.baserecal.bam
   │   │   ├── flexiprep
   │   │   └── metrics
   │   ├── run_lib_2
   │   │   ├── <samplename>-lib_2.dedup.bai
   │   │   ├── <samplename>-lib_2.dedup.bam
   │   │   ├── <samplename>-lib_2.dedup.metrics
   │   │   ├── <samplename>-lib_2.dedup.realign.baserecal
   │   │   ├── <samplename>-lib_2.dedup.realign.baserecal.bai
   │   │   ├── <samplename>-lib_2.dedup.realign.baserecal.bam
   │   │   ├── flexiprep
   │   │   └── metrics
   │   └── variantcalling
   │       ├── <samplename>.dedup.realign.bai
   │       ├── <samplename>.dedup.realign.bam
   │       ├── <samplename>.final.vcf.gz
   │       ├── <samplename>.final.vcf.gz.tbi
   │       ├── <samplename>.hc.discovery.gvcf.vcf.gz
   │       ├── <samplename>.hc.discovery.gvcf.vcf.gz.tbi
   │       ├── <samplename>.hc.discovery.variants_only.vcf.gz.tbi
   │       ├── <samplename>.hc.discovery.vcf.gz
   │       ├── <samplename>.hc.discovery.vcf.gz.tbi
   │       ├── <samplename>.raw.filter.variants_only.vcf.gz.tbi
   │       ├── <samplename>.raw.filter.vcf.gz
   │       ├── <samplename>.raw.filter.vcf.gz.tbi
   │       └── <samplename>.raw.vcf
~~~

----

### Best practice

## References