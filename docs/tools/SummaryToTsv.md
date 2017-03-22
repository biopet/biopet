# SummaryToTsv

## Introduction
 

## Example
The help menu:
~~~

Usage: SummaryToTsv [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -s <file> | --summary <file>
        
  -o <file> | --outputFile <file>
        
  -p <string> | --path <string>
        
String that determines the values extracted from the summary. Should be of the format:
<header_name>=<namespace>:<lower_namespace>:<even_lower_namespace>...
      
  -m <root|sample|lib> | --mode <root|sample|lib>
        
Determines on what level to aggregate data.
root: at the root level
sample: at the sample level
lib: at the library level
      


~~~

To run the tool use:
~~~
biopet tool SummaryToTsv    
~~~


## Output
