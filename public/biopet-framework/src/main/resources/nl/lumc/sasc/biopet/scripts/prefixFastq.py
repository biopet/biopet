#!/usr/bin/env python
#
# Biopet is built on top of GATK Queue for building bioinformatic
# pipelines. It is mainly intended to support LUMC SHARK cluster which is running
# SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
# should also be able to execute Biopet tools and pipelines.
#
# Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
#
# Contact us at: sasc@lumc.nl
#
# A dual licensing mode is applied. The source code within this project that are
# not part of GATK Queue is freely available for non-commercial use under an AGPL
# license; For commercial users or users who do not want to follow the AGPL
# license, please contact us to obtain a separate license.
#


# prep_deepsage.py
#
# Script for preprocessing deepSAGE sequencing samples.
#
# Adapted from: http://galaxy.nbic.nl/u/pacthoen/w/deepsagelumc

import argparse

from Bio import SeqIO

PREFIX = 'CATG'

def prep_deepsage(input_fastq, output_fastq, is_gzip, seq):
    if is_gzip:
        import gzip
        opener = gzip.GzipFile
    else:
        opener = open
    with opener(input_fastq, 'r') as in_handle, open(output_fastq, 'w') as \
            out_handle:
        # adapted from fastools.fastools.add
        # not importing since we also need to cut away parts of the sequence
        for rec in SeqIO.parse(in_handle, 'fastq'):
            qual = rec.letter_annotations['phred_quality']
            rec.letter_annotations = {}
            rec.seq = seq + rec.seq
            rec.letter_annotations = {'phred_quality': [40] * len(seq) +
                    qual}
            SeqIO.write(rec, out_handle, "fastq")


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('input_fastq', type=str,
            help="Path to input FASTQ file")
    parser.add_argument('-o', '--output', type=str,
            dest='output_fastq',
            default="prep_deepsage_output.fq",
            help="Path to output FASTQ file")
    parser.add_argument('--gzip', dest='gzip',
            action='store_true',
            help="Whether input FASTQ file is gzipped or not.")
    parser.add_argument('--prefix', dest='prefix', type=str,
            default=PREFIX,
            help="Whether input FASTQ file is gzipped or not.")

    args = parser.parse_args()

    prep_deepsage(args.input_fastq, args.output_fastq, args.gzip, args.prefix)
