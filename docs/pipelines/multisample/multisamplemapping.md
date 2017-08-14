# Introduction

The MultiSampleMapping pipeline was created for handling data from multiple samples at the same time. It extends the functionality of the mapping
pipeline, which is meant to take only single sample data as input. As most experimental setups require data generation from many different samples and 
the alignment of the data to a reference of choice is a very common task for further downstream analyses, 
this pipeline serves also as a first step for the following analysis pipelines bundled within BIOPET:

 *  [Basty](basty.md) - Bacterial typing
 *  [Carp](carp.md) - ChIP-seq analysis
 *  [Gentrap](gentrap.md) - Generic transcriptome analysis pipeline
 *  [Shiva](shiva.md) - Variant calling
 *  [Tinycap](tinycap.md) - smallRNA analysis

# MultisampleMapping

Its aim is to align the input data to the reference of interest with the most commonly used aligners 
(for a complete list of supported aligners see [here](../mapping.md)).  

## Setting up

### Reference files

An important step prior to the analysis is the proper generation of all the required index files for the reference, apart from the 
reference sequence file itself.

The index files are created from the supplied reference:

* ```.dict``` (can be produced with <a href="http://broadinstitute.github.io/picard/" target="_blank">Picard tool suite</a>)
* ```.fai``` (can be produced with <a href="http://samtools.sourceforge.net/samtools.shtml" target="_blank">Samtools faidx</a>) 
* ```.idxSpecificForAligner``` (depending on which aligner is used one should create a suitable index specific for that aligner. 
Each aligner has its own way of creating index files. Therefore the options for creating the index files can be found inside the aligner itself)

### Configuration files

MultiSampleMapping relies on __YML__ (or __JSON__) configuration files to run its analyses. There are two important parts here, the configuration for the samples
(to determine the sample layout of the experiment) and the configuration of the pipeline settings (to determine the different parameters for the
pipeline components).

#### Sample config

For a detailed explanation of how the samples configuration file should be created please see [here](../../general/config.md).
As an example for two samples, one with two libraries and one with a single library, a samples config would look like this:

```YAML
samples:
  sample1:
    libraries:
      lib01:
        R1: /full/path/to/R1.fastq.gz
        R2: /full/path/to/R2.fastq.gz
      lib02:
        R1: /full/path/to/R1.fastq.gz
        R2: /full/path/to/R2.fastq.gz
  sample2:
     libraries:
       lib01:
         R1: /full/path/to/R1.fastq.gz
         R2: /full/path/to/R2.fastq.gz
```

#### Settings config

As this is an extension of the mapping pipeline a comprehensive list for all the settings affecting the analysis can be found [here](../mapping.md###Config).
Required settings that should be included in this config file are:

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| - | output_dir | String | | Path to output directory |
| mapping | reference_fasta | String | | This must point to a reference FASTA file and in the same directory, there must be a `.dict` file of the FASTA file. If the `.dict` file does not exist, you can create it using: ```` java -jar <picard jar> CreateSequenceDictionary R=<reference.fasta> O=<outputDict> ```` |

Optional settings

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| mapping | merge_strategy | String | preprocessmarkduplicates | Determines how the individual bam files from each library are merged into one bam file per sample. Available options: `preprocessmarkduplicates` (add read group information, mark duplicates and then merge), `mergesam` (simple samtools merge), `preprocessmergesam` (add read group information and merge with samtools), `markduplicates` (mark duplicates first and then merge), `preprocesssambambamarkdup` (add read group information, mark duplicates with sambamba and then merge), `none` (do not merge the bam files) |
| mapping | duplicates_method | String | picard | Determines the method to use for marking duplicates. Available options: `picard`, `sambamba` or `none` to disable duplicate marking |
| mapping | skip_flexiprep | Boolean| false | Determines whether the input is analysed with [Flexiprep](../flexiprep.md). |
| mapping | mapping_to_gears | String | none | Determines whether the input is analysed with [Gears](../gears.md) or not. Available options: `all` (all reads), `unmapped` (extract only the unmapped reads and analyse with Gears) and `none` (skip this step) |

An example config.yml

```yaml
output_dir: /path/to/output/dir
reference_fasta: /path/to/reference
mapping_to_gears: unmapped
bwamem:
  t: 4
duplicates_method: sambamba
 ```

## Running Gears

By default [Gears](../gears.md) is run automatically for the data analysed with MultiSampleMapping. 
There are two levels on which this can be done and this should be specified in the [config](../../general/config.md) file:
           
* `mapping_to_gears: all` : Trimmed and clipped reads from [Flexiprep](../flexiprep.md) (default)
* `mapping_to_gears: unmapped` : Only send unmapped reads after alignment to Gears, e.g., a kind of "trash bin" analysis.
* `mapping_to_gears: none` : Disable this functionality.
           
## Running multisamplemapping

To run the pipeline (it is recommended to first do a dry run, removing the `-run` option)

```bash
biopet pipeline multisamplemapping -run \
-config /path/to/samples.yml \
-config /path/to/config.yml
```

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
