# Shiva

## Introduction

This pipeline is built for variant calling on NGS data (preferably Illumina data). Part of this pipeline resembles the 
<a href="https://www.broadinstitute.org/gatk/guide/best-practices" target="_blank">best practices</a>) of GATK in terms 
of their approach to variant calling. The pipeline accepts `.fastq` & `.bam` files as input.

----

## Overview of tools and sub-pipelines for this pipeline

* [Flexiprep for QC](../flexiprep.md)
* [Metagenomics analysis](../gears.md)
* [Mapping](../mapping.md)
* [VEP annotation](../toucan.md)
* [CNV analysis](../kopisu.md)
* <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>
* <a href="https://www.broadinstitute.org/gatk/" target="_blank">GATK tools</a>
* <a href="https://github.com/ekg/freebayes" target="_blank">Freebayes</a>
* <a href="http://dkoboldt.github.io/varscan/" target="_blank">Varscan</a>
* <a href="https://samtools.github.io/bcftools/bcftools.html" target="_blank">Bcftools</a>
* <a href="http://www.htslib.org/" target="_blank">Samtools</a>

----

## Basic usage

Note that one should first create the appropriate sample and pipeline setting [configs](../../general/config.md).

Shiva pipeline can start from FASTQ or BAM files. This pipeline will include pre-process steps for the BAM files. 

When using BAM files as input, note that one should alter the sample config field from `R1` into `bam`.

To view the help menu, execute:
~~~
biopet pipeline shiva -h
 
Arguments for Shiva:
 -s,--sample <sample>                  Only Process This Sample
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or 
                                       'namespace:namespace:key=value'
 -DSC,--disablescatter                 Disable all scatters

~~~

To run the pipeline:
~~~
biopet pipeline shiva -config MySamples.yml -config MySettings.yml -run
~~~

A dry run can be performed by simply removing the `-run` flag from the command line call.


An example of MySettings.yml file is provided here and more detailed config options are explained in [config options](#config-options).

```yaml
samples:
    SampleID:
        libraries:
            lib_id_1:
                bam: YourBam.bam
            lib_id_2:
                R1: file_R1.fq.gz
                R2: file_R2.fq.gz
species: H.sapiens
reference_name: GRCh38_no_alt_analysis_set
dbsnp_vcf: <dbsnp.vcf.gz>
vcffilter:
    min_alternate_depth: 1
output_dir: <output directory>
variantcallers:
    - haplotypecaller
    - unifiedgenotyper
    - haplotypecaller_gvcf
unifiedgenotyper:
    merge_vcf_results: false # This will do the variantcalling but will not merged into the final vcf file
```
----

## Supported variant callers
At this moment the following variant callers can be used

##### Germline

When doing variant calling most often germline is used. This will detect variants based on the assumption that there is a fixed number of alleles. Mostly the default used is a ploidy of 2. When this assumption does not hold for your data, somatic variant calling can be a better solution.

| ConfigName | Tool | Description |
| ---------- | ---- | ----------- |
| haplotypecaller_gvcf | <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller</a> | Running HaplotypeCaller in gvcf mode |
| haplotypecaller_allele | <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller</a> | Only genotype a given list of alleles with HaplotypeCaller |
| unifiedgenotyper_allele | <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php">unifiedgenotyper</a> | Only genotype a given list of alleles with UnifiedGenotyper |
| unifiedgenotyper | <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php">unifiedgenotyper</a> | Running default UnifiedGenotyper |
| haplotypecaller | <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller</a> | Running default HaplotypeCaller |
| freebayes | <a href="https://github.com/ekg/freebayes">freebayes</a> |  |
| raw | [Naive variant caller](../../tools/MpileupToVcf) |  |
| bcftools | <a href="https://samtools.github.io/bcftools/bcftools.html">bcftools</a> |  |
| bcftools_singlesample | <a href="https://samtools.github.io/bcftools/bcftools.html">bcftools</a> |  |
| varscan_cns_singlesample | <a href="http://varscan.sourceforge.net/">varscan</a> |  |

##### Somatic

In contrast to germline variant calling, somatic variant calling does not have a direct assumption about the number of alleles. Some can also take a control into account, like MuTect2. Having a control is useful when analysing tumor samples.

| ConfigName | Tool | Description |
| ---------- | ---- | ----------- |
| mutect2 | <a href="https://software.broadinstitute.org/gatk/gatkdocs/3.7-0/org_broadinstitute_gatk_tools_walkers_cancer_m2_MuTect2.php">MuTect2</a> | Running mutect2, requires tumor normal pairs |
| varscan_cns_singlesample | <a href="http://varscan.sourceforge.net/">varscan</a> |  |
| raw | [Naive variant caller](../../tools/MpileupToVcf) |  |

###### Config for tumor-normal pairs

To define the tumor-normal pairs, the config can look like this:

```yaml
samples:
  sample1:
    tags:
      type: tumor
      control: sample2
  sample2:
    tags:
      type: control
```

## Config options

### Required settings
| ConfigNamespace | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| - | output_dir | String |  | Path to output directory |
| Shiva | variantcallers | List[String] | | Which variant callers to use |

### Config options

| ConfigNamespace | Name |  Type | Default | Function | Applicable variant caller |
| ----------- | ---- | ----- | ------- | -------- | -------- |
| shiva | species | String | unknown_species | Name of species, like H.sapiens | all |
| shiva | reference_name | String | unknown_reference_name | Name of reference, like hg19 | all |
| shiva | reference_fasta | String |  | reference to align to | all |
| shiva | dbsnp_vcf | String |  | vcf file of dbsnp records | haplotypecaller, haplotypecaller_gvcf, haplotypecaller_allele, unifiedgenotyper, unifiedgenotyper_allele |
| shiva | variantcallers | List[String] |  | variantcaller to use, see list | all |
| shiva | input_alleles | String |  | vcf file contains sites of interest for genotyping (including HOM REF calls). Only used when haplotypecaller_allele or unifiedgenotyper_allele is used.  | haplotypecaller_allele, unifiedgenotyper_allele |
| shiva | use_indel_realigner | Boolean | true | Realign indels | all |
| shiva | use_base_recalibration | Boolean | true | Base recalibrate | all |
| shiva | use_analyze_covariates | Boolean | true | Analyze covariates during base recalibration step | all |
| shiva | bam_to_fastq | Boolean | false | Convert bam files to fastq files | Only used when input is a bam file |
| shiva | correct_readgroups | Boolean | false | Attempt to correct read groups | Only used when input is a bam file |
| shiva | amplicon_bed | Path |  | Path to target bed file | all |
| shiva | regions_of_interest | Array of paths |  | Array of paths to region of interest (e.g. gene panels) bed files | all |
| shivavariantcalling | gender_aware_calling | Boolean | false | Enables gander aware variantcalling | haplotypecaller_gvcf |
| shivavariantcalling | hap̦loid_regions | Bed file |  | Haploid regions for all genders | haplotypecaller_gvcf |
| shivavariantcalling | hap̦loid_regions_male | Bed file |  | Haploid regions for males | haplotypecaller_gvcf |
| shivavariantcalling | hap̦loid_regions_female | Bed file |  | Haploid regions for females | haplotypecaller_gvcf |
| shiva | amplicon_bed | Path |  | Path to target bed file | all |
| vcffilter | min_sample_depth | Integer | 8 | Filter variants with at least x coverage | raw |
| vcffilter | min_alternate_depth | Integer | 2 | Filter variants with at least x depth on the alternate allele | raw |
| vcffilter | min_samples_pass | Integer | 1 | Minimum amount of samples which pass custom filter (requires additional flags) | raw |
| vcffilter | filter_ref_calls | Boolean | true | Remove reference calls | raw |

Since Shiva uses the [Mapping](../mapping.md) pipeline internally, mapping config values can be specified as well.
For all the options, please see the corresponding documentation for the mapping pipeline.

----

## Advanced usage

### Gender aware variantcalling

In Shiva and ShivaVariantcalling while using haplotypecaller_gvcf it is possible to do gender aware variant calling. 
In this mode it is required to supply bed files to define haploid regions (see config values). 
- For males, `hap̦loid_regions` and `hap̦loid_regions_male` is used.
- For females, `hap̦loid_regions` and `hap̦loid_regions_female` is used.

The pipeline will use a union of those files. At least 1 file is required while using this mode.

### Reporting modes

Shiva furthermore supports three modes. The default and recommended option is `multisample_variantcalling`.
In this mode, all bam files will be simultaneously called in one big VCF file. It will work with any number of samples.

Additionally, Shiva provides two separate modes that only work with a single sample.
Those are not recommended, but may be useful to those who need to validate replicates.

Mode `single_sample_variantcalling` calls a single sample as a merged bam file.
I.e., it will merge all libraries in one bam file, then calls on that.

The other mode, `library_variantcalling`, will call simultaneously call all library bam files.

The config for these therefore is:

| namespace | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| shiva | multisample_variantcalling | Boolean | true | Default, multisample calling |
| shiva | single_sample_variantcalling | Boolean | false | Not-recommended, single sample, merged bam |
| shiva | library_variantcalling | Boolean | false | Not-recommended, single sample, per library |


### Only variant calling

It is possible to run Shiva while only performing its variant calling steps starting from BAM files.
This has been separated in its own pipeline named `shivavariantcalling`. Different than running shiva, which converts BAM files to fastq files first, 
shivavariantcalling will not perform any pre-processing and mapping steps. Instead it will just call variants based on the input BAM files.

To view the help menu, execute:
~~~
biopet pipeline shivavariantcalling -h

Arguments for ShivaVariantcalling:
 -BAM,--inputbamsarg <inputbamsarg>    Bam files (should be deduped bams)
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or 
                                       'namespace:namespace:key=value'
 -DSC,--disablescatter                 Disable all scatters 

~~~

To run the pipeline:
~~~
biopet pipeline shivavariantcalling -config MySettings.yml -run
~~~

### Only Structural Variant calling
It is possible to run Shiva while only performing the Structural Variant calling steps starting from BAM files. For this, there is a separate pipeline named `ShivaSvCalling`. 
The difference from running Shiva, is that it will not convert the BAM files into fastq files first and it will omit any pre-processing or alignment steps. 
It will call SVs based on the input alignment (BAM) files. 

To view the help menu, type:
~~~
biopet pipeline ShivaSvCalling -h
 
Arguments for ShivaSvCalling:
 -BAM,--inputbamsarg <inputbamsarg>    Bam files (should be deduped bams)
 -sample,--sampleid <sampleid>         Sample ID
 -library,--libid <libid>              Library ID
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or 
                                       'namespace:namespace:key=value'
 -DSC,--disablescatter                 Disable all scatters
~~~ 
 
To run `ShivaSvCalling`, the user needs to supply the input BAM files from the command line using the `-BAM` flag. 
It is not possible to provide them in a sample sheet as a config file. No sample ID or library information is necessary.
  
To run the pipeline, you can type something like:
~~~
biopet pipeline ShivaSvCalling -BAM sampleA.bam -BAM sampleB.bam -config MySettings.yml -run
~~~

### Exome variant calling

If one calls variants with Shiva on exome samples and an ```amplicon_bed``` file is available, the user is able to add this file to the config file.
 When the file is given, the coverage over the positions in the bed file will be calculated plus the number of variants on each position. If there is an interest
 in a specific region of the genome/exome one is capable to give multiple ```regionOfInterest.bed``` files with the option ```regions_of_interest``` (in list/array format).
 
 A short recap: the option ```amplicon_bed``` can only be given one time and should be composed of the amplicon kit used to obtain the exome data.
 The option ```regions_of_interest``` can contain multiple bed files in ```list``` format and can contain any region a user wants. If multiple regions are given,
 the pipeline will make an coverage plot over each bed file separately. 

### VEP annotation

Shiva can be linked to our VEP based annotation pipeline to annotate the VCF files. 

**example config**
```yaml
toucan:
  vep_version: 86
  enable_scatter: false
```

### SV calling 

In addition to standard variant calling, Shiva also supports SV calling. 
One can enable this option by setting the `sv_calling` config option to `true`.

**example config**
```yaml
shiva:
    sv_calling: true
sv_callers:
- breakdancer
- delly
- clever
pysvtools:
  flanking: 100
```

### CNV calling 

In addition to standard variant calling, Shiva also supports CNV calling. 
One can enable this option by setting the `cnv_calling` config option to `true`.

For CNV calling Shiva uses the [Kopisu](../kopisu.md) as a sub-pipeline. 
Please see the documentation for Kopisu.

**example config**
```yaml
shiva:
    cnv_calling: true
kopisu:
    use_cnmops_method: false
    use_freec_method: false
    use_xhmm_method: true
amplicon_bed: <path_to_bed>
xhmm:
    discover_params: <path_to_file>
    exe: <path_to_executable>
```

## References

* Shiva follows the best practices of GATK: [GATK best practices](https://www.broadinstitute.org/gatk/guide/best-practices)


## Getting Help

If you have any questions on running Shiva, suggestions on how to improve the overall flow, or requests for your favorite variant calling related program to be added, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)
