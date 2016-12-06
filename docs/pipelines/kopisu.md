# Kopisu 

## Introduction
Kopisu is our pipeline for detecting CNVs for DNA-seq.
It can work with `bam` files. 

Kopisu is usually used as a module inside Shiva, but can also be used stand-alone.
 
## Tools for this pipeline

* [FreeC](http://boevalab.com/FREEC/)
* [cn.mops](http://bioconductor.org/packages/release/bioc/html/cn.mops.html)
* [XHMM](http://atgu.mgh.harvard.edu/xhmm/tutorial.shtml)

## Usage

```
biopet pipeline kopisu --help

Arguments for Kopisu:
 -BAM,--inputbamsarg <inputbamsarg>    Bam files (should be deduped bams)
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or 
                                       'namespace:namespace:key=value'
 -DSC,--disablescatter                 Disable all scatters
```

## Methods

For CNV calling, Kopisu supports three methods, each with different use cases: 
 
 * FreeC: Use for WGS or Exomes. For WGS, a control sample set is not required
 * Cn.mops: Use for Exomes. Must be used on a set of samples, preferably with N>20. Do not use on low-coverage samples.
 * XHMM: Use for Exomes or targeted approach. Number of target regions _must_ be larger than the amount of samples. Use with N >= 40.
 
All three methods can be run concurrently. However, we do not yet provide any merging of calls, since output formats vary widely.

### Freec 

TODO

### Cn.mops

TODO


### XHMM 

When using the XHMM method, one _must_ provide a target bed file. 
XHMM cannot work without it. 
Additionally, the XHMM method requires the path to a parameters file for XHMM. 
Please see the XHMM website for what this file should contain.
 
This means the following config values are required:

| Namespace | Name | Type | Default | Meaning |
| --------- | ---- | ---- | ------- | ------- |
| - | amplicon_bed | Path | - | Path to target bed file |
| xhmm | discover_params | Path | - | Path to XHMM params file |

It is recommended you use at least 40 samples with this method. 
One should also have more _covered_ target regions than there are samples.
This means this method is not suited for very small target kits. 

Please note that it is _not_ possible to run this method without GATK dependencies. 

Additional optional config values:

| Namespace | Name | Type | Default | Meaning | 
| --------- | ---- | ---- | ------- | ------- |
| xhmm_normalize | normalize_method | String | PVE_mean | The normalization method to use |
| xhmm_normalize | pve_mean_factor | Float | 0.7 |  Factor for PVE mean normalization |


**XHMM example**

```yaml
kopisu:
    use_cnmops_method: false
    use_freec_method: false
    use_xhmm_method: true
amplicon_bed: <path_to_bed>
xhmm:
    discover_params: <path_to_file>
    exe: <path_to_executable>
```

## Getting Help

If you have any questions on running Kopisu, suggestions on how to improve the overall flow, or requests for your favorite CNV related program to be added, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)