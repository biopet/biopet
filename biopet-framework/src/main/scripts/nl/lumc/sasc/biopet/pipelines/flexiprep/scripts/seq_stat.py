#!/usr/bin/env python2

"""
Simple sequencing file statistics.


Gathers various statistics of a FASTQ file.

Requirements:
    * Python == 2.7.x
    * Biopython >= 1.60

Copyright (c) 2013 Wibowo Arindrarto <w.arindrarto@lumc.nl>
Copyright (c) 2013 LUMC Sequencing Analysis Support Core <sasc@lumc.nl>
MIT License <http://opensource.org/licenses/MIT>
"""

RELEASE = False
__version_info__ = ('0', '2', )
__version__ = '.'.join(__version_info__)
__version__ += '-dev' if not RELEASE else ''


import argparse
import json
import os
import sys

from Bio import SeqIO


# quality points we want to measure
QVALS = [1] + range(10, 70, 10)


def dict2json(d_input, f_out):
    """Dump the given dictionary as a JSON file."""
    if isinstance(f_out, basestring):
        target = open(f_out, 'w')
    else:
        target = f_out

    json.dump(d_input, target, sort_keys=True, indent=4,
            separators=(',', ': '))

    target.close()


def gather_stat(in_fastq, out_json, fmt):

    total_bases, total_reads = 0, 0

    bcntd = dict.fromkeys(QVALS, 0)
    rcntd = dict.fromkeys(QVALS, 0)

    len_max, len_min = 0, None
    n_bases, reads_with_n = 0, 0
    # adjust format name to Biopython-compatible name
    for rec in SeqIO.parse(in_fastq, 'fastq-' + fmt):
        read_quals = rec.letter_annotations['phred_quality']
        read_len = len(read_quals)

        # compute quality metrics
        avg_qual = sum(read_quals) / len(read_quals)
        for qval in QVALS:
            bcntd[qval] += len([q for q in read_quals if q >= qval])
            if avg_qual >= qval:
                rcntd[qval] += 1

        # compute length metrics
        if read_len > len_max:
            len_max = read_len
        if len_min is None:
            len_min = read_len
        elif read_len < len_min:
            len_min = read_len

        # compute n metrics
        n_count = rec.seq.lower().count('n')
        n_bases += n_count
        if n_count > 0:
            reads_with_n += 1

        total_bases += read_len
        total_reads += 1

    pctd = {
        'files': {
            'fastq': {
                'path': os.path.abspath(in_fastq),
                'checksum_sha1': None,
            },
        },
        'stats': {
            'qual_encoding': fmt,
            'bases': {
                'num_qual_gte': {},
                'num_n': n_bases,
                'num_total': total_bases,
            },
            'reads': {
                'num_mean_qual_gte': {},
                'len_max': len_max,
                'len_min': len_min,
                'num_with_n': reads_with_n,
                'num_total': total_reads,
            },
        },
    }

    for qval in QVALS:
        pctd['stats']['bases']['num_qual_gte'][qval] = bcntd[qval]
        pctd['stats']['reads']['num_mean_qual_gte'][qval] = rcntd[qval]

    dict2json(pctd, out_json)


if __name__ == '__main__':

    usage = __doc__.split('\n\n\n')
    parser = argparse.ArgumentParser(
            formatter_class=argparse.RawDescriptionHelpFormatter,
            description=usage[0], epilog=usage[1])

    parser.add_argument('input', type=str, help='Path to input FASTQ file')
    parser.add_argument('-o', '--output', type=str, default=sys.stdout,
            help='Path to output JSON file')
    parser.add_argument('--fmt', type=str, choices=['sanger', 'illumina',
        'solexa'], default='sanger', help='FASTQ quality encoding')
    parser.add_argument('--version', action='version', version='%(prog)s ' +
            __version__)

    args = parser.parse_args()

    gather_stat(args.input, args.output, args.fmt)
