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

#
# oarse_cuffcmp.py
#
# Parses cuffcompare's cuffcmp.stats output into a JSON file.
#
# Part of the Gentrap pipeline.
#
# (c) 2013 by Wibowo Arindrarto [LUMC - SASC]

import argparse
import json
import locale
import os
import re


# set locale to group digits
locale.setlocale(locale.LC_ALL, '')

# precompiled regex patterns
_base_qr = '\s+(\d+)/(\d+)'
_base_table = '\s+(.*?)\s+(.*?)\s+(.*?)\s+(.*?)\s+'
# source transcripts gtf
_re_dataset = re.compile(r'Summary for dataset:\s+(.*?)\s+:')
# ref exons not covered by query exons, total ref exons
_re_rexons = re.compile(r'Missed exons:%s' % _base_qr)
# query exons not covered by ref exons, total query exons
_re_qexons = re.compile(r'Novel exons:%s' % _base_qr)
# ref introns not covered by query introns, total ref introns
_re_rintrons = re.compile(r'Missed introns:%s' % _base_qr)
# query introns not covered by ref introns, total query introns
_re_qintrons = re.compile(r'Novel introns:%s' % _base_qr)
# ref loci not covered by query loci, total ref loci
_re_rloci = re.compile(r'Missed loci:%s' % _base_qr)
# query loci not covered by ref loci, total query loci
_re_qloci = re.compile(r'Novel loci:%s' % _base_qr)
# base level metrics
_re_base = re.compile(r'Base level:%s' % _base_table)
# exon level metrics
_re_exon = re.compile(r'Exon level:%s' % _base_table)
# intron level metrics
_re_intron = re.compile(r'Intron level:%s' % _base_table)
# intron chain level metrics
_re_intron_chain = re.compile(r'Intron chain level:%s' % _base_table)
# transcript level metrics
_re_transcript = re.compile(r'Transcript level:%s' % _base_table)
# locus level metrics
_re_locus = re.compile(r'Locus level:%s' % _base_table)


def _fallback_search(re_pattern, string, match_type, fallback_str, group,
            replacement=None):
    """Function to handle cases when the regex match is of a different type,
    e.g. '-' instead of an integer."""
    match = re.search(re_pattern, string).group(group)

    if match == fallback_str:
        return replacement
    else:
        return match_type(match)


def parse_cuffcmp_stats(stat_file):
    """Parses the statistics in the given cuffcmp.stats file into a
    dictionary."""
    assert os.path.exists(stat_file), "File %r not found" % stat_file

    with open(stat_file, 'r') as source:
        # not expecting a huge output, we can store everything in memory
        stat_str = source.read()

    stats = {
        'dataSet': re.search(_re_dataset, stat_str).group(1),
        'refExonsNotInQuery': int(re.search(_re_rexons, stat_str).group(1)),
        'refExonsTotal': int(re.search(_re_rexons, stat_str).group(2)),
        'queryExonsNotInRef': int(re.search(_re_qexons, stat_str).group(1)),
        'queryExonsTotal': int(re.search(_re_qexons, stat_str).group(2)),

        'refIntronsNotInQuery': int(re.search(_re_rintrons, stat_str).group(1)),
        'refIntronsTotal': int(re.search(_re_rintrons, stat_str).group(2)),
        'queryIntronsNotInRef': int(re.search(_re_qintrons, stat_str).group(1)),
        'queryIntronsTotal': int(re.search(_re_qintrons, stat_str).group(2)),

        'refLociNotInQuery': int(re.search(_re_rloci, stat_str).group(1)),
        'refLociTotal': int(re.search(_re_rloci, stat_str).group(2)),
        'queryLociNotInRef': int(re.search(_re_qloci, stat_str).group(1)),
        'queryLociTotal': int(re.search(_re_qloci, stat_str).group(2)),

        'baseLevelSn': _fallback_search(_re_base, stat_str, float, '-', 1),
        'baseLevelSp': _fallback_search(_re_base, stat_str, float, '-', 2),
        'baseLevelFSn': _fallback_search(_re_base, stat_str, float, '-', 3),
        'baseLevelFSp': _fallback_search(_re_base, stat_str, float, '-', 4),

        'exonLevelSn': _fallback_search(_re_exon, stat_str, float, '-', 1),
        'exonLevelSp': _fallback_search(_re_exon, stat_str, float, '-', 2),
        'exonLevelFSn': _fallback_search(_re_exon, stat_str, float, '-', 3),
        'exonLevelFSp': _fallback_search(_re_exon, stat_str, float, '-', 4),

        'intronLevelSn': _fallback_search(_re_intron, stat_str, float, '-', 1),
        'intronLevelSp': _fallback_search(_re_intron, stat_str, float, '-', 2),
        'intronLevelFSn': _fallback_search(_re_intron, stat_str, float, '-', 3),
        'intronLevelFSp': _fallback_search(_re_intron, stat_str, float, '-', 4),

        'intronChainLevelSn': _fallback_search(_re_intron_chain, stat_str, float, '-', 1),
        'intronChainLevelSp': _fallback_search(_re_intron_chain, stat_str, float, '-', 2),
        'intronChainLevelFSn': _fallback_search(_re_intron_chain, stat_str, float, '-', 3),
        'intronChainLevelFSp': _fallback_search(_re_intron_chain, stat_str, float, '-', 4),

        'transcriptLevelSn': _fallback_search(_re_transcript, stat_str, float, '-', 1),
        'transcriptLevelSp': _fallback_search(_re_transcript, stat_str, float, '-', 2),
        'transcriptLevelFSn': _fallback_search(_re_transcript, stat_str, float, '-', 3),
        'transcriptLevelFSp': _fallback_search(_re_transcript, stat_str, float, '-', 4),

        'locusLevelSn': _fallback_search(_re_locus, stat_str, float, '-', 1),
        'locusLevelSp': _fallback_search(_re_locus, stat_str, float, '-', 2),
        'locusLevelFSn': _fallback_search(_re_locus, stat_str, float, '-', 3),
        'locusLevelFSp': _fallback_search(_re_locus, stat_str, float, '-', 4),
    }

    return stats


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('input', type=str,
            help='Path to input cuffcmp.stats file')
    parser.add_argument('-o', '--output-json', dest='output', type=str,
            help='Path to JSON output file', default=None)
    args = parser.parse_args()

    stats = parse_cuffcmp_stats(args.input)

    if args.output is not None:
        with open(args.output, 'w') as jsonfile:
            json.dump(stats, jsonfile, sort_keys=True, indent=4,
                    separators=(',', ': '))
    else:
        print json.dumps(stats, sort_keys=True, indent=4,
                separators=(',', ': '))
