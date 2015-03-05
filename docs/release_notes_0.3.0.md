# Release notes BioPet version 0.3.0

Since our first release in December 2014 many new functions have been added to the pipelines including:
- Multisample compatibility
- Copy number analysis
- Structural variants analysis
- Full RNA-seq pipeline
- Annotation pipeline ( still under development )
- Summary framework
    - Md5sum of all input/output files ( need to be provided in the summary )
    - Sequence stats
    - Program stats/versions
    - mapping stats
    - Tool stats ( if pipeline uses a biopet tool, it will output the version of the tool and all other statistics that might be captured )
- GATK variantcalling has a lot of new features and is now called SHIVA

Also a impressive list of tools have been added to the new framework:

- AnnotateVcfWithBed ( This enables the user to Annotate a VCF file based on a bed file containing the locations of interest )
- CheckAllelesVcfInBam
- ExtractAlignedFastq
- FastqSplitter
- FastqSync
- SamplesTsvToJson
- Seqstat ( this is a lift over tool based on our previous python implementation of seqstat )
- VEPNormalizer ( This normalizer enables a user to parse VEP )
- VcfStats
