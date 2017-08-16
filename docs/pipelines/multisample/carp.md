# Carp

## Introduction

Carp is a pipeline for analyzing ChIP-seq NGS data. It uses the `bwa mem` aligner and the [MACS2](https://github.com/taoliu/MACS/wiki) peak caller 
by default to align ChIP-seq data and call the peaks and allows you to run all your samples (control or otherwise) in one go.

### Sample input extensions

Please refer to our [config documentation page](../../general/config.md) for information about how the input samples should be handled. 

## Configuration File

### Sample Configuration

The layout of the sample configuration for Carp is basically the same as with our other multisample pipelines. 
It may be either `json` or `yaml` formatted.
Below we show two examples for `json` and `yaml`. One should appreciate that multiple libraries can be used if a sample is sequenced on multiple lanes. 
This is noted with library id in the config file.


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
ChIP-seq runs from input DNA and/or from treatment with nonspecific binding proteins such as IgG. 
In the example above, we are specifying `sample_Y` as the control for `sample_X`.
**Please notice** that the control is given in the form of a ```list```. This is because sometimes one wants to use multiple control samples, 
this can be achieved to pass the sampleNames of the control samples in a list to the field **control** in the config file.

In `json` this will become: 

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

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| - | output_dir | String | - | Path to output directory (if it does not exist, Gentrap will create it for you) |
| mapping | reference_fasta | String | This must point to a reference `FASTA` file and in the same directory, there must be a `.dict` file of the FASTA file.| 

While optional settings are:

| ConfigNamespace | Name | Type | Default | Function |
| --------- | ---- | ---- | ------- | -------- |
| mapping | aligner | String | bwa-mem | Aligner of choice. Options: `bowtie` |

Here only the `callpeak` function of macs2 is implemented. 
In order to pass parameters specific to macs2 callpeak the `macs2callpeak` namespace should be used. 
For example, including the following in your config file, will set the effective genome size:
 
 ```yaml
 macs2callpeak:
   gsize: 2.7e9
 ```

A comprehensive list of all available options for `masc2 callpeak` can be found [here](https://github.com/taoliu/MACS/#call-peaks).  

## Configuration for detection of broad peaks (ATAC-seq)

Carp can do broad peak-calling by using the following config: 

```yaml
mapping:
  bowtie2: 
    maxins: 2000
    m: 1
carp:
  macs2callpeak:
    gsize: 1.87e9 #This is specific to the mouse genome
    bdg: true
    nomodel: true
    broad: true
    extsize: 200
    shift: 100
    qvalue: 0.001
```

These settings are optimized to call peaks on samples prepared using the ATAC protocol.


## Running Carp

As with other pipelines in the Biopet suite, Carp can be run by specifying the pipeline after the `pipeline` sub-command:

~~~ bash
java -jar </path/to/biopet.jar> pipeline carp \
-config </path/to/config.yml> \
-config </path/to/samples.yml>
~~~

You can also use the `biopet` environment module (recommended) when you are running the pipeline in SHARK:

~~~ bash
$ module load biopet/v0.9.0
$ biopet pipeline carp -config </path/to/config.yml> \
-qsub -jobParaEnv BWA -run
~~~

## Example output

```
.
├── carp.summary.db
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

