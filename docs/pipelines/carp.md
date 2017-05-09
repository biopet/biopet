# Carp

## Introduction

Carp is a pipeline for analyzing ChIP-seq NGS data. It uses the BWA MEM aligner and the MACS2 peak caller by default to align ChIP-seq data and call the peaks and allows you to run all your samples (control or otherwise) in one go.

### Sample input extensions

Please refer [to our mapping pipeline](mapping.md) for information about how the input samples should be handled. 

## Configuration File

### Sample Configuration

The layout of the sample configuration for Carp is basically the same as with our other multi sample pipelines it may be either ```json``` or ```yaml``` formatted.
Below we show two examples for ```json``` and ```yaml```. One should appreciate that multiple libraries can be used if a sample is sequenced on multiple lanes. This is noted with library id in the config file.


~~~ json
{
  "samples": {
    "sample_X": {
      "control": ["sample_Y"],
      "libraries": {
        "lib_one": {
          "R1": "/absolute/path/to/first/read/pair.fq",
          "R2": "/absolute/path/to/second/read/pair.fq"
        }
      }
    },
    "sample_Y": {
      "libraries": {
        "lib_one": {
          "R1": "/absolute/path/to/first/read/pair.fq",
          "R2": "/absolute/path/to/second/read/pair.fq"
        },
        "lib_two": {
          "R1": "/absolute/path/to/first/read/pair.fq",
          "R2": "/absolute/path/to/second/read/pair.fq"
        }
      }
    }
  }
}
~~~

~~~ yaml
samples:
  sample_X
    control:
      - sample_Y
    libraries:
      lib_one:
        R1: /absolute/path/to/first/read/pair.fq
        R2: /absolute/path/to/second/read/pair.fq
  sample_Y:
    libraries:
      lib_one:
        R1: /absolute/path/to/first/read/pair.fq
        R2: /absolute/path/to/second/read/pair.fq
      lib_two:
        R1: /absolute/path/to/first/read/pair.fq
        R2: /absolute/path/to/second/read/pair.fq
~~~

What's important here is that you can specify the control ChIP-seq experiment(s) for a given sample. These controls are usually 
ChIP-seq runs from input DNA and/or from treatment with nonspecific binding proteins such as IgG. In the example above, we are specifying `sample_Y` as the control for `sample_X`.
**Please notice** that the control is given in the form of a ```list```. This is because sometimes one wants to use multiple control samples, this can be achieved to pass the sampleNames of the control samples in a list to the field **control** in the config file.
In ```json``` this will become: 

~~~ json
{
  "samples": {
    "sample_X": {
      "control": ["sample_Y","sample_Z"]
      }
    }
 }
~~~

In ```yaml``` this is a bit different and will look like this:

~~~ yaml
samples:
    sample_X:
      control:
        - sample_Y
        - sample_Z
~~~


### Pipeline Settings Configuration

For the pipeline settings, there are some values that you need to specify while some are optional. Required settings are:

1. `output_dir`: path to output directory (if it does not exist, Carp will create it for you).
2. `reference`: this must point to a reference FASTA file and in the same directory, there must be a `.dict` file of the FASTA file.

While optional settings are:

