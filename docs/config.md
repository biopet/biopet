# How to create configs

### The sample config

The sample config should be in [__JSON__](http://www.json.org/) format

- First field should have the key __"samples"__
- Second field should contain the __"libraries"__
- Third field contains __"R1" or "R2"__ or __"bam"__
- The fastq input files can be provided zipped and un zipped

#### Example sample config
~~~
    {  
       "samples":{  
          "Sample_ID1":{  
             "libraries":{  
                "MySeries_1":{  
                   "R1":"Youre_R1.fastq.gz",
                   "R2":"Youre_R2.fastq.gz"
                }
             }
          }
       }
    }
~~~

- For BAM files as input one should use a config like this:
  
~~~
    {
       "samples":{  
          "Sample_ID_1":{  
             "libraries":{  
                "Lib_ID_1":{  
                   "bam":"MyFirst.bam"
                },
                "Lib_ID_2":{  
                   "bam":"MySecond.bam"
                }
             }
          }
       }
    }
~~~


Note that there is a tool called [SamplesTsvToJson](tools/SamplesTsvToJson.md) this enables a user to get the sample config without any chance of creating a wrongly formatted JSON file.


### The settings config
The settings config enables a user to alter the settings for almost all settings available in the tools used for a given pipeline.
This config file should be written in JSON format. It can contain setup settings like references for the tools used,
if the pipeline should use chunking or setting memory limits for certain programs almost everything can be adjusted trough this config file.
One could set global variables containing settings for all tools used in the pipeline or set tool specific options one layer deeper into the JSON file.
E.g. in the example below the settings for Picard tools are altered only for Picard and not global. 

~~~
"picard": { "validationstringency": "LENIENT" } 
~~~

Global setting examples are:
~~~
"java_gc_timelimit": 98,
"numberchunks": 25,
"chunking": true
~~~


----

#### Example settings config
~~~
{
        "reference": "/data/LGTC/projects/vandoorn-melanoma/data/references/hg19_nohap/ucsc.hg19_nohap.fasta",
        "dbsnp": "/data/LGTC/projects/vandoorn-melanoma/data/references/hg19_nohap/dbsnp_137.hg19_nohap.vcf",
        "joint_variantcalling": false,
        "haplotypecaller": { "scattercount": 100 },
        "multisample": { "haplotypecaller": { "scattercount": 1000 } },
        "picard": { "validationstringency": "LENIENT" },
        "library_variantcalling_temp": true,
        "target_bed_temp": "/data/LGTC/projects/vandoorn-melanoma/analysis/target.bed",
        "min_dp": 5,
        "bedtools": {"exe":"/share/isilon/system/local/BEDtools/bedtools-2.17.0/bin/bedtools"},
        "bam_to_fastq": true,
        "baserecalibrator": { "memory_limit": 8, "vmem":"16G" },
        "samtofastq": {"memory_limit": 8, "vmem": "16G"},
        "java_gc_timelimit": 98,
        "numberchunks": 25,
        "chunking": true,
        "haplotypecaller": { "scattercount": 1000 }
}
~~~

### JSON validation

To check if the JSON file created is correct we can use multiple options the simplest way is using [this](http://jsonformatter.curiousconcept.com/)
website. It is also possible to use Python or Scala for validating but this requires some more knowledge.