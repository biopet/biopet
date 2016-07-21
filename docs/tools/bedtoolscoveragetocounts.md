# BedtoolsCoverageToCounts

## Introduction
This tool enables a user to generate a count file, out of a coverage file.


## Example
To get the help menu:
~~~bash
biopet tool BedtoolsCoverageToCounts -h
Usage: BedtoolsCoverageToCounts [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --input <file>
        coverage file produced with bedtools
  -o <file> | --output <file>
        output file name
~~~

input: A coverage file produced with bedtools
output: A count file with the counts from the the values inside the coverage file. Where values could be almost everything, e.g.
genes, ensemblIDs etc. etc.

To run the tool:
~~~bash
biopet tool BedtoolsCoverageToCounts
~~~