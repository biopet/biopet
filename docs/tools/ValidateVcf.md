# ValidateVcf

## Introduction
 

## Example
The help menu:
~~~

INFO  [2017-03-22 10:08:02,092] [ValidateVcf$] - Start
Usage: ValidateVcf [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <file> | --inputVcf <file>
        Vcf file to check
  -R <file> | --reference <file>
        Reference fasta to check vcf file against
  --disableFail
        Do not fail on error. The tool will still exit when encountering an error, but will do so with exit code 0


~~~

To run the tool use:
~~~
biopet tool ValidateVcf    
~~~


## Output
