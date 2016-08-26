# ExtractAlignedFastq

## Introduction
This tool extracts reads from a BAM file based on alignment intervals.
E.g if one is interested in a specific location this tool extracts the full reads from the location.
The tool is also very usefull to create test data sets.


## Example
To get the help menu:
~~~
biopet tool ExtractAlignedFastq -h
ExtractAlignedFastq - Select aligned FASTQ records
      
Usage: ExtractAlignedFastq [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <bam> | --input_file <bam>
        Input BAM file
  -r <interval> | --interval <interval>
        Interval strings (e.g. chr1:1-100)
  -i <fastq> | --in1 <fastq>
        Input FASTQ file 1
  -j <fastq> | --in2 <fastq>
        Input FASTQ file 2 (default: none)
  -o <fastq> | --out1 <fastq>
        Output FASTQ file 1
  -p <fastq> | --out2 <fastq>
        Output FASTQ file 2 (default: none)
  -Q <value> | --min_mapq <value>
        Minimum MAPQ of reads in target region to remove (default: 0)
  -s <value> | --read_suffix_length <value>
        Length of suffix mark from each read pair (default: 0). This is used for distinguishing read pairs with
        different suffices. For example, if your FASTQ records end with `/1` for the first pair and `/2` for the
        second pair, the value of `read_suffix_length` should be 2.

This tool creates FASTQ file(s) containing reads mapped to the given alignment intervals. A set of FASTQ files that was
used in creating the BAM file is also required since this is used for retrieving full sequences of FASTQ records which
map to the given region. This is useful since some of the records may have undergone modifications such as quality
trimming before alignment. In this case, retrieving the aligned SAM records will only give the modified sequence.
~~~

To run the tool:
~~~
biopet tool ExtractAlignedFastq \
--input_file myBam.bam --in1 myFastq_R1.fastq --out1 myOutFastq_R1.fastq --interval chr5:100-200
~~~
* Note that this tool works for single end and paired end data. The above example can be easily extended for paired end data.
The only thing one should add is: `--in2 myFastq_R2.fastq --out2 myOutFastq_R2.fastq`
* The interval is just a genomic position or multiple genomic positions wherefrom one wants to extract the reads.


## Output
The output of this tool will be fastq files containing only mapped reads with the given alignment intervals extracted from the bam file.