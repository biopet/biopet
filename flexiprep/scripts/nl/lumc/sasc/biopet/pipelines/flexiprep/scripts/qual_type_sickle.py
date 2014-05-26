#!/usr/bin/env python

import argparse

from pyfastqc import load_from_dir

## CONSTANTS ##
# fastqc-sickle encoding name map
# from FastQC source
# (uk.ac.babraham.FastQC.Sequence.QualityEncoding.PhredEncoding.java)
# and sickle source (src/sickle.h)
FQ_SICKLE_ENC_MAP = {
    'Sanger / Illumina 1.9': ('sanger', 33),
    'Illumina <1.3': ('solexa', 59),
    'Illumina 1.3': ('illumina', 64),
    'Illumina 1.5': ('illumina', 64),
}


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('results_dir', type=str, 
            help='Path to FastQC result directory file')
    parser.add_argument('--force', type=str,
            help='Force quality to use the given encoding')

    args = parser.parse_args()

    fastqc = load_from_dir(args.results_dir)
    if args.force is None:
        enc, offset = FQ_SICKLE_ENC_MAP.get(fastqc.basic_statistics.data['Encoding'])
    else:
        enc = args.force
        offset = [x[1] for x in FQ_SICKLE_ENC_MAP.values() if x[0] == enc][0]

    print "{0}\t{1}".format(enc, offset)
