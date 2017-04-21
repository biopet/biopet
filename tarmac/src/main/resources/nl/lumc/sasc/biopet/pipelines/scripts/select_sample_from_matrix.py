#!/usr/bin/env python3
#
# Biopet is built on top of GATK Queue for building bioinformatic
# pipelines. It is mainly intended to support LUMC SHARK cluster 
# which is running SGE. But other types of HPC that are supported by 
# GATK Queue (such as PBS) should also be able to execute Biopet tools and 
# pipelines.
#
# Copyright 2014-2017 Sequencing Analysis Support Core - Leiden University Medical Center
#
# Contact us at: sasc@lumc.nl
#
# A dual licensing mode is applied. The source code within this project is 
# freely available for non-commercial use under an AGPL license; 
# For commercial users or users who do not want to follow the AGPL
# license, please contact us to obtain a separate license.
#


import argparse

def xhmm_region_to_bed(region):
    """Convert xhmm-style region to bed-style region."""
    chromosome, interval = region.split(':')
    start, end = interval.split('-')
    return "{0}\t{1}\t{2}".format(chromosome, start, end)

if __name__ == "__main__":
    desc = """
    Extract a sample from an XHMM-style matrix.
    Will print (to stdout) a four-column bed file,
    where the fourth column is the data field.
    """
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument('-I', '--input', required=True, type=str, 
            help='Path to input matrix')
    parser.add_argument('-s', '--sample', required=True, type=str,
            help='Sample name to be extracted')
    args = parser.parse_args()

    values = None

    with open(args.input) as handle:
        header = next(handle).strip().split('\t')[1:]
        regions = [xhmm_region_to_bed(x) for x in header]
        for line in handle:
            if line.startswith(args.sample):
                values = line.strip().split('\t')[1:]
                break
    if values is not None:
        for reg, val in zip(regions, values):
            print(reg+'\t'+val)
    else:
        raise ValueError('sample {0} does not exist'.format(args.sample))

