# CheckAllelesVcfInBam

## Introduction
This tool has been written to check the allele frequency in BAM files. This is meant for comparison with the allele frequency reported at the VCF file 

## Example
To get the help menu:
~~~
biopet tool CheckAllelesVcfInBam -h
Usage: CheckAllelesVcfInBam [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputFile <file>
        VCF file
  -o <file> | --outputFile <file>
        output VCF file name
  -s <value> | --sample <value>
        sample name
  -b <value> | --bam <value>
        bam file, from which the variants (VCF files) were called
  -m <value> | --min_mapping_quality <value>
        minimum mapping quality score for a read to be taken into account
~~~

To run the tool:
~~~
biopet tool CheckAllelesVcfInBam --inputFile myVCF.vcf \
--bam myBam1.bam --sample bam_sample1 --outputFile myAlleles.vcf

~~~
Note that the tool can run multiple BAM files at once.
The only thing one needs to make sure off is matching the `--bam` and `--sample` in that same order.

For multiple bam files:
~~~
biopet tool CheckAllelesVcfInBam --inputFile myVCF.vcf \
--bam myBam1.bam --sample bam_sample1 --bam myBam2.bam --sample bam_sample2 \
--bam myBam3.bam --sample bam_sample3 --outputFile myAlleles.vcf
~~~

## Output
outputFile = VCF file which contains an extra field with the allele frequencies per sample given to the tool.
