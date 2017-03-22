# VcfWithVcf

## Introduction
 

## Example
The help menu:
~~~

INFO  [2017-03-22 10:08:02,817] [VcfWithVcf$] - Init phase
Usage: VcfWithVcf [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        
  -o <file> | --outputFile <file>
        
  -s <file> | --secondaryVcf <file>
        
  -R <file> | --reference <file>
        
  -f <field> or <input_field:output_field> or <input_field:output_field:method> | --field <field> or <input_field:output_field> or <input_field:output_field:method>
         If only <field> is given, the field's identifier in the output VCF will be identical to <field>.
 By default we will return all values found for a given field.
 For INFO fields with type R or A we will take the respective alleles present in the input file.
 If a <method> is supplied, a method will be applied over the contents of the field.
 In this case, all values will be considered.
 The following methods are available:
   - max   : takes maximum of found value, only works for numeric (integer/float) fields
   - min   : takes minimum of found value, only works for numeric (integer/float) fields
   - unique: takes only unique values 
  --match <Boolean>
        Match alternative alleles; default true


~~~

To run the tool use:
~~~
biopet tool VcfWithVcf    
~~~


## Output
