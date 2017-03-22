# DownloadNcbiAssembly

## Introduction
 

## Example
The help menu:
~~~

Usage: DownloadNcbiAssembly [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -a <file> | --assembly_report <file>
        refseq ID from NCBI
  -o <file> | --output <file>
        output Fasta file
  --report <file>
        where to write report from ncbi
  --nameHeader <string>
        
 What column to use from the NCBI report for the name of the contigs.
 All columns in the report can be used but this are the most common field to choose from:
 - 'Sequence-Name': Name of the contig within the assembly
 - 'UCSC-style-name': Name of the contig used by UCSC ( like hg19 )
 - 'RefSeq-Accn': Unique name of the contig at RefSeq (default for NCBI)
  --mustHaveOne:<key>=<column_name=regex>
        This can be used to filter based on the NCBI report, multiple conditions can be given, at least 1 should be true
  --mustNotHave:<key>=<column_name=regex>
        This can be used to filter based on the NCBI report, multiple conditions can be given, all should be false


~~~

To run the tool use:
~~~
biopet tool DownloadNcbiAssembly    
~~~


## Output
