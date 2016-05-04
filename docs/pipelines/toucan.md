Toucan
======

Introduction
-----------
The Toucan pipeline is a VEP-based annotation pipeline. 
Currently, it comprises just two steps by default:

* Variant Effect Predictor run
* [VEP Normalizer on the VEP output](../tools/VepNormalizer.md)

Additionally, annotation and data-sharing with [Varda](http://varda.readthedocs.org/en/latest/) is possible. 

Example
-----------

~~~~bash
biopet pipeline Toucan -h
Arguments for Toucan:
 -Input,--inputvcf <inputvcf>          Input VCF file
 -config,--config_file <config_file>   JSON config file(s)
 -DSC,--disablescatter                 Disable all scatters
~~~~

Configuration
-------------
You can set all the usual [flags and options](http://www.ensembl.org/info/docs/tools/vep/script/vep_options.html) of the VEP in the configuration,
with the same name used by native VEP, except those added after version 75.
The naming scheme for flags an options is indentical to the one used by the VEP
As some of these flags might conflict with other Biopet tools/pipelines, it is wise to put the VEP in its own config namespace.

You **MUST** set the following fields:

* `vep_script`: the path to the VEP executable
* `dir` or `dir_cache`: the path to the VEP cache

It is wise to set the `cache_version` field as well.
Furthermore, the `fork` field will be overwritten by `threads` in case that one exists in the config. 
Therefore, it is recommended not to use `fork`, but to rather use `threads`. 

With that in mind, an example configuration using mode `standard` of the VepNormalizer would thus be:

~~~ json
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
    "output_dir": <path_to_output_directory>
}
~~~

Varda
-----
Annotation with a [Varda](http://varda.readthedocs.org/en/latest/) database instance is possible.
When annotation with Varda is enabled, data-sharing of your variants into Varda is taken care of as well. 
Since Varda requires knowledge about well-covered regions, a gVCF file is additionally ***required*** when using Varda.
This gVCF should contain the same samples as the input VCF.
Toucan will use said gVCF file to generate a bed track of well-covered regions based on the genome quality.

One can enable to use of Varda by setting the `use_varda` config value to `true`. 

Varda requires some additional config values. The following config values are required:
  
  * `varda_root`: URL to Varda root. 
  * `varda_token`: Your user token
  
The following config values are optional: 

  * `varda_verify_certificate`: By default set to `true`. 
  Determines whether the client will verify the SSL certificate. 
  You can also set a path to a certificate file here; 
  This is useful when your Varda instance has a self-signed certificate. 
  * `varda_cache_size`: The size of the cache. Default = 20
  * `varda_buffer_size`: The size of the buffer when sending large files. In bytes. Default = 1 Mib.
  * `varda_task_poll_wait`: Wait time in seconds for Varda poller. Defaults to 2.
     
Annotation queries can be set by the `annotation_queries` config value in the `manwe` config namespace. 
By default, a global query is returned. 


###Groups
In case you want to add your samples to a specific group in your varda database, you can use the tagging system in your sample config.
Specifically, the `varda_group` tag should be a list of strings pointing to group. 

E.g. :

```json
{
    "samples": {
        "sample1": {
            "tags": {
                "varda_group": ["group1", "group2"]
            }
        }
    }
}
```

Running the pipeline
---------------
The command to run the pipeline is:

~~~~ bash
biopet pipeline Toucan -Input <input_vcf> -config <config_json> -run
~~~~

If one wishes to run it on a cluster, the command becomes:

~~~~ bash
biopet pipeline Toucan -Input <input_vcf> -config <config_json> -run -qsub -jobParaEnv <PE>
~~~~

With Varda:

~~~~ bash
biopet pipeline Toucan -Input <input_vcf> -gvcf <gvcf file> -config <config_json> -run -qsub -jobParaEnv <PE> 
~~~~


## Getting Help

If you have any questions on running Toucan, suggestions on how to improve the overall flow, or requests for your favorite VCF annotator to be added, feel free to post an issue to our issue tracker at [GitHub](https://github.com/biopet/biopet).
Or contact us directly via: [SASC email](mailto:SASC@lumc.nl)