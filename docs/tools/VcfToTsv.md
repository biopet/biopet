# VcfToTsv

## Introduction
Tool converts a vcf file to a Tab Separated Values (TSV) file. For every key in the INFO column of the VCF file, a separate column will be created with the corresponding values. 
User can select the keys that will be parsed into the output TSV file.
This can be useful in the case a program only accepts a TSV file for downstream analysis.


## Example
To open the help menu:

~~~ bash
biopet tool VcfToTsv -h
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

~~~ bash
biopet tool VcfToTsv --inputFile myVCF.vcf \
--outputFile my_tabDelimited_VCF.tsv --all_info
~~~

## Output
The output of this tool is a TSV file produced from the input vcf file. 
Depending on which options are enabled their could be some fields discarded.