# MergeTables

## Introduction
 

## Example
The help menu:
~~~


MergeTables - Tabular file merging based on feature ID equality.
      
Usage: MergeTables [options] [<input_tables> ...]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <idx1>,<idx2>, ... | --id_column_index <idx1>,<idx2>, ...
        Index of feature ID column from each input file (1-based)
  -a <idx> | --value_column_index <idx>
        Index of column from each input file containing the value to merge (1-based)
  -o <path> | --output <path>
        Path to output file (default: '-' <stdout>)
  -n <name> | --id_column_name <name>
        Name of feature ID column in the output merged file (default: feature)
  -N <name> | --column_names <name>
        Name of feature ID column in the output merged file (default: feature)
  -e <ext> | --strip_extension <ext>
        Common extension of all input tables to strip (default: empty string)
  -m <value> | --num_header_lines <value>
        The number of header lines present in all input files (default: 0; no header)
  -f <value> | --fallback <value>
        The string to use when a value for a feature is missing in one or more sample(s) (default: '-')
  -d <value> | --delimiter <value>
        The character used for separating columns in the input files (default: '\t')
  <input_tables> ...
        Input tables to merge

This tool merges multiple tab-delimited files and outputs a single
tab delimited file whose columns are the feature IDs and a single
column from each input files.

Note that in each input file there must not be any duplicate features.
If there are, the tool will only keep one and discard the rest.
      


~~~

To run the tool use:
~~~
biopet tool MergeTables    
~~~


## Output
