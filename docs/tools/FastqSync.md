# FastqSync

## Introduction
 

## Example
The help menu:
~~~


FastqSync - Sync paired-end FASTQ files.

This tool works with gzipped or non-gzipped FASTQ files. The output
file will be gzipped when the input is also gzipped.
      
Usage: FastqSync [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -r <fastq> | --ref <fastq>
        Reference FASTQ file
  -i <fastq> | --in1 <fastq>
        Input FASTQ file 1
  -j <fastq[.gz]> | --in2 <fastq[.gz]>
        Input FASTQ file 2
  -o <fastq[.gz]> | --out1 <fastq[.gz]>
        Output FASTQ file 1
  -p <fastq> | --out2 <fastq>
        Output FASTQ file 2


~~~

To run the tool use:
~~~
biopet tool FastqSync    
~~~


## Output
