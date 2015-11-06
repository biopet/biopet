# MpileupToVcf

## Introduction
This tool enables a user to extract a VCF file out a mpileup file generated from the BAM file. 
The tool can also stream through STDin and STDout so that the mpileup file is not stored on disk.
Mpileup files tend to be very large since they describe each covered base position in the genome on a per read basis,
so usually one does not want to safe these files.

----

## Example
To start the tool:
~~~ bash
java -jar Biopet-0.2.0.jar tool mpileupToVcf
~~~


To open the help:
~~~ bash
java -jar Biopet-0.2.0.jar tool mpileupToVcf -h
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