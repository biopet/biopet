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

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 

Shiva is a special pipeline in the sense that it can also start directly from `bam` files. Note that one should alter the sample config field from `R1` into `bam`.

### Full pipeline

The full pipeline can start from fastq or from bam file. This pipeline will include pre-process steps for the bam files.

To view the help menu, execute:
~~~
biopet pipeline shiva -h

Arguments for Shiva:
 -sample,--onlysample <onlysample>               Only Sample
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters

~~~

To run the pipeline:
~~~
biopet pipeline shiva -config MySamples.json -config MySettings.json -run
~~~

A dry run can be performed by simply removing the `-run` flag from the command line call.

[Gears](gears) is run automatically for the data analysed with `Shiva`. There are two levels on which this can be done and this should be specified in the [config](../general/config) file:

*`mapping_to_gears: unmapped` : Unmapped reads after alignment. (default)
*`mapping_to_gears: all` : Trimmed and clipped reads from [Flexiprep](flexiprep).
*`mapping_to_gears: none` : Disable this functionality.

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
biopet pipeline shivavariantcalling -config MySettings.json -run
~~~

A dry run can be performed by simply removing the `-run` flag from the command line call.


----

## Variant caller
At this moment the following variant callers can be used

* <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller</a>
    * Running default HaplotypeCaller
* <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller_gvcf</a>
    * Running HaplotypeCaller in gvcf mode
* <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php">haplotypecaller_allele</a>
    * Only genotype a given list of alleles with HaplotypeCaller
* <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php">unifiedgenotyper</a>
    * Running default UnifiedGenotyper
* <a href="https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php">unifiedgenotyper_allele</a>
    * Only genotype a given list of alleles with UnifiedGenotyper
* <a href="https://samtools.github.io/bcftools/bcftools.html">bcftools</a>
* <a href="https://samtools.github.io/bcftools/bcftools.html">bcftools_singlesample</a>
* <a href="https://github.com/ekg/freebayes">freebayes</a>
* [raw](../tools/MpileupToVcf)

## Config options

To view all possible config options please navigate to our Gitlab wiki page
<a href="https://git.lumc.nl/biopet/biopet/wikis/GATK-Variantcalling-Pipeline" target="_blank">Config</a>

### Required settings
| Confignamespace | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| - | output_dir | String |  | Path to output directory |
| Shiva | variantcallers | List[String] | | Which variant callers to use |


### Config options

| ConfignNamespace | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| shiva | species | String | unknown_species | Name of species, like H.sapiens |
| shiva | reference_name | String | unknown_reference_name | Name of reference, like hg19 |
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

Since Shiva uses the [Mapping](mapping.md) pipeline internally, mapping config values can be specified as well.
For all the options, please see the corresponding documentation for the mapping pipeline.

### Exome variant calling

If one calls variants with Shiva on exome samples and a ```amplicon_bed``` file is available, the user is able to add this file to the config file.
 When the file is given, the coverage over the positions in the bed file will be calculated plus the number of variants on each position. If there is an interest
 in a specific region of the genome/exome one is capable to give multiple ```regionOfInterest.bed``` files with the option ```regions_of_interest``` (in list/array format).
 
 A short recap: the option ```amplicon_bed``` can only be given one time and should be composed of the amplicon kit used to obtain the exome data.
 The option ```regions_of_interest``` can contain multiple bed files in ```list``` format and can contain any region a user wants. If multiple regions are given,
 the pipeline will make an coverage plot over each bed file separately. 
 

### Modes

Shiva furthermore supports three modes. The default and recommended option is `multisample_variantcalling`.
During this mode, all bam files will be simultaneously called in one big VCF file. It will work with any number of samples.

On top of that, Shiva provides two separate modes that only work with a single sample.
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

## CNV calling 

In addition to standard variant calling, Shiva also supports CNV calling. 
One can enable this option by setting the `cnv_calling` config option to `true`.

For CNV calling Shiva uses the [Kopisu](kopisu.md) as a module. 
Please see the documentation for Kopisu.  


## Example configs 
**Config example**

``` yaml
samples:
    SampleID:
        libraries:
            lib_id_1:
                bam: YourBam.bam
            lib_id_2:
                R1: file_R1.fq.gz
                R2: file_R2.fq.gz
dbsnp: <dbsnp.vcf.gz>
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

**Additional XHMM CNV calling example**

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
