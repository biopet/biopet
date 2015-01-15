# BiopetFlagstat

## Introduction
This tool has been created to extract all the metrics from a required bam file.
It captures for example the # of mapped reads, # of duplicates, # of mates unmapped, # of reads with a certain mapping quality etc. etc.


## Example
To get the help menu:
~~~
java -jar Biopet-0.2.0.jar tool BiopetFlagstat -h
Usage: BiopetFlagstat [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        out is a required file property
  -r <chr:start-stop> | --region <chr:start-stop>
        out is a required file property
~~~

To run the tool:
~~~
java -jar Biopet-0.2.0.jar tool BiopetFlagstat -I myBAM.bam
~~~

### Output

|Number	|Total Flags|	Fraction|	Name|
|------ | --------  | --------- | ------|
|1	|862623034|	100.0000%|	All|
|2	|861096240|	99.8230%|	Mapped|
|3	|26506366|	3.0728%|	Duplicates|
|4	|431233321|	49.9909%|	FirstOfPair|
|5	|431389713|	50.0091%|	SecondOfPair|
|6	|430909871|	49.9534%|	ReadNegativeStrand|
|7	|0|	0.0000%|	NotPrimaryAlignment|
|8	|862623034|	100.0000%|	ReadPaired|
|9	|803603283|	93.1581%|	ProperPair|
|10	|430922821|	49.9549%|	MateNegativeStrand|
|11	|1584255|	0.1837%|	MateUnmapped|
|12	|0|	0.0000%|	ReadFailsVendorQualityCheck|
|13	|1380318|	0.1600%|	SupplementaryAlignment|
|14	|1380318|	0.1600%|	SecondaryOrSupplementary|
|15	|821996241|	95.2903%|	MAPQ>0|
|16	|810652212|	93.9753%|	MAPQ>10|
|17	|802852105|	93.0710%|	MAPQ>20|
|18	|789252132|	91.4944%|	MAPQ>30|
|19	|770426224|	89.3120%|	MAPQ>40|
|20	|758373888|	87.9149%|	MAPQ>50|
|21	|0|	0.0000%|	MAPQ>60|
|22	|835092541|	96.8085%|	First normal, second read inverted (paired end orientation)|
|23	|765156|	0.0887%|	First normal, second read normal|
|24	|624090|	0.0723%|	First inverted, second read inverted|
|25	|11537740|	1.3375%|	First inverted, second read normal|
|26	|1462857|	0.1696%|	Mate in same strand|
|27	|11751691|	1.3623%|	Mate on other chr|