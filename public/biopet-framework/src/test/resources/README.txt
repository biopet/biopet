====
    Biopet is built on top of GATK Queue for building bioinformatic
    pipelines. It is mainly intended to support LUMC SHARK cluster which is running
    SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
    should also be able to execute Biopet tools and pipelines.

    Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center

    Contact us at: sasc@lumc.nl

    A dual licensing mode is applied. The source code within this project that are
    not part of GATK Queue is freely available for non-commercial use under an AGPL
    license; For commercial users or users who do not want to follow the AGPL
    license, please contact us to obtain a separate license.
====

Test datasets for the Biopet Framework
======================================

* Please add an entry here when adding a new test dataset.


Filename              Explanation
========              ===========
single01.sam          single-end SAM file, used for testing WipeReads
single01.bam          single01.sam compressed with samtools v0.1.18
single01.bam.bai      index for single01.bam
single02.sam          single-end SAM file, used for testing WipeReads
single02.bam          single02.sam compressed with samtools v0.1.18
single02.bam.bai      index for single02.bam
paired01.sam          paired-end SAM file, used for testing WipeReads
paired01.bam          paired01.sam compressed with samtools v0.1.18
paired01.bam.bai      index for paired01.bam
fake_chrQ.fa          fasta file of a non-existant chrQ of length 16571
fake_chrQ.fa.fai      index for fake_chrQ.fa
fake_chrQ.dict        dict for fake_chrQ.fa
chrQ.vcf              One-line VCF with record on chrQ
chrQ.vcf.gz           bgzipped chrQ.vcf
chrQ.vcf.gz.tbi       index for chrQ.vcf.gz
VEP_oneline.vcf       One-line VCF record annotated with VEP
VEP_oneline.vcf.gz    bgzipped VEP_oneline.vcf
VEP_oneline.vcf.gz.tbi index for VEP_oneline.vcf.gz
VCFv3.vcf             5 line VCF of version 3
unvepped.vcf          one-line VCF
unvepped.vcf.gz       bgzipped unvepped.vcf
unvepped.vcf.gz.tbi   index for unvepped.vcf.gz
