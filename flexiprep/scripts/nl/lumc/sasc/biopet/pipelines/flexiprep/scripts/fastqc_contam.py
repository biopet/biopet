#!/usr/bin/env python

import argparse
import os

from pyfastqc import load_from_dir


def parse_contam_file(contam_file, delimiter='\t'):
    """Given a contaminant file, return a dictionary of contaminant sequences
    names and their sequences.

    Args:
        contam_file -- path to contaminants file
        delimiter -- ID, sequence delimiter in the contaminants file
    """
    assert os.path.exists(contam_file), "Contaminant file %r does not exist" % \
            contam_file
    with open(contam_file, 'r') as source:
        # read only lines not beginning with '#' and discard empty lines
        lines = filter(None, (line.strip() for line in source if not
            line.startswith('#')))
        # parse contam seq lines into lists of [id, sequence]
        parse = lambda line: filter(None, line.split(delimiter))
        parsed = (parse(line) for line in lines)
        # and create a dictionary, key=sequence id and value=sequence
        contam_ref = {name: seq for name, seq in parsed}

    return contam_ref


def get_contams_present(results_dir, contam_ref):
    """Given a path to a FastQC HTML results file, return the <div> tag of the
    overrepresented sequences list.

    Args:
        results_dir -- Path to FastQC results directory.
    """
    assert os.path.exists(results_dir), "Directory {0} not " \
            "found.".format(results_dir)

    fastqc = load_from_dir(results_dir)
    contam_names = set([x[3] for x in fastqc.overrepresented_sequences.data])
    in_sample = lambda rid: any([cid.startswith(rid) for cid in contam_names])
    contams_present = {x: y for x, y in contam_ref.items() if in_sample(x)}

    return contams_present


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('results_dir', type=str, 
            help='Path to FastQC result directory file')
    parser.add_argument('-c', '--contam_file', type=str,
            dest='contam_file',
            help='Path to contaminant file')
    parser.add_argument('-o', '--output', type=str,
            dest='output',
            help='Path to output file')
    parser.add_argument('--seq-only',dest='seq_only',
            action='store_true',
            help='Whether to output contaminant sequences only or not')

    args = parser.parse_args()

    contam_ref = parse_contam_file(args.contam_file)
    contam_ids = get_contams_present(args.results_dir, contam_ref)

    if args.seq_only:
       fmt_out = lambda cid, seq: seq
    else:
       fmt_out = lambda cid, seq: "{0}\t{1}".format(cid, seq)

    if args.output is None:
        for cid, seq in contam_ids.items():
            print fmt_out(cid, seq)
    else:
        with open(args.output, 'w') as target:
            for cid, seq in contam_ids.items():
                target.write(fmt_out(cid, seq) + '\n')
