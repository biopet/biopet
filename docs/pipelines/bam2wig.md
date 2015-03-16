# Bam2Wig

## Introduction

Bam2Wig is a small pipeline consisting of three steps that is used to convert BAM files into track coverage files: bigWig, wiggle, and TDF. While this seems like a task that should be tool, at the time of writing, there are no command line tools that can do such conversion in one go. Thus, the Bam2Wig pipeline was written.

## Configuration

The required configuration file for Bam2Wig is really minimal, only a single JSON file containing an `output_dir` entry:

~~~
{"output_dir": "/path/to/output/dir"}
~~~

## Running Bam2Wig

As with other pipelines, you can run the Bam2Wig pipeline by invoking the `pipeline` subcommand. There is also a general help available which can be invoked using the `-h` flag:

~~~
$ java -jar /path/to/biopet.jar pipeline sage -h
~~~

If you are on SHARK, you can also load the `biopet` module and execute `biopet pipeline` instead:

~~~
$ module load biopet/v0.3.0
$ biopet pipeline bam2wig

~~~

To run the pipeline:
~~~
 biopet pipeline bam2wig -config </path/to/config.json> -qsub -jobParaEnv BWA -run
~~~

## Output Files

The pipeline generates three output track files: a bigWig file, a wiggle file, and a TDF file.
