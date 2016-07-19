# MpileupToVcf

## Introduction
This tool enables a user to extract a VCF file out a mpileup file generated from the BAM file using *samtools mpileup*, for instance. 
The tool can also stream through STDin and STDout so that it is not necessary to store the mpileup file on disk.
Mpileup files can to be very large because they describe each covered base position in the genome on a per read basis,
so it is not desired to store them.

----

## Example
To start the tool:
~~~ bash
biopet tool mpileupToVcf
~~~


To open the help:
~~~ bash
biopet tool mpileupToVcf -h
Usage: MpileupToVcf [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --input <file>
        input, default is stdin
  -o <file> | --output <file>
        out is a required file property
  -s <value> | --sample <value>
        
  --minDP <value>
        
  --minAP <value>
        
  --homoFraction <value>
        
  --ploidy <value>
~~~