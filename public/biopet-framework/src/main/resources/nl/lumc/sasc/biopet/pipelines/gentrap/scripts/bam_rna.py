#!/usr/bin/env python2.7

# bam_rna.py
#
# Generate read pair and alignment statistics for a BAM file.
#
# Part of the Gentrap pipeline
#
# (c) 2013 Wibowo Arindrarto [SASC - LUMC]

import argparse
import json
import locale
import os
from collections import OrderedDict
from functools import partial

import pysam

# set locale to group digits
locale.setlocale(locale.LC_ALL, '')
# formatters for output
int_fmt = partial(locale.format, grouping=True, percent='%i')
float_fmt = partial(locale.format, grouping=True, percent='%.2f')

# -F 0x4
func_nmap = lambda rec: not rec.flag & 0x4
# -f 0xC
func_nunmap = lambda rec: (rec.flag & 0x4) and (rec.flag & 0x8)
# -F 0xC
func_nmap_pair = lambda rec: not rec.flag & 0xC
# -f 0x2
func_nmap_pair_ok = lambda rec: rec.flag & 0x2
# mapped to different chromosomes / reference sequences
func_nmap_diffchr = lambda rec: rec.rnext != rec.tid
# -F 0x4 -f 0x8
func_nmap_sgltn = lambda rec: (not rec.flag & 0x4) and (rec.flag & 0x8)
# check if spliced
func_splice = lambda rec: 'N' in rec.cigarstring
# check if pass QC
# not bool since we always need to run each read through it
func_qc = lambda rec: 1 if rec.flag & 0x200 else 0

FLAGS = OrderedDict((
    ('total', 'Total'),
    ('unmapped', 'Unmapped'), 
    ('mapped', 'Mapped'),
    ('mappedPair', 'Mapped pairs'),
    ('mappedPairProper', 'Properly mapped pairs'),
    ('mappedDiffChr', 'Different chromosomes'),
    ('mappedDiffChrQ', 'Different chromosomes, MAPQ >=5'),
    ('singleton', 'Singletons'),
    ('totalSplice', 'Split reads, total'),
    ('splicePairProper', 'Split reads, properly mapped'),
    ('spliceSingleton', 'Split reads, singletons'),
))

class BarnStat(object):

    """Class representing a collection of BAM statistics for RNA-seq data."""

    def __init__(self, bamfile, read_pair_suffix_len=0, id_sorted=False,
            validate=False):
        assert os.path.exists(bamfile), "BAM file %r not found." % bamfile
        self.validate = validate
        self.bamfile = bamfile

        self.flags = FLAGS.keys()
        # length of read pair suffix (e.g. '/2' has len == 2)
        self.suflen = read_pair_suffix_len
        if not id_sorted:
            self._count_unsorted()
        else:
            self._count_sorted()

        self._format_counts()

    def _adjust_counts(self, alns, reads):
        """Adjusts the alignment and read counts."""
        # we need to adjust the unmapped counts for alignments as each
        # alignments consists of two reads (one read pair) which may be mapped
        # multiple times as singletons and/or whole read pairs.
        # so unmapped alns is always == unmapped read pairs + singleton *reads*
        # counts (proxy for the set of unmapped singletons)
        if 'unmapped' in alns:
            alns['unmapped'] += reads['singleton']
        else:
            # tophat, splits unmapped reads into its own BAM file
            alns['unmapped'] = 0

        return alns, reads

    def _count_sorted(self):
        """Counts read and alignment statistics for ID-sorted BAM file."""
        flags = self.flags
        reads, alns, alns_qc = {}, {}, {}
        read_flags = dict.fromkeys(flags, False)
        cur_qname = None

        # init counts
        for flag in flags:
            reads[flag], alns[flag], alns_qc[flag] = 0, 0, 0

        # iterate over each record
        # index for suffix removal, if suffix exist (> 0)
        if self.suflen:
            sufslice = slice(-self.suflen)
        else:
            sufslice = slice(None)
        for rec in pysam.Samfile(self.bamfile, 'rb'):
            # different qname mean we've finished parsing each unique read
            # so reset the flags and add them to the counters appropriately
            if cur_qname != rec.qname[sufslice]:
                for flag in flags:
                    reads[flag] += int(read_flags[flag])
                # reset the qname tracker
                cur_qname = rec.qname[sufslice]
                # and the read flag tracker
                read_flags = dict.fromkeys(flags, False)
            # total, total splice
            alns['total'] += 1
            alns_qc['total'] += func_qc(rec)
            read_flags['total'] = True
            if func_splice(rec):
                alns['totalSplice'] += 1
                alns_qc['totalSplice'] += func_qc(rec)
                read_flags['totalSplice'] = True
            # unmapped
            if func_nunmap(rec):
                alns['unmapped'] += 1
                alns_qc['unmapped'] += func_qc(rec)
                read_flags['unmapped'] = True
            else:
                # mapped
                if func_nmap(rec):
                    alns['mapped'] += 1
                    alns_qc['mapped'] += func_qc(rec)
                    read_flags['mapped'] = True
                    # mapped pairs
                    if func_nmap_pair(rec):
                        alns['mappedPair'] += 1
                        alns_qc['mappedPair'] += func_qc(rec)
                        read_flags['mappedPair'] = True
                        # proper pairs, proper pairs splice
                        if func_nmap_pair_ok(rec):
                            alns['mappedPairProper'] += 1
                            alns_qc['mappedPairProper'] += func_qc(rec)
                            read_flags['mappedPairProper'] = True
                            if func_splice(rec):
                                alns['splicePairProper'] += 1
                                alns_qc['splicePairProper'] += func_qc(rec)
                                read_flags['splicePairProper'] = True
                        # mapped to different chromosomes
                        elif func_nmap_diffchr(rec):
                            alns['mappedDiffChr'] += 1
                            alns_qc['mappedDiffChr'] += func_qc(rec)
                            read_flags['mappedDiffChr'] = True
                            if rec.mapq >= 5:
                                alns['mappedDiffChrQ'] += 1
                                alns_qc['mappedDiffChrQ'] += func_qc(rec)
                                read_flags['mappedDiffChrQ'] = True
                    # singletons, singletons splice
                    elif func_nmap_sgltn(rec):
                        alns['singleton'] += 1
                        alns_qc['singleton'] += func_qc(rec)
                        read_flags['singleton'] = True
                        if func_splice(rec):
                            alns['spliceSingleton'] += 1
                            alns_qc['spliceSingleton'] += func_qc(rec)
                            read_flags['spliceSingleton'] = True

        # for the last read, since we don't pass the qname check again
        for flag in flags:
            reads[flag] += int(read_flags[flag])

        self.aln_counts, self.read_counts = self._adjust_counts(alns, reads)
        self.aln_qc_counts = alns_qc
        if self.validate:
            assert self.validate_counts()

    def _count_unsorted(self):
        """Counts read and alignment statistics for non-ID-sorted BAM file."""
        flags = self.flags
        reads_total, reads_unmap, reads_map, reads_pair_map, reads_pair_proper, \
                reads_sgltn, reads_total_splice, reads_pair_proper_splice, \
                reads_sgltn_splice, reads_pair_diffchr, reads_pair_diffchr_h = \
                set(), set(), set(), set(), set(), set(), set(), set(), set(), \
                set(), set()

        reads, alns, alns_qc = {}, {}, {}
        for flag in flags:
            reads[flag], alns[flag], alns_qc[flag] = 0, 0, 0
        # index for suffix removal, if suffix exist (> 0)
        if self.suflen:
            sufslice = slice(-self.suflen)
        else:
            sufslice = slice(None)
        for rec in pysam.Samfile(self.bamfile, 'rb'):
            # remove '/1' or '/2' suffixes, to collapse read pair counts
            pass
            # do countings on alns and reads directly
            hname = hash(rec.qname[sufslice])
            # total, total splice
            alns['total'] += 1
            alns_qc['total'] += func_qc(rec)
            if hname not in reads_total:
                reads_total.add(hname)
            if func_splice(rec):
                alns['totalSplice'] += 1
                alns_qc['totalSplice'] += func_qc(rec)
                reads_total_splice.add(hname)
            # unmapped
            if func_nunmap(rec):
                alns['unmapped'] += 1
                alns_qc['unmapped'] += func_qc(rec)
                reads_unmap.add(hname)
            else:
                # mapped
                if func_nmap(rec):
                    alns['mapped'] += 1
                    alns_qc['mapped'] += func_qc(rec)
                    reads_map.add(hname)
                    # mapped pairs
                    if func_nmap_pair(rec):
                        alns['mappedPair'] += 1
                        alns_qc['mappedPair'] += func_qc(rec)
                        reads_pair_map.add(hname)
                        # proper pairs, proper pairs splice
                        if func_nmap_pair_ok(rec):
                            alns['mappedPairProper'] += 1
                            alns_qc['mappedPairProper'] += func_qc(rec)
                            reads_pair_proper.add(hname)
                            if func_splice(rec):
                                alns['splicePairProper'] += 1
                                alns_qc['splicePairProper'] += func_qc(rec)
                                reads_pair_proper_splice.add(hname)
                        # mapped to different chromosomes
                        elif func_nmap_diffchr(rec):
                            alns['mappedDiffChr'] += 1
                            alns_qc['mappedDiffChr'] += func_qc(rec)
                            reads_pair_diffchr.add(hname)
                            if rec.mapq >= 5:
                                alns['mappedDiffChrQ'] += 1
                                alns_qc['mappedDiffChrQ'] += func_qc(rec)
                                reads_pair_diffchr_h.add(hname)
                    # singletons, singletons splice
                    elif func_nmap_sgltn(rec):
                        alns['singleton'] += 1
                        alns_qc['singleton'] += func_qc(rec)
                        reads_sgltn.add(hname)
                        if func_splice(rec):
                            alns['spliceSingleton'] += 1
                            alns_qc['spliceSingleton'] += func_qc(rec)
                            reads_sgltn_splice.add(hname)

        # set counts for reads
        reads['total'] = len(reads_total)
        reads['totalSplice'] = len(reads_total_splice)
        reads['unmapped'] = len(reads_unmap)
        reads['mapped'] = len(reads_map)
        reads['mappedPair'] = len(reads_pair_map)
        reads['mappedPairProper'] = len(reads_pair_proper)
        reads['mappedDiffChr'] = len(reads_pair_diffchr)
        reads['mappedDiffChrQ'] = len(reads_pair_diffchr_h)
        reads['splicePairProper'] = len(reads_pair_proper_splice)
        reads['singleton'] = len(reads_sgltn)
        reads['spliceSingleton'] = len(reads_sgltn_splice)

        # free the memory
        del reads_total, reads_map, reads_pair_map, reads_pair_proper,  \
                reads_sgltn, reads_total_splice, reads_pair_proper_splice, \
                reads_sgltn_splice, reads_unmap, reads_pair_diffchr, \
                reads_pair_diffchr_h

        self.aln_counts, self.read_counts = self._adjust_counts(alns, reads)
        self.aln_qc_counts = alns_qc
        if self.validate:
            assert self.validate_counts()

    def validate_counts(self):
        """Checks whether all reads and alignment counts add up."""
        for ctype in ('read_counts', 'aln_counts'):
            count = getattr(self, ctype)
            ntotal = count['total']
            nmap = count['mapped']
            nunmap = count['unmapped']
            nmap_pair = count['mappedPair']
            nmap_pair_ok = count['mappedPairProper']
            nmap_pair_diffchr = count['mappedDiffChr']
            nmap_sgltn = count['singleton']
            nsplice_total = count['totalSplice']
            nsplice_pair_ok = count['splicePairProper']
            nsplice_sgltn = count['spliceSingleton']

            assert nmap == nmap_pair + nmap_sgltn, \
                    "Mismatch: %r == %r + %r" % (nmap, nmap_pair, nmap_sgltn)
            assert nmap_pair_ok + nmap_pair_diffchr <= nmap_pair
            assert nsplice_total <= ntotal
            assert nsplice_pair_ok <= nmap_pair_ok
            assert nsplice_sgltn <= nmap_sgltn
            # total is always == unmapped + mapped pair + singletons
            assert ntotal == nunmap + nmap_pair + nmap_sgltn, "Mismatch: " \
                    "%r == %r + %r + %r" % (ntotal, nunmap, nmap_pair,
                    nmap_sgltn)

        return True

    def show(self):
        """Prints the read and alignment counts in human-readable format to
        stdout."""
        import pprint
        pprint.pprint(dict(self.counts.items()))

    def write_json(self, out_file):
        """Writes JSON to the output file."""
        with open(out_file, 'w') as outfile:
            json.dump(self.counts, outfile, sort_keys=True, indent=4,
                    separators=(',', ': '))

    def _format_counts(self):
        """Formats read and alignment counts into nice-looking numbers."""
        counts = OrderedDict()
        flags = self.flags
        for ctype in ('read_counts', 'aln_counts', 'aln_qc_counts'):
            count = getattr(self, ctype)

            ntotal = count['total']
            cont = {} 
            lvl = 'aln' if ctype == 'aln_counts' else 'read'

            if ctype == 'aln_qc_counts':
                lvl = 'aln_qc'
            else:
                pct = lambda x: x * 100.0 / ntotal
                if ctype == 'read_counts':
                    lvl = 'read'
                elif ctype == 'aln_counts':
                    lvl = 'aln'

            for flag in flags:
                # format all counts
                cont[flag] = int_fmt(value=count[flag])
                # and add percentage values
                if lvl != 'aln_qc':
                    if flag == 'total':
                        cont['totalPct'] = '100'
                    else:
                        cont[flag + 'Pct'] = float_fmt(value=pct(count[flag]))

            counts[lvl] = cont

        self.counts = counts


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('bamfile', help='Path to BAM file')
    parser.add_argument('--id-sorted', action='store_true',
            dest='id_sorted', help='Whether the BAM file is ID-sorted or not')
    parser.add_argument('--suffix-len', type=int, dest='suffix_len', default=0,
            help='Length of read pair suffix, if present')
    parser.add_argument('-o', '--outfile', dest='out_file', type=str,
            help='Path to output file')
    parser.add_argument('-f', '--outfmt', dest='out_fmt', type=str,
            choices=['json'], default='json',
            help='Format of output file')
    args = parser.parse_args()

    bamstat = BarnStat(args.bamfile, args.suffix_len, args.id_sorted)

    if args.out_file is None:
        bamstat.show()
    elif args.out_fmt == 'json':
        bamstat.write_json(args.out_file)