1. `aligner`: which aligner to use (`bwa` or `bowtie`)
2. `macs2`: Here only the callpeak modus is implemented. But one can set all the options from [macs2 callpeak](https://github.com/taoliu/MACS/#call-peaks) in this settings config. Note that the config value is: `macs2_callpeak`

[Gears](gears) is run automatically for the data analysed with `Carp`. There are two levels on which this can be done and this should be specified in the [config](../general/config) file:

*`mapping_to_gears: unmapped` : Unmapped reads after alignment. (default)
*`mapping_to_gears: all` : Trimmed and clipped reads from [Flexiprep](flexiprep).
*`mapping_to_gears: none` : Disable this functionality.

## Configuration for detection of broad peaks (ATAC-seq)

Carp can do broad peak-calling by using the following config: 

``` json
  "bowtie2": {
    "maxins": 2000,
    "m": 1
  },
  
  "macs2callpeak":{
    "gsize": 1.87e9,
    "bdg": true,
    "nomodel": true,
    "broad": true,
    "extsize": 200,
    "shift": 100,
    "qvalue": 0.001
  }
```

These settings are optimized to call peaks on samples prepared using the ATAC protocol.

## Taxonomy extraction 

It is possible to only align reads matching a certain taxonomy.  
This is useful in situations where known contaminants exist in the sequencing files.

By default this option is **disabled**. 
Due to technical reasons, we **cannot** recover reads that do not match to any known taxonomy.

Taxonomies are determined using [Gears](gears.md) as a sub-pipeline. 

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


## Running Carp

As with other pipelines in the Biopet suite, Carp can be run by specifying the pipeline after the `pipeline` subcommand:

~~~ bash
biopet pipeline carp -config </path/to/config.json> -qsub -jobParaEnv BWA -run
~~~

If you already have the `biopet` environment module loaded, you can also simply call `biopet`:

~~~ bash
biopet pipeline carp -config </path/to/config.json> -qsub -jobParaEnv BWA -run
~~~

It is also a good idea to specify retries (we recommend `-retry 4` up to `-retry 8`) so that cluster glitches do not interfere 
with your pipeline runs.

## Example output

```bash
.
├── Carp.summary.json
├── report
│   ├── alignmentSummary.png
│   ├── alignmentSummary.tsv
│   ├── ext
│   │   ├── css
│   │   │   ├── bootstrap_dashboard.css
│   │   │   ├── bootstrap.min.css
│   │   │   ├── bootstrap-theme.min.css
│   │   │   └── sortable-theme-bootstrap.css
│   │   ├── fonts
│   │   │   ├── glyphicons-halflings-regular.ttf
│   │   │   ├── glyphicons-halflings-regular.woff
│   │   │   └── glyphicons-halflings-regular.woff2
│   │   └── js
│   │       ├── bootstrap.min.js
│   │       ├── jquery.min.js
│   │       └── sortable.min.js
│   ├── Files
│   │   └── index.html
│   ├── index.html
│   ├── insertsize.png
│   ├── insertsize.tsv
│   ├── QC_Bases_R1.png
│   ├── QC_Bases_R1.tsv
│   ├── QC_Bases_R2.png
│   ├── QC_Bases_R2.tsv
│   ├── QC_Reads_R1.png
│   ├── QC_Reads_R1.tsv
│   ├── QC_Reads_R2.png
│   ├── QC_Reads_R2.tsv
│   ├── Samples
│   │   ├── 10_Input_2
│   │   │   ├── Alignment
│   │   │   │   ├── index.html
│   │   │   │   ├── insertsize.png
│   │   │   │   ├── insertsize.tsv
│   │   │   │   ├── wgs.png
│   │   │   │   └── wgs.tsv
│   │   │   ├── Files
│   │   │   │   └── index.html
│   │   │   ├── index.html
│   │   │   └── Libraries
│   │   │       ├── 3307
│   │   │       │   ├── Alignment
│   │   │       │   │   ├── index.html
│   │   │       │   │   ├── insertsize.png
│   │   │       │   │   ├── insertsize.tsv
│   │   │       │   │   ├── wgs.png
│   │   │       │   │   └── wgs.tsv
│   │   │       │   ├── index.html
│   │   │       │   └── QC
│   │   │       │       ├── fastqc_R1_duplication_levels.png
│   │   │       │       ├── fastqc_R1_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_per_base_quality.png
│   │   │       │       ├── fastqc_R1_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_duplication_levels.png
│   │   │       │       ├── fastqc_R1_qc_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_qc_per_base_quality.png
│   │   │       │       ├── fastqc_R1_qc_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_sequence_length_distribution.png
│   │   │       │       ├── fastqc_R1_sequence_length_distribution.png
│   │   │       │       └── index.html
│   │   │       └── index.html
│   │   ├── 11_GR_2A
│   │   │   ├── Alignment
│   │   │   │   ├── index.html
│   │   │   │   ├── insertsize.png
│   │   │   │   ├── insertsize.tsv
│   │   │   │   ├── wgs.png
│   │   │   │   └── wgs.tsv
│   │   │   ├── alignmentSummary.png
│   │   │   ├── alignmentSummary.tsv
│   │   │   ├── Files
│   │   │   │   └── index.html
│   │   │   ├── index.html
│   │   │   └── Libraries
│   │   │       ├── 3307
│   │   │       │   ├── Alignment
│   │   │       │   │   ├── index.html
│   │   │       │   │   ├── insertsize.png
│   │   │       │   │   ├── insertsize.tsv
│   │   │       │   │   ├── wgs.png
│   │   │       │   │   └── wgs.tsv
│   │   │       │   ├── index.html
│   │   │       │   └── QC
│   │   │       │       ├── fastqc_R1_duplication_levels.png
│   │   │       │       ├── fastqc_R1_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_per_base_quality.png
│   │   │       │       ├── fastqc_R1_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_duplication_levels.png
│   │   │       │       ├── fastqc_R1_qc_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_qc_per_base_quality.png
│   │   │       │       ├── fastqc_R1_qc_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_sequence_length_distribution.png
│   │   │       │       ├── fastqc_R1_sequence_length_distribution.png
│   │   │       │       └── index.html
│   │   │       ├── 3385
│   │   │       │   ├── Alignment
│   │   │       │   │   ├── index.html
│   │   │       │   │   ├── insertsize.png
│   │   │       │   │   ├── insertsize.tsv
│   │   │       │   │   ├── wgs.png
│   │   │       │   │   └── wgs.tsv
│   │   │       │   ├── index.html
│   │   │       │   └── QC
│   │   │       │       ├── fastqc_R1_duplication_levels.png
│   │   │       │       ├── fastqc_R1_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_per_base_quality.png
│   │   │       │       ├── fastqc_R1_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_duplication_levels.png
│   │   │       │       ├── fastqc_R1_qc_kmer_profiles.png
│   │   │       │       ├── fastqc_R1_qc_per_base_quality.png
│   │   │       │       ├── fastqc_R1_qc_per_base_sequence_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_gc_content.png
│   │   │       │       ├── fastqc_R1_qc_per_sequence_quality.png
│   │   │       │       ├── fastqc_R1_qc_sequence_length_distribution.png
│   │   │       │       ├── fastqc_R1_sequence_length_distribution.png
│   │   │       │       └── index.html
│   │   │       └── index.html
```

## Getting Help

If you have any questions on running Carp, suggestions on how to improve the overall flow, or requests for your favorite ChIP-seq related program to be added, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)

