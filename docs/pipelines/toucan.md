TOUCAN
===========

Introduction
-----------
The Toucan pipeline is a VEP-based annotation pipeline. 
Currently, it comprises just two steps:

* Variant Effect Predictor run
* [VEP Normalizer on the VEP output](../tools/VEPNormalizer.md)

Example
-----------
~~~~bash
java -jar Biopet-0.3.0.jar pipeline Toucan -h
Arguments for Toucan:
 -Input,--inputvcf <inputvcf>          Input VCF file
 -config,--config_file <config_file>   JSON config file(s)
 -DSC,--disablescatter                 Disable all scatters
~~~~

Configuration
-------------
You can set all the usual [flags and options](http://www.ensembl.org/info/docs/tools/vep/script/vep_options.html) of the VEP in the configuration,
with the same name used by native VEP.
As some of these flags might conflict with other Biopet tools/pipelines, it is wise to put the VEP in its own JSON object.

You **MUST** set the following fields:

* `vep_script`: the path to the VEP executable
* `dir` or `dir_cache`: the path to the VEP cache

It is wise to set the `cache_version` field as well.
Furthermore, the `fork` field will be overwritten by `threads` in case that one exists in the config. 
Therefore, it is recommended not to use `fork`, but to rather use `threads`. 

With that in mind, an example configuration using mode `standard` of the VEPNormalizer would thus be:
~~~~
{
    "varianteffectpredictor": {
        "vep_script": <path_to_exe>,
        "dir": <path_to_cache>,
        "cache_version": <cache_version>,
        "threads": 8 
    },
    "vepnormalizer": {
        "mode": "standard"
    },
    "out_dir": <path_to_output_directory>
}
~~~~

Running the pipeline
---------------
The command to run the pipeline is:
~~~~
java -jar pipeline Toucan -Input <input_vcf> -config <config_json> -run
~~~~

If one wishes to run it on a cluster, the command becomes
~~~~
java -jar pipeline Toucan -Input <input_vcf> -config <config_json> -run -qsub -jobParaEnv <PE>
~~~~
