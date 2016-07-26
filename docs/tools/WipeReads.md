# WipeReads

## Introduction
WipeReads is a tool for removing reads from indexed BAM files that are inside a user defined region.
It takes pairing information into account and can be set to remove reads if one of the pairs maps outside of the target region. 
An application example is to remove reads mapping to known ribosomal RNA regions (using a supplied BED file containing intervals for these regions).

## Example
To open the help menu:

~~~ bash
biopet tool WipeReads -h

WipeReads - Region-based reads removal from an indexed BAM file
      
Usage: WipeReads [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <bam> | --input_file <bam>
        Input BAM file
  -r <bed/gtf/refflat> | --interval_file <bed/gtf/refflat>
        Interval BED file
  -o <bam> | --output_file <bam>
        Output BAM file
  -f <bam> | --discarded_file <bam>
        Discarded reads BAM file (default: none)
  -Q <value> | --min_mapq <value>
        Minimum MAPQ of reads in target region to remove (default: 0)
  -G <rgid> | --read_group <rgid>
        Read group IDs to be removed (default: remove reads from all read groups)
  --limit_removal
        Whether to remove multiple-mapped reads outside the target regions (default: yes)
  --no_make_index
        Whether to index output BAM file or not (default: yes)

GTF-only options:
  -t <gtf_feature_type> | --feature_type <gtf_feature_type>
        GTF feature containing intervals (default: exon)

Advanced options:
  --bloom_size <value>
        Expected maximum number of reads in target regions (default: 7e7)
  --false_positive <value>
        False positive rate (default: 4e-7)

This tool will remove BAM records that overlaps a set of given regions.
By default, if the removed reads are also mapped to other regions outside
the given ones, they will also be removed.
~~~

To run the tool:

~~~ bash
biopet tool WipeReads --input_file myBam.bam \
--interval_file myRibosomal_regions.bed --output_file myFilteredBam.bam
~~~

## Output
This tool outputs a bam file containing all the reads not inside the ribosomal region.
It can optionally output a bam file with only the reads inside the ribosomal region
