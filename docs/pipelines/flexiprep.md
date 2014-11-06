# Introduction

# [Flexiprep](https://git.lumc.nl/biopet/biopet/tree/master/flexiprep)

QC pipeline for fastq files

### Commandline options

| Argument | Explain |
| -------- | ------- |
| -R1,--input_r1 <input_r1> | R1 fastq file (gzipped allowed) |
| -outputDir,--outputdir <outputdir> | Output directory |
| -config,--configfiles <configfiles> | Config Json file |
| -R2,--input_r2 <input_r2> | R2 fastq file (gzipped allowed) |
| -skiptrim,--skiptrim | Skip Trim fastq files |
| -skipclip,--skipclip | Skip Clip fastq files |

---

### Config options

| Config Name | Name |  Type | Default | Function |
| ----------- | ---- | ----- | ------- | -------- |
| flexiprep | skip_native_link |  Boolean | false | Do not make a link to the final file with name: <sample>.qc.<fastq extension> |
| flexiprep | skiptrim | Boolean | false |  |
| flexiprep | skiptrim | Boolean | false |  |

---

### sub Module options

This can be used in the root of the config or within the flexiprep, within flexiprep got prio over the root value

| Config Name | Name | Type | Default | Function |
| ----------- | ---- | ---- | ------- | -------- |
| cutadapt | exe |  String | cutadapt | Excuteble for cutadapt |
| cutadapt | default_clip_mode |  String | 3 | Do not make a link with name: <sample>.qc.<fastq extension> |
| cutadapt | adapter |  Array[String] |  |  |
| cutadapt | anywhere |  Array[String] |  |  |
| cutadapt | front |  Array[String] |  |  |
| cutadapt | discard |  Boolean | false |  |
| cutadapt | opt_minimum_length |  Int | 1 |  |
| cutadapt | opt_maximum_length | Int |  |  |
| fastqc | exe | String | fastqc | Excuteble for fastqc |
| fastqc->java | kmers |  String | java | Excuteble for java for fastqc |
| fastqc | kmers | Int | 5 |  |
| fastqc | quiet | Boolean | false |  |
| fastqc | noextract | Boolean | false |  |
| fastqc | nogroup | Boolean | false |  |
| sickle | exe | String | sickle | Excuteble for sickle |
| sickle | qualitytype | String |  |  |
| sickle | defaultqualitytype | String | sanger | use this when quality type can't be found at fastqc |

---

### License

A dual licensing model is applied. The source code within this project is freely available for non-commercial use under an AGPL license; For commercial users or users who do not want to follow the AGPL license, please contact sasc@lumc.nl to purchase a separate license.





# Invocation

# Example

# Testcase A

# Testcase B

# Examine results

## Result files

## Best practice

# References
