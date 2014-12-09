# FastqSplitter

## Introduction
This tool splits a fastq files based on the number of output files specified. So if one specifies 5 output files it will split the fastq
into 5 files. This can be very usefull if one wants to use chunking option in one of our pipelines, we can generate the exact amount of fastqs
needed for the number of chunks specified. Note that this will be automatically done inside the pipelines.  


## Example
To get the help menu:
~~~
java -jar Biopet-0.2.0.jar tool FastqSplitter -h
Usage: FastqSplitter [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        out is a required file property
  -o <file> | --output <file>
        out is a required file property
~~~
To run the tool:
~~~
java -jar Biopet-0.2.0.jar tool FastqSplitter --inputFile myFastq.fastq \
--output mySplittedFastq_1.fastq --output mySplittedFastq_2.fastq \
--output mySplittedFastq_3.fastq
~~~
The above invocation will split the input in 3 equally divided fastq files.

## Output
Multiple fastq files based on the number of outputFiles specified.