VepNormalizer
============

Introduction
------------
This tool modifies a VCF file annotated with the Variant Effect Predictor (VEP). 
Since the VEP does not use INFO fields to annotate, but rather puts all its annotations in one big string inside a "CSQ" INFO tag it is necessary to normalize it. 

Tool will parse the information in the CSQ header to create INFO fields for each annotation field. Tool has two modes: `standard` and `explode`. 

The `standard` mode will produce a VCF according to the VCF specification.
This means that every VEP INFO tag will consist of the comma-separated list of values for each transcript.
In case the value is empty, the VEP INFO tag will not be shown for that specific record 

Mode `explode` will, on the other hand, create a new VCF record for each transcript it encounters.
This thus means each VEP INFO tag will consist of a single value (if present at all). This can be useful if one must work on a per-transcript basis.
Please note, however, that this means records may seem to be "duplicated".

The CSQ tag is by default removed from the output VCF file. If one wishes to retain it, one can set the `--do-not-remove` option. 


Example
---------
Help menu:

~~~ bash
biopet tool VepNormalizer -h
|VepNormalizer - Parse VEP-annotated VCF to standard VCF format 
Usage: VepNormalizer [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <vcf> | --InputFile <vcf>
        Input VCF file. Required.
  -O <vcf> | --OutputFile <vcf>
        Output VCF file. Required.
  -m <mode> | --mode <mode>
        Mode. Can choose between <standard> (generates standard vcf) and <explode> (generates new record for each transcript). Required.
  --do-not-remove
        Do not remove CSQ tag. Optional

~~~   

