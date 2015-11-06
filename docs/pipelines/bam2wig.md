# Bam2Wig

## Introduction

Bam2Wig is a small pipeline consisting of three steps that are used to convert BAM files into track coverage files: bigWig, wiggle, and TDF. While this seems like a task that should be tool, at the time of writing, there are no command line tools that can do such conversion in one go. Thus, the Bam2Wig pipeline was written.

## Configuration
The required configuration file for Bam2Wig is really minimal, only a single JSON file containing an `output_dir` entry:

~~~
{"output_dir": "/path/to/output/dir"}
~~~
For technical reasons, single sample pipelines, such as this mapping pipeline do **not** take a sample config.
Input files are in stead given on the command line as a flag.
Bam2wig requires a one to set the `--bamfile` command line argument to point to the to-be-converted BAM file.

## Running Bam2Wig

As with other pipelines, you can run the Bam2Wig pipeline by invoking the `pipeline` subcommand. There is also a general help available which can be invoked using the `-h` flag:

~~~bash
$ java -jar /path/to/biopet.jar pipeline bam2wig -h

Arguments for Bam2Wig:
 --bamfile <bamfile>                   Input bam file
 -config,--config_file <config_file>   JSON / YAML config file(s)
 -cv,--config_value <config_value>     Config values, value should be formatted like 'key=value' or
                                       'path:path:key=value'
 -DSC,--disablescatter                 Disable all scatters

~~~

If you are on SHARK, you can also load the `biopet` module and execute `biopet pipeline` instead:

~~~bash
$ module load biopet/v0.5.0
$ biopet pipeline bam2wig

~~~

To run the pipeline:

~~~bash
 biopet pipeline bam2wig -config </path/to/config.json> --bamfile </path/to/bam.bam> -qsub -jobParaEnv BWA -run
~~~

## Output Files

The pipeline generates three output track files: a bigWig file, a wiggle file, and a TDF file.

## Getting Help

If you have any questions on running Bam2Wig or suggestions on how to improve the overall flow, feel free to post an issue to our
 issue tracker at [GitHub](https://github.com/biopet/biopet). Or contact us directly via: [SASC email](mailto: SASC@lumc.nl)
