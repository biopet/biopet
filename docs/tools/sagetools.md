# SAGE tools
These tools are written to create the appropriate files for the SAGE pipeline. 
Note that these tools are already implemented in the pipeline.

## SageCountFastq
To open the help menu:
~~~
java -jar Biopet-0.2.0.jar tool SageCreateLibrary -h
Usage: SageCountFastq [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --input <file>
        
  -o <file> | --output <file>
~~~

## SageCreateLibrary
To open the help menu:
~~~
java -jar Biopet-0.2.0.jar tool SageCreateLibrary -h
Usage: SageCreateLibrary [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --input <file>
        
  -o <file> | --output <file>
        
  --tag <value>
        
  --length <value>
        
  --noTagsOutput <file>
        
  --noAntiTagsOutput <file>
        
  --allGenesOutput <file>
~~~

## SageCreateTagCounts
To open the help menu:
~~~
java -jar Biopet-0.2.0.jar tool SageCreateTagCounts -h
Usage: SageCreateTagCounts [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --input <file>
        
  -t <file> | --tagLib <file>
        
  --countSense <file>
        
  --countAllSense <file>
        
  --countAntiSense <file>
        
  --countAllAntiSense <file>
~~~