# VcfFilter

## Introduction
This tool filters VCF files on a number values. For example, it can filter on sample depth and/or total depth.
It can also filter out the reference calls and/or minimum number of sample passes.
For more on filtering options and how to set them, please refer to the help menu. 


This tool enables a user to filter VCF files. For example on sample depth and/or total depth.
It can also be used to filter out the reference calls and/or minimum number of sample passes.
There is a wide set of options which one can use to change the filter settings.

## Example
To open the help menu:

~~~ bash
boppet tool VcfFilter -h
Usage: VcfFilter [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -I <file> | --inputVcf <file>
        Input vcf file
  -o <file> | --outputVcf <file>
        Output vcf file
  --minSampleDepth <int>
        Min value for DP in genotype fields
  --minTotalDepth <int>
        Min value of DP field in INFO fields
  --minAlternateDepth <int>
        Min value of AD field in genotype fields
  --minSamplesPass <int>
        Min number of samples to pass --minAlternateDepth, --minBamAlternateDepth and --minSampleDepth
  --minBamAlternateDepth <int>
  --denovoInSample <sample>
        Only keep variants that contain unique alleles in complete set for the given sample
  --mustHaveVariant <sample>
        Only keep variants that for the given sample have an alternative allele
  --diffGenotype <sample:sample>
        Only keep variands that for the given samples have a different genotype
  --filterHetVarToHomVar <sample1:sample2>
        Filter out varianst that are heterozygous in sample1 and homozygous in sample2
  --filterRefCalls
        Filter out ref calls
  --filterNoCalls
        Filter out no calls
  --minQualScore <value>
        Filter out variants with Min qual score below threshold
~~~

To run the tool:
~~~ bash
biopet tool VcfFilter --inputVcf myInput.vcf \
--outputVcf myOutput.vcf --filterRefCalls --minSampleDepth 
~~~

## Output
The output is a vcf file containing the values that pass the user-defined filtering options
