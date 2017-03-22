# FindOverlapMatch

## Introduction
 

## Example
The help menu:
~~~

Usage: FindOverlapMatch [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <file> | --input <file>
        Input should be a table where the first row and column have the ID's, those can be different
  -o <file> | --output <file>
        default to stdout
  -c <value> | --cutoff <value>
        minimum value to report it as pair
  --use_same_names
        Do not compare samples with the same name
  --rowSampleRegex <regex>
        Samples in the row should match this regex
  --columnSampleRegex <regex>
        Samples in the column should match this regex


~~~

To run the tool use:
~~~
biopet tool FindOverlapMatch    
~~~


## Output
