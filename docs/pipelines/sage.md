# Introduction
The Sage pipeline has been created to process SAGE data, which requires a different approach than standard NGS data.


# Tools for this pipeline

* [Flexiprep](flexiprep.md)
* [Mapping](mapping.md)
* [SageCountFastq](../tools/sagetools.md)
* [SageCreateLibrary](../tools/sagetools.md)
* [SageCreateTagCounts](../tools/sagetools.md)


# Example
Note that one should first create the appropriate [configs](../config.md).

To get the help menu:
~~~
java -jar Biopet-0.2.0.jar pipeline Sage -h
Arguments for Sage:
 -outDir,--output_directory <output_directory>   Output directory
 --countbed <countbed>                           countBed
 --squishedcountbed <squishedcountbed>           squishedCountBed, by suppling this file the auto squish job will be 
                                                 skipped
 --transcriptome <transcriptome>                 Transcriptome, used for generation of tag library
 -config,--config_file <config_file>             JSON config file(s)
 -DSC,--disablescatterdefault                    Disable all scatters
~~~

To run the pipeline:
~~~
 java -jar Biopet-0.2.0-DEV-801b72ed.jar pipeline Sage -run --config MySamples.json --config --MySettings.json
~~~


# Examine results

## Result files
~~~
.
├── 1A
│   ├── 1A-2.merge.bai
│   ├── 1A-2.merge.bam
│   ├── 1A.fastq
│   ├── 1A.genome.antisense.counts
│   ├── 1A.genome.antisense.coverage
│   ├── 1A.genome.counts
│   ├── 1A.genome.coverage
│   ├── 1A.genome.sense.counts
│   ├── 1A.genome.sense.coverage
│   ├── 1A.raw.counts
│   ├── 1A.tagcount.all.antisense.counts
│   ├── 1A.tagcount.all.sense.counts
│   ├── 1A.tagcount.antisense.counts
│   ├── 1A.tagcount.sense.counts
│   ├── run_1
│   │   ├── 1A-1.bai
│   │   ├── 1A-1.bam
│   │   ├── flexiprep
│   │   └── metrics
│   └── run_2
│       ├── 1A-2.bai
│       ├── 1A-2.bam
│       ├── flexiprep
│       └── metrics
├── 1B
│   ├── 1B-2.merge.bai
│   ├── 1B-2.merge.bam
│   ├── 1B.fastq
│   ├── 1B.genome.antisense.counts
│   ├── 1B.genome.antisense.coverage
│   ├── 1B.genome.counts
│   ├── 1B.genome.coverage
│   ├── 1B.genome.sense.counts
│   ├── 1B.genome.sense.coverage
│   ├── 1B.raw.counts
│   ├── 1B.tagcount.all.antisense.counts
│   ├── 1B.tagcount.all.sense.counts
│   ├── 1B.tagcount.antisense.counts
│   ├── 1B.tagcount.sense.counts
│   ├── run_1
│   │   ├── 1B-1.bai
│   │   ├── 1B-1.bam
│   │   ├── flexiprep
│   │   └── metrics
│   └── run_2
│       ├── 1B-2.bai
│       ├── 1B-2.bam
│       ├── flexiprep
│       └── metrics
├── ensgene.squish.bed
├── summary-33.tsv
├── taglib
    ├── no_antisense_genes.txt
    ├── no_sense_genes.txt
    └── tag.lib
~~~



## Best practice

# References
