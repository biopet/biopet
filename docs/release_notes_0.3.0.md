# Release notes Biopet version 0.3.0

Since our first release in December 2014 many new functions have been added to the pipelines including:
- Multisample compatibility
- Summary framework
    - Md5sum of all input/output files ( need to be provided in the summary )
    - Sequence stats
    - Program stats/versions
    - mapping stats
    - Tool stats ( if pipeline uses a biopet tool, it will output the version of the tool and all other statistics that might be captured )
- A entire new pipeline named Gentrap based on our previous [Makefile version](http://sasc-server.lumcnet.prod.intern/pipelines/makefile-0.6.0/gentrap/), with extra features like:
    - remove all ribosomal reads
    - multi sample runs
- GATK VariantCalling has a lot of new features and is now called Shiva
- Annotation pipeline ( development version )

Also a impressive list of tools have been added to the updated framework:

- AnnotateVcfWithBed ( This enables the user to Annotate a VCF file based on a bed file containing the locations of interest )
- CheckAllelesVcfInBam
- ExtractAlignedFastq
- FastqSplitter
- FastqSync
- MergeTables
- SamplesTsvToJson
- Seqstat ( this is a lift over tool based on our previous python implementation of seqstat )
- VEPNormalizer ( This normalizer enables a user to parse VEP output VCFs to the exact specs of [VCF 4.1](https://samtools.github.io/hts-specs/VCFv4.1.pdf) )
- VcfStats

Some tools have a new version for better compatibility with our latest pipelines. The tools that have a changed version are:
- FastQC v0.11.2
- seqTK Version: 1.0-r63-dirty
- sickle version 1.33
- Cutadapt 1.5
- GSNAP version 2014-12-22
- TopHat v2.0.13
- cufflinks v2.2.1
- HTseq-count version 0.6.1p1
- pdfTeX, Version 3.1415926-2.5-1.40.14 (pdflatex)
- R scripting front-end version 3.1.1 (2014-07-10) (Rscript)
- tabix Version: 0.2.5 (r1005)
- grep (GNU grep) 2.16
- gzip 1.6
- Samtools Version: 1.1
