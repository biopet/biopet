# AnnotateVcfWithBed 

## Introduction
 This tool to annotates a vcf file with input from a bed file

## Example
To get the help menu:
~~~
Usage: AnnotateVcfWithBed [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <vcf file> | --inputFile <vcf file>
        Input is a required file property
  -B <bed file> | --bedFile <bed file>
        Bedfile is a required file property
  -o <vcf file> | --output <vcf file>
        out is a required file property
  -f <name of field in vcf file> | --fieldName <name of field in vcf file>
        Name of info field in new vcf file
  -d <name of field in vcf file> | --fieldDescription <name of field in vcf file>
        Description of field in new vcf file
  -t <name of field in vcf file> | --fieldType <name of field in vcf file>
        Description of field in new vcf file
~~~

To run the tool:
~~~
biopet tool AnnotateVcfWithBed -I myVcf.vcf -B myBed.bed -o myannotatedVcf.vcf  
~~~


## Results
The result of this tool will be a vcf file with an extra field with annotation

