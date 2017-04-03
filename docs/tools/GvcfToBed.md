# GvcfToBed

## Introduction
 

## Example
The help menu:
~~~

Usage: GvcfToBed [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputVcf <file>
        Input vcf file
  -O <file> | --outputBed <file>
        Output bed file
  --invertedOutputBed <file>
        Output bed file
  -S <sample> | --sample <sample>
        Sample to consider. Will take first sample on alphabetical order by default
  --minGenomeQuality <int>
        Minimum genome quality to consider


~~~

To run the tool use:
~~~
biopet tool GvcfToBed    
~~~


## Output
