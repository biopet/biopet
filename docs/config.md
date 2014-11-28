# How to create configs

### The sample config

The sample config should be in [__JSON__](http://www.json.org/) format

- First field should have the key __"samples"__
- Second field should contain the __"libraries"__
- Third field contains __"R1" or "R2"__ or __"bam"__
- The fastq input files can be provided zipped and un zipped

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