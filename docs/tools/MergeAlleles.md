# MergeAlleles

## Introduction
This tool is used to merge overlapping alleles. 


## Example
To get the help menu:
~~~
biopet tool MergeAlleles -h
Usage: MergeAlleles [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputVcf <file>
        
  -o <file> | --outputVcf <file>
        
  -R <file> | --reference <file>
~~~

To run the tool:
~~~
java -jar Biopet-0.2.0-DEV-801b72ed.jar tool MergeAlleles \
--inputVcf myInput.vcf --outputVcf myOutput.vcf \
--reference /H.Sapiens/hg19/reference.fa
~~~

## Output
The output of this tool is a VCF file like format containing the merged Alleles only.