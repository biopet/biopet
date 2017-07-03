# Gentrap

## Introduction

Gentrap (*generic transcriptome analysis pipeline*) is a general data analysis pipelines for quantifying expression levels from RNA-seq libraries generated using the Illumina machines. It was designed to be flexible, providing several aligners and quantification modes to choose from, with optional steps in between. It can be used to run different experiment configurations, from single sample runs to multiple sample runs containing multiple sequencing libraries. It can also do a very simple variant calling (using VarScan).

At the moment, Gentrap supports the following aligners:

1. [GSNAP](http://research-pub.gene.com/gmap/)
2. [TopHat](http://ccb.jhu.edu/software/tophat/index.shtml)
3. [Star](https://github.com/alexdobin/STAR/releases)
4. [Hisat2](https://ccb.jhu.edu/software/hisat2/index.shtml)

and the following quantification modes:

1. Fragment counts per gene (using HTSeq-count)
2. Base counts per gene
3. Base counts per exon
4. Cufflinks-style quantification, with a strict reference, with a reference as a guide, and/or without any references

You can also provide a `.refFlat` file containing ribosomal sequence coordinates to measure how many of your libraries originate from ribosomal sequences. Then, you may optionally remove those regions as well.

## Sample input extensions

Please refer [to our mapping pipeline](multisamplemapping.md) for information about how the input samples should be handled. 

## Configuration File
As with other biopet pipelines, Gentrap relies on a JSON configuration file to run its analyses. There are two important parts here, the configuration for the samples (to determine the sample layout of your experiment) and the configuration for the pipeline settings (to determine which analyses are run).
To get help creating the appropriate [configs](../../general/config.md) please refer to the config page in the general section.

## Running Gears
[Gears](../gears.md) is run automatically for the data analysed with Gentrap.
To fine-tune this functionality see [here](multisamplemapping.md#Running-Gears).

## Taxonomy extraction 

It is possible to only align reads matching a certain taxonomy.  
This is useful in situations where known contaminants exist in the sequencing files.

By default this option is **disabled**. 
Due to technical reasons, we **cannot** recover reads that do not match to any known taxonomy.

Taxonomies are determined using [Gears](../gears.md) as a sub-pipeline. 

To enable taxonomy extraction, specify the following additional flags in your
config file:

| Name | Namespace | Type | Function |
| ---- | --------- | ---- | -------- |
| mapping_to_gears | mapping | Boolean | Must be set to **true** |
| taxonomy_extract | mapping | Boolean (must be **true** for this purpose) | enable taxonomy extraction |
| taxonomy | taxextract | string | The name of the taxonomy you wish to extract | 

The extraction can be fine-tuned with two additional optional config values:
 
 | Name | Namespace | Type | Function |
 | ---- | --------- | ---- | -------- |
 | reverse | taxextract | Boolean | Set to true to select those reads _not_ matching the taxonomy. |
 | no_children | taxextract | Boolean | Set to true to put an exact match on the taxonomy, rather than the specific node and its children |
 
 
### Example config
  
```yaml
extract_taxonomies: true
mapping_to_gears: all
taxextract:
  exe: /path/to/taxextract
  taxonomy: H.sapiens
```
  


### Sample Configuration

Samples are single experimental units whose expression you want to measure. They usually consist of a single sequencing library, but in some cases (for example when the experiment demands each sample have a minimum library depth) a single sample may contain multiple sequencing libraries as well. All this is can be configured using the correct JSON nesting, with the following pattern:

~~~ yaml
---
  samples: 
    sample_A: 
      libraries: 
        lib_01: 
          R1: "/absolute/path/to/first/read/pair.fq"
          R2: "/absolute/path/to/second/read/pair.fq"
~~~

In the example above, there is one sample (named `sample_A`) which contains one sequencing library (named `lib_01`). The library itself is paired end, with both `R1` and `R2` pointing to the location of the files in the file system. A more complicated example is the following:

~~~ yaml
---
  samples: 
    sample_X: 
      libraries: 
        lib_one: 
          R1: "/absolute/path/to/first/read/pair.fq"
          R2: "/absolute/path/to/second/read/pair.fq"
    sample_Y: 
      libraries: 
        lib_one: 
          R1: "/absolute/path/to/first/read/pair.fq"
          R2: "/absolute/path/to/second/read/pair.fq"
        lib_two: 
          R1: "/absolute/path/to/first/read/pair.fq"
          R2: "/absolute/path/to/second/read/pair.fq"
~~~

In this case, we have two samples (`sample_X` and `sample_Y`) and `sample_Y` has two different libraries (`lib_one` and `lib_two`). Notice that the names of the samples and libraries may change, but several keys such as `samples`, `libraries`, `R1`, and `R2` remain the same.


### Pipeline Settings Configuration

For the pipeline settings, there are some values that you need to specify while some are optional. Required settings are:

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| - | output_dir | String | - | Path to output directory (if it does not exist, Gentrap will create it for you) |
| mapping | aligner | String | - | Aligner of choice. (`gsnap`, `tophat`, `hisat2`, `star`, `star-2pass`) `star-2pass` enables the 2-pass mapping option of STAR, for the most sensitive novel junction discovery. For more, please refer to [STAR user Manual](https://github.com/alexdobin/STAR/blob/master/doc/STARmanual.pdf) |
| mapping | reference_fasta | String | | this must point to a reference FASTA file and in the same directory, there must be a `.dict` file of the FASTA file. If the `.dict` file does not exist, you can create it using: ```` java -jar <picard jar> CreateSequenceDictionary R=<reference.fasta> O=<outputDict> ```` |
| gentrap | expression_measures | String | |this entry determines which expression measurement modes Gentrap will do. You can choose zero or more from the following: `fragments_per_gene`, `base_counts`, `cufflinks_strict`, `cufflinks_guided` and/or `cufflinks_blind`. If you only wish to align, you can set the value as an empty list (`[]`). |
| gentrap | strand_protocol | String | |this determines whether your library is prepared with a specific stranded protocol or not. There are two protocols currently supported: `dutp` for dUTP-based protocols and `non_specific` for non-strand-specific protocols. |
| gentrap | annotation_reffat | String | | contains the path to an annotation refFlat file of the entire genome |


While optional settings are:

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| gentrap | annotation_gtf | String | | contains path to an annotation GTF file, only required when `expression_measures` contain `fragments_per_gene`, `cufflinks_strict`, and/or `cufflinks_guided` |
| gentrap | annotation_bed | String | | contains path to a flattened BED file (no overlaps), only required when `expression_measures` contain `base_counts` |
| gentrap | remove_ribosomal_reads | Boolean | False |  contains path to a flattened BED file (no overlaps), only required when `expression_measures` contain `base_counts` |
| gentrap | ribosomal_refflat | String | | contains path to a refFlat file of ribosomal gene coordinates, required when `remove_ribosomal_reads` is `true` |
| gentrap | call_variants | Boolean | False |  whether to call variants on the RNA-seq data or not |
 

Thus, an example settings configuration is as follows:
~~~ yaml
---
  output_dir: "/path/to/output/dir"
  expression_measures: 
    - "fragments_per_gene"
    - "bases_per_gene"
  strand_protocol: "dutp"
  reference_fasta: "/path/to/reference/fastafile"
  annotation_gtf: "/path/to/gtf"
  annotation_refflat: "/path/to/refflat"
~~~

#### Best practice example
If you are unsure of how to use the numerous options of gentrap, please refer to the following best practice configuration file example. 
~~~ yaml
---
  output_dir: "/path/to/output/dir"
  aligner: "gsnap"
  reference_fasta: "/path/to/reference/fastafile"
  expression_measures: 
    - "fragments_per_gene"
  strand_protocol: "dutp"
  annotation_refflat: "/path/to/refflat"
~~~

#### Example configurations

In most cases, it's practical to combine the samples and settings configuration into one file. 
Here is an [example config file](/examples/gentrap_example.json) where both samples and settings are stored into one file. 
Note also that there are additional tool configurations in the config file.

## Running Gentrap

As with other pipelines in the Biopet suite, Gentrap can be run by specifying the pipeline after the `pipeline` sub-command:

~~~ bash
java -jar </path/to/biopet.jar> pipeline gentrap \
-config </path/to/config.yml> -run
~~~

You can also use the `biopet` environment module (recommended) when you are running the pipeline in SHARK:

~~~ bash
$ module load biopet/v0.9.0
$ biopet pipeline gentrap \
-config </path/to/config.yml> \
-qsub -jobParaEnv BWA -run
~~~

It is also a good idea to specify retries (we recommend `-retry 3` up to `-retry 5`) so that cluster glitches do not interfere with your pipeline runs.

## Output Files

The numbers and types of output files depend on your run configuration. 
What you can always expect, however, is that there will be a `sqlite` file of your run called `gentrap.summary.db` and an HTML report in a `report` folder 
called `index.html`. 
The summary file contains files and statistics specific to the current run, which is meant for cases when you wish to do further 
processing with your Gentrap run (for example, plotting some figures), while the html report provides a quick overview of your run results.

## Getting Help

If you have any questions on running Gentrap, suggestions on how to improve the overall flow, or requests for your favorite RNA-seq related program to be added,
 feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet). Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)