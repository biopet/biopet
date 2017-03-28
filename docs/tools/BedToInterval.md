# BedToInterval

## Introduction
BedToInterval has been written to ensure a proper input for the tools from Picard. 
Since the latest release of Picard tools (v 1.124) there is already a tool available called: BedToIntervalList.

## Example
To get the help menu:
~~~
biopet tool BedToInterval -h
Usage: BedToInterval [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        
  -o <file> | --output <file>
        
  -b <file> | --bam <file>
~~~

To run the tool:
~~~
biopet tool BedToInterval -I myBed.bed -o myIntervals.txt -b myBam.bam
~~~

## Results
The results of this tool will be a tab delimited text file called a interval list.