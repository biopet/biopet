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
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        Input vcf file
  -o <file> | --outputFile <file>
        output file, default to stdout
  -f Genotype field name | --field Genotype field name
        Genotype field to use
  -i Info field name | --info_field Info field name
        Info field to use
  --all_info
        Use all info fields in the vcf header
  --all_format
        Use all genotype fields in the vcf header
  -s <value> | --sample_field <value>
        Genotype fields to use in the tsv file
  -d | --disable_defaults
        Don't output the default columns from the vcf file
  --separator <value>
        Optional separator. Default is tab-delimited
  --list_separator <value>
        Optional list separator. By default, lists are separated by a comma
  --max_decimals <value>
        Number of decimal places for numbers. Default is 2
~~~

To run the tool:

~~~ bash
biopet tool VcfToTsv --inputFile myVCF.vcf \
--outputFile my_tabDelimited_VCF.tsv --all_info
~~~

## Output
The output of this tool is a TSV file produced from the input vcf file. 
Depending on which options are enabled their could be some fields discarded.