# How to create configs

### The sample config

The sample config should be in [__JSON__](http://www.json.org/) or [__YAML__](http://yaml.org/) format. For yaml the file should be named *.yml or *.yaml.

- First field should have the key __"samples"__
- Second field should contain the __"libraries"__
- Third field contains __"R1" or "R2"__ or __"bam"__
- The fastq input files can be provided zipped and unzipped
- `output_dir` is a required setting that should be set either in a `config.json` or specified on the invocation command via -cv output_dir=<path/to/outputdir\>.

#### Example sample config

###### YAML:

``` yaml
output_dir: /home/user/myoutputdir
samples:
  Sample_ID1:
    libraries:
      MySeries_1:
        R1: /path/to/R1.fastq.gz
        R2: /path/to/R2.fastq.gz
```

###### JSON:

``` json
    {  
       "output_dir": "/home/user/myoutputdir",
       "samples":{  
          "Sample_ID1":{  
             "libraries":{  
                "MySeries_1":{  
                   "R1":"Your_R1.fastq.gz",
                   "R2":"Your_R2.fastq.gz"
                }
             }
          }
       }
    }
```

For BAM files as input one should use a config like this:
  
``` yaml
samples:
  Sample_ID_1:
    tags:
      gender: male
      father: sampleNameFather
      mother: sampleNameMother
    libraries:  
      Lib_ID_1:
        tags:
          key: value
        bam: MyFirst.bam
      Lib_ID_2:
        bam: MySecond.bam
```

Note that there is a tool called [SamplesTsvToConfig](../tools/SamplesTsvToConfig.md) that enables the user to get the sample config without any chance of creating a wrongly formatted file.

#### Tags

In the `tags` key inside a sample or library users can supply tags that belong to samples/libraries. These tags will we automatically parsed inside the summary of a pipeline.

### The settings config
The settings config enables a user to alter the settings for almost all settings available in the tools used for a given pipeline.
This config file should be written in either JSON or YAML format. It can contain setup settings like:

 * references,
 * cut offs,
 * program modes and memory limits (program specific),
 * Whether chunking should be used
 * set program executables (if for some reason the user does not want to use the systems default tools)
 * One could set global variables containing settings for all tools used in the pipeline or set tool specific options one layer 
 deeper into the JSON file. E.g. in the example below the settings for Picard tools are altered only for Picard and not global. 


``` json
"picard": { "validationstringency": "LENIENT" } 
```

Global setting examples are:
~~~
"java_gc_timelimit": 98,
"numberchunks": 25,
"chunking": true
~~~


----

#### References
Pipelines and tools that use references should now use the reference module.
This gives a more fine-grained control over references and enables a user to curate the references in a structural way.
E.g. pipelines and tools which use a FASTA references should now set value `"reference_fasta"`.
Additionally, we can set `"reference_name"` for the name to be used (e.g. `"hg19"`). If unset, Biopet will default to `unknown`.
It is also possible to set the `"species"` flag. Again, we will default to `unknown` if unset.

#### Example settings config
``` json
{
        "reference_fasta": "/references/hg19_nohap/ucsc.hg19_nohap.fasta",
        "reference_name": "hg19_nohap",
        "species": "homo_sapiens",
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
```

# More advanced use of config files.
### 4 levels of configuring settings
In biopet, a value of a ConfigNamespace (e.g., "reference_fasta") for a tool or a pipeline can be defined in 4 different levels.
 * Level-4: As a fixed value hardcoded in biopet source code
 * Level-3: As a user specified value in the user config file
 * Level-2: As a system specified value in the global config files. On the LUMC's SHARK cluster, these global config files are located at /usr/local/sasc/config.
 * Level-1: As a default value provided in biopet source code.

During execution, biopet framework will resolve the value for each ConfigNamespace following the order from level-4 to level-1. Hence, a value defined in the a higher level will overwrite a value define in a lower level for the same ConfigNamespace.

### JSON validation

To check if the created JSON file is correct their are several possibilities: the simplest way is using [this](http://jsonformatter.curiousconcept.com/)
website. It is also possible to use Python, Scala or any other programming languages for validating JSON files but this requires some more knowledge.

#Creating config files with Biopet

With the pipelines Gentrap, MultiSampleMapping and Shiva it is possible to use Biopet itself for creating the config files. Biopet should be called with the keyword *template* and the user will be then prompted to enter the values for the parameters needed by the pipelines. Biopet will generate a config file that can be used as input when running the pipelines. The purpose is to ease the step of creating the config files. It is useful especially when Biopet has been pre-configured to use a list of reference genomes. Then the user needs only to specify which refence genome he/she wants to use and the location of the reference genome files can be derived from Biopet's global configuration.

<br/>
<b> Example </b>

For viewing the pipelines for which this functionality is supported:

``` bash
biopet template
```

For getting help about using it for a specific pipeline:

``` bash
biopet template Gentrap -h
```

For running the tool:

``` bash
biopet template Gentrap -o gentrap_config.yml -s gentrap_run.sh
```
<br/>
<b> Description of the parameters </b>

| Flag  (short)| Flag (long) | Type | Function |
| ------------ | ----------- | ---- | -------- |
| -o | --outputConfig | Path (**required**) | Name of the config file that gets generated.|
| -s | --outputScript | Path (optional) | Biopet can also output a script that can be directly used for running the pipeline, the call of the pipeline is generated with the config file as input. This parameter sets the name for the script file.|
| -t | --template | Path (optional) | A template file with 2 placeholders *%s* is required for generating the script. The first placeholder will be replaced with the name of the pipeline, the second with the paths to the sample and settings config files. When Biopet has been pre-configured to use the default template file, then setting this parameter is optional. |
|    | --expert |  | This flag enables the user to configure a more extensive list of parameters for the pipeline. |