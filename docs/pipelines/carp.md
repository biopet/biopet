# Carp

## Introduction

Carp is a pipeline for analyzing ChIP-seq NGS data. It uses the BWA MEM aligner and the MACS2 peak caller by default to align ChIP-seq data and call the peaks and allows you to run all your samples (control or otherwise) in one go.


## Configuration File

### Sample Configuration

The layout of the sample configuration for Carp is basically the same as with our other multi sample pipelines, for example:

~~~
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
        }
        "lib_two": {
          "R1": "/absolute/path/to/first/read/pair.fq",
          "R2": "/absolute/path/to/second/read/pair.fq"
        }
      }
    }
  }
}
~~~

What's important here is that you can specify the control ChIP-seq experiment(s) for a given sample. These controls are usually 
ChIP-seq runs from input DNA and/or from treatment with nonspecific binding proteins such as IgG. In the example above, we are specifying `sample_Y` as the control for `sample_X`.

### Pipeline Settings Configuration

For the pipeline settings, there are some values that you need to specify while some are optional. Required settings are:

1. `output_dir`: path to output directory (if it does not exist, Carp will create it for you).
2. `reference`: this must point to a reference FASTA file and in the same directory, there must be a `.dict` file of the FASTA file.

While optional settings are:

1. `aligner`: which aligner to use (`bwa` or `bowtie`)
2. `macs2`: Here only the callpeak modus is implemented. But one can set all the options from [macs2 callpeak](https://github
.com/taoliu/MACS/#call-peaks)
## Running Carp

As with other pipelines in the Biopet suite, Carp can be run by specifying the pipeline after the `pipeline` subcommand:

~~~
java -jar </path/to/biopet.jar> pipeline carp -config </path/to/config.json> -qsub -jobParaEnv BWA -run
~~~

If you already have the `biopet` environment module loaded, you can also simply call `biopet`:

~~~
biopet pipeline carp -config </path/to/config.json> -qsub -jobParaEnv BWA -run
~~~

It is also a good idea to specify retries (we recomend `-retry 3` up to `-retry 5`) so that cluster glitches do not interfere with your pipeline runs.

## Getting Help

If you have any questions on running Carp, suggestions on how to improve the overall flow, or requests for your favorite ChIP-seq related program to be added, feel free to post an issue to our issue tracker at [https://git.lumc.nl/biopet/biopet/issues](https://git.lumc.nl/biopet/biopet/issues).

