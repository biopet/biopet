# VcfToTsv

## Introduction
This tool enables a user to convert a vcf file to a tab delimited file (TSV). 
This can be very usefull since some programs only accept a TSV for downstream analysis.
It gets rid of the vcf header and parses all data columns in a nice TSV file.
There is also a possibility to only select some specific fields from you vcf and only parse those fields to a TSV.

## Example
To open the help menu:
~~~
java -jar Biopet-0.2.0.jar tool VcfToTsv -h
Usage: VcfToTsv [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        
  -o <file> | --outputFile <file>
        output file, default to stdout
  -f <value> | --field <value>
        
  -i <value> | --info_field <value>
        
  --all_info
        
  --all_format
        
  -s <value> | --sample_field <value>
        
  -d | --disable_defaults
~~~

To run the tool:
~~~
java -jar Biopet-0.2.0.jar tool VcfToTsv --inputFile myVCF.vcf \
--outputFile my_tabDelimited_VCF.tsv --all_info
~~~

## Output
The output of this tool is a TSV file produced from the input vcf file. 
Depending on which options are enabled their could be some fields discarded.