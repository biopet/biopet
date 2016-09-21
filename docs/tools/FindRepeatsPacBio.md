# FindRepeatsPacBio

## Introduction
This tool searches for and annotates repeat regions inside a BAM file. 
It intersect the regions provided in the bed file with the BAM file and extracts them. 
On the extracted regions *samtools mpileup* will be run and all insertions, deletions or substitutions will be counted on a per read basis 



## Example
To get the help menu:
~~~
biopet tool FindRepeatsPacBio -h
Usage: FindRepeatsPacBio [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputBam <file>
        
  -b <file> | --inputBed <file>
        output file, default to stdout
~~~

To run the tool:
~~~
biopet tool FindRepeatsPacBio --inputBam myInputbam.bam \
--inputBed myRepeatRegions.bed > mySummary.txt
~~~
Since the default output of the program is printed in stdout we can use > to write the output to a text file.


## Output
The Output is a tab delimited text file which looks like this:

|chr  |startPos|stopPos |Repeat_seq|repeatLength|original_Repeat_readLength|
|-----|--------|--------|----------|------------|--------------------------|
|chr4 |3076603 |3076667 |CAG       |3     	|65                        |
|chr4 |3076665 |3076667 |GCC       |3           |3                         |
|chrX |66765158|66765261|GCA       |3           |104                       |

table continues below:

|Calculated_repeat_readLength|minLength|maxLength|inserts                              |
|----------------------------|---------|---------|-------------------------------------|
|61,73,68                    |61       |73       |GAC,G,T/A,C,G,G,A,G,A,G/C,C,C,A,C,A,G|
|3,3,3                       |3        |3        |//                                   |
|98                          |98       |98       |A,G,G                                |

table continues below:

|deletions           |notSpan|
|--------------------|-------|
|1,1,2,1,1,1,2//2,1,1|0      |
|//                  |0      |
|1,1,1,1,1,1,2,1     |0      |