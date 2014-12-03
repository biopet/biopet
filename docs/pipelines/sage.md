# Introduction
The Sage pipeline has been created to process SAGE data, which requires a different approach than NGS data.



# Invocation

# Example
Note that one should first create the appropriate [configs](../config.md).
~~~
java -jar Biopet-0.2.0-DEV-801b72ed.jar pipeline Sage -h
Arguments for Sage:
 -outDir,--output_directory <output_directory>   Output directory
 --countbed <countbed>                           countBed
 --squishedcountbed <squishedcountbed>           squishedCountBed, by suppling this file the auto squish job will be 
                                                 skipped
 --transcriptome <transcriptome>                 Transcriptome, used for generation of tag library
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters
~~~


# Testcase A

# Testcase B

# Examine results

## Result files

## Best practice

# References
