# FindRepeatsPacBio

## Introduction
This tool looks and annotates repeat regions inside a BAM file. It extracts the regions of interest from a bed file and then intersects
those regions with the BAM file. On those extracted regions the tool will perform a
 Mpileup and counts all insertions/deletions etc. etc. for that specific location on a per read basis.


## Example
To get the help menu:
~~~
 java -jar Biopet-0.2.0-DEV-801b72ed.jar tool FindRepeatsPacBio -h
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
 java -jar Biopet-0.2.0.jar tool FindRepeatsPacBio --inputBam myInputbam.bam \
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