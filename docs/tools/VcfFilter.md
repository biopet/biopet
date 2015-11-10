# VcfFilter

## Introduction
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
        Only show variants that contain unique alleles in compete set for given sample
  --mustHaveVariant <sample>
        Given sample must have 1 alternative allele
  --diffGenotype <sample:sample>
        Given samples must have a different genotype
  --filterHetVarToHomVar <sample:sample>
        If variants in sample 1 are heterogeneous and alternative alleles are homogeneous in sample 2 variants are filtered
  --filterRefCalls
        Filter when there are only ref calls
  --filterNoCalls
        Filter when there are only no calls
  --minQualScore <value>
        Min qual score
~~~

To run the tool:
~~~ bash
biopet tool VcfFilter --inputVcf myInput.vcf \
--outputVcf myOutput.vcf --filterRefCalls --minSampleDepth 
~~~

## Output
The output is a vcf file containing the filters specified values.