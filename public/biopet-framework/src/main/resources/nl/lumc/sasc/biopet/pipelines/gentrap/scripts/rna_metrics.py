#!/usr/bin/env python2
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


# rna_metrics.py
#
# Given a sorted, indexed BAM file from an RNA seq experiment,
# output the annotation metrics using Picard CollectRnaSeqMetrics per chromosome

import argparse
import json
import functools
import locale
import os
import subprocess
import threading
import time
import tempfile
import warnings
import Queue

# valid column names
# from http://picard.sourceforge.net/picard-metric-definitions.shtml#RnaSeqMetrics
COL_NAMES = {
    'PF_BASES': 'pfBases',
    'PF_ALIGNED_BASES': 'pfAlignedBases',
    'RIBOSOMAL_BASES': 'ribosomalBases',
    'CODING_BASES': 'codingBases',
    'UTR_BASES': 'utrBases',
    'INTRONIC_BASES': 'intronicBases',
    'INTERGENIC_BASES': 'intergenicBases',
    'IGNORED_READS': 'ignoredReads',
    'CORRECT_STRAND_READS': 'correctStrandReads',
    'INCORRECT_STRAND_READS': 'incorrectStrandReads',
    'PCT_RIBOSOMAL_BASES': 'pctRibosomalBases',
    'PCT_CODING_BASES': 'pctCodingBases',
    'PCT_UTR_BASES': 'pctUtrBases',
    'PCT_INTRONIC_BASES': 'pctIntronicBases',
    'PCT_INTERGENIC_BASES': 'pctIntergenicBases',
    'PCT_MRNA_BASES': 'pctMrnaBases',
    'PCT_USABLE_BASES': 'pctUsableBases',
    'PCT_CORRECT_STRAND_READS': 'pctCorrectStrandReads',
    'MEDIAN_CV_COVERAGE': 'medianCvCoverage',
    'MEDIAN_5PRIME_BIAS': 'median5PrimeBias',
    'MEDIAN_3PRIME_BIAS': 'median3PrimeBias',
    'MEDIAN_5PRIME_TO_3PRIME_BIAS': 'median5PrimeTo3PrimeBias',
}
# executables, default to ones in PATH
EXE_SAMTOOLS = 'samtools'
EXE_JAVA = 'java'

# set locale to group digits
locale.setlocale(locale.LC_ALL, '')
int_fmt = functools.partial(locale.format, grouping=True, percent='%i')
float_fmt = functools.partial(locale.format, grouping=True, percent='%.2f')


class MetricsTracker(object):

    """Class to track metrics file output."""

    def __init__(self, main_bams, chrs):
        self.lock = threading.Lock()
        self.chrs = chrs
        self.files = {}
        for rtype, bam in main_bams.items():
            self.files[bam] = {
                'chrs': dict.fromkeys(chrs),
                'strand': rtype,
            }

    def add_stat_file(self, main_bam, chr, chr_stat):
        self.lock.acquire()
        self.files[main_bam]['chrs'][chr] = chr_stat
        self.lock.release()

    def check_files(self):
        # only for strand-specific rna-seq
        for bam, data in self.files.items():
            for chr, path in data['chrs'].items():
                assert path is not None, "Missing statistics file for {0}, " \
                        "chromosome {1}".format(bam, chr)


class Worker(threading.Thread):

    """Class representing worker to execute jobs."""

    def __init__(self, queue, group=None, target=None, name=None, args=(),
            kwargs={}):
        threading.Thread.__init__(self, group, target, name, args, kwargs)
        self.queue = queue
        self.setDaemon(True)
        self.start()

    def run(self):
        while True:
            func, args, kwargs = self.queue.get()
            func(*args, **kwargs)
            self.queue.task_done()


class ThreadPool(object):

    """Class representing thread pool to execute."""

    def __init__(self, num_threads):
        self.queue = Queue.Queue()
        for _ in range(num_threads):
            Worker(self.queue)

    def add_task(self, func, *args, **kwargs):
        self.queue.put((func, args, kwargs))

    def wait_completion(self):
        self.queue.join()


def picard_metrics_worker(in_bam, chr, tracker, annot, jar,
        samtools_exe, java_exe):
    """Worker for collecting RNA-seq metrics."""
    # check if index exists
    assert os.path.exists(in_bam + '.bai')
    # create output directory, contains all chr stats
    out_dir = os.path.splitext(in_bam)[0] + '.rna_metrics'
    # create output directory
    try:
        os.makedirs(out_dir)
    except OSError:
        if not os.path.exists(out_dir):
            raise
    # if chr is none, do stat on all regions
    if chr is None:
        chr = 'ALL'
    out_stat = os.path.join(out_dir, chr + '.rna_metrics.txt')
    # split BAM file per chr, write to tmp file
    if chr != 'ALL':
        bam = tempfile.NamedTemporaryFile(prefix='tmp_rna_metrics_', delete=True)
        name = bam.name
        tokens = [samtools_exe, 'view', '-bh', '-o', bam.name, in_bam, chr]
        proc = subprocess.Popen(tokens, stdout=bam)
        while proc.poll() is None:
            time.sleep(1)
    else:
        name = in_bam
    picard_toks = [java_exe, '-jar', jar]
    for key, value in os.environ.items():
        if key.startswith('OPT_PICARD_COLLECTRNASEQMETRICS_'):
            # input, output, and annotation are handled separately
            if key.endswith('INPUT') or key.endswith('OUTPUT') or \
                key.endswith('REF_FLAT'):
                continue
            if value:
                picard_toks.append('%s=%s' %
                        (key.replace('OPT_PICARD_COLLECTRNASEQMETRICS_', ''), value))

    picard_toks += ['REF_FLAT={0}'.format(annot),
            'STRAND_SPECIFICITY=SECOND_READ_TRANSCRIPTION_STRAND',
            'I={0}'.format(name), 'O={0}'.format(out_stat)]
    picard = subprocess.Popen(picard_toks)
    while picard.poll() is None:
        time.sleep(1)
    assert os.path.exists(out_stat)
    if chr != 'ALL':
        bam.close()
    tracker.add_stat_file(in_bam, chr, out_stat)


def samtools_reads_per_region_count(bam_files, chrs, samtools_exe):
    """Counts read per chromosome using samtools (simple count of mapped read
    per region."""
    tokens = [samtools_exe, 'view', '-c', '-F', '0x4']
    keys = ['fwd', 'rev', 'mix']
    all_dict = dict.fromkeys(keys)
    for rtype, bam in bam_files.items():
        assert rtype in keys, "Unknown key: {0}".format(rtype)
        aggr_dict = {}
        for chr in chrs:
            if chr == 'ALL':
                continue
            proc = subprocess.Popen(tokens + [bam, chr], stdout=subprocess.PIPE)
            count = int(proc.stdout.read())
            aggr_dict[chr] = {
                'metrics': {'countMapped': count}    
            }
        name = os.path.basename(os.path.splitext(bam)[0])
        all_dict[rtype] = {}
        all_dict[rtype]['bamFile'] = name
        all_dict[rtype]['allMetrics'] = aggr_dict

    return all_dict


def picard_reads_per_region_count(bam_files, chrs, annot, jar, samtools_exe, java_exe):
    """Counts read per chromosome using Picard and annotation files."""
    assert os.path.exists(annot), "Annotation file {0} not found".format(annot)
    # only analyze sense and antisense reads
    bam_files = {'fwd': bam_files['fwd'], 'rev': bam_files['rev']}
    # create tracker for metric files
    metrics_tracker = MetricsTracker(bam_files, chrs)
    # create main task pool
    metrics_pool = ThreadPool(args.threads)
    # add tasks to the pool
    for bam in bam_files.values():
        for chr in chrs:
            metrics_pool.add_task(picard_metrics_worker, in_bam=bam, chr=chr,
                    tracker=metrics_tracker, annot=annot, jar=jar,
                    samtools_exe=samtools_exe, java_exe=java_exe)
    metrics_pool.wait_completion()
    # checks whether all required stat files are present
    metrics_tracker.check_files()
    return aggregate_metrics(metrics_tracker, chrs)


def prep_bam_file(bams, strand_spec, samtools_exe):
    """Index input BAM files and return a dictionary of BAM files to process."""
    for in_bam in in_bams.values():
        bam = os.path.abspath(in_bam)
        assert os.path.exists(bam), "File {0} does not exist".format(in_bam)
        if not os.path.exists(bam + '.bai'):
            subprocess.call([samtools_exe, 'index', bam])
    return bams


def parse_metrics_file(metrics_path):
    """Given a path to a Picard CollectRnaSeqMetrics output file, return a
    dictionary consisting of its column, value mappings.
    """
    data_mark = 'PF_BASES'
    tokens = []
    with open(metrics_path, 'r') as source:
        line = source.readline().strip()
        fsize = os.fstat(source.fileno()).st_size
        while True:
            if not line.startswith(data_mark):
                # encountering EOF before metrics is an error
                if source.tell() == fsize:
                    raise ValueError("Metrics not found inside %r" % \
                            metrics_path)
                line = source.readline().strip()
            else:
                break

        assert line.startswith(data_mark)
        # split header line and append to tokens
        tokens.append(line.split('\t'))
        # and the values (one row after)
        tokens.append(source.readline().strip().split('\t'))
    data = {}
    for col, value in zip(tokens[0], tokens[1]):
        if not value:
            data[COL_NAMES[col]] = None
        elif col.startswith('PCT') or col.startswith('MEDIAN'):
            if value != '?':
                data[COL_NAMES[col]] = float(value)
            else:
                warnings.warn("Undefined value for %s in %s: %s" % (col,
                    metrics_path, value))
                data[COL_NAMES[col]] = None
        else:
            assert col in COL_NAMES, 'Unknown column: %s' % col
            data[COL_NAMES[col]] = int(value)

    return data


def write_json(out_file, data, **kwargs):
    with open(out_file, 'w') as jsonfile:
        json.dump(data, jsonfile, sort_keys=True, indent=4,
                separators=(',', ': '))


def write_html(out_file, data, chrs, is_strand_spec):
    if is_strand_spec:
        table_func = build_table_html_ss
        tpl = open(prog_path('rna_metrics.html')).read()
    else:
        table_func = build_table_html_nonss
        tpl = open(prog_path('rna_metrics_nonss.html')).read()

    html_data = table_func(data, chrs)
    with open(out_file, 'w') as htmlfile:
        htmlfile.write(tpl.format(**html_data))


def get_base_counts(metrics):
    res = {
        'total': metrics['pfAlignedBases'],
        'exonic': metrics['utrBases'] + metrics['codingBases'],
        'intronic': metrics['intronicBases'],
        'intergenic': metrics['intergenicBases'],
    }
    # count percentages
    for reg in ('exonic', 'intronic', 'intergenic'):
        res[reg + '_pct'] = res[reg] * 100.0 / res['total']
    # format for display
    for key, value in res.items():
        if key.endswith('_pct'):
            res[key] = float_fmt(value=value)
        else:
            res[key] = int_fmt(value=value)

    return res


def build_table_html_nonss(data, chrs):
    assert data['mix'] is not None and (data['fwd'] is None and data['rev'] is
            None), "Invalid data %r" % data
    mix = data['mix']['allMetrics']
    read_table = [
        '<table>',
        '<tr>',
        '<th>Chromosome</th>',
        '<th>Reads</th>',
        '</tr>'
    ]
    rrow_tpl = '<tr><td>{0}</td><td>{1}</td>'
    for chr in chrs:
        # not showing all stats in table, per chr only
        if chr == 'ALL':
            continue
        count = int_fmt(value=mix[chr]['metrics']['countMapped'])
        read_table.append(rrow_tpl.format(chr, count))
    read_table.append('</table>')

    return {'table_read_count': '\n'.join(read_table),
            'css': open(prog_path('rna_metrics.css')).read()}


def build_table_html_ss(data, chrs):
    read_table = [
        '<table>',
        '<tr>',
        '<th rowspan="2">Chromosome</th>',
        '<th>Mapped</th>',
        '<th>Sense Annotation Only</th>',
        '<th>Antisense Annotation Only</th>',
        '</tr>'
        '<tr>',
        '<th>Both strands</th>',
        '<th><green>Sense</green> / <red>Antisense</red> Reads</th>',
        '<th><green>Antisense</green> / <red>Sense</red> Reads</th>',
        '</tr>'
    ]
    rrow_tpl = '<tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td></tr>'
    rcell_tpl = '<green>{0}</green> / <red>{1}</red>'

    fwd_data, rev_data = data['fwd']['allMetrics'], data['rev']['allMetrics']
    mix_data = data['mix']['allMetrics']
    for chr in chrs:
        # not showing all stats in table, per chr only
        if chr == 'ALL':
            continue
        cur_fwd = fwd_data[chr]['metrics']
        cur_rev = rev_data[chr]['metrics']
        mix_count = mix_data[chr]['metrics']['countMapped']
        sense = rcell_tpl.format(
                int_fmt(value=cur_fwd['correctStrandReads']),
                int_fmt(value=cur_fwd['incorrectStrandReads']))
        antisense = rcell_tpl.format(
                int_fmt(value=cur_rev['correctStrandReads']),
                int_fmt(value=cur_rev['incorrectStrandReads']))
        read_table.append(rrow_tpl.format(chr, int_fmt(value=mix_count),
            sense, antisense))
    read_table.append('</table>')

    base_table = [
        '<table>',
        '<tr>',
        '<th rowspan="2">Region</th>',
        '<th colspan="2">Sense Annotation</th>',
        '<th colspan="2">Antisense Annotation</th>',
        '</tr>'
        '<tr>',
        '<th>Count</th>', '<th>%</th>', 
        '<th>Count</th>', '<th>%</th>', 
        '</tr>'
    ]
    crow_tpl = [
        '<tr>',
        '<td>{0}</td>', '<td>{1}</td>',
        '<td>{2}</td>', '<td>{3}</td>',
        '<td>{4}</td>',
        '</tr>',
    ]
    crow_tpl = ''.join(crow_tpl)
    fwd_bcounts = get_base_counts(fwd_data['ALL']['metrics'])
    rev_bcounts = get_base_counts(rev_data['ALL']['metrics'])
    for reg in ('exonic', 'intronic', 'intergenic'):
        pct = reg + '_pct'
        base_table.append(
            crow_tpl.format(reg.capitalize(), fwd_bcounts[reg],
                fwd_bcounts[pct], rev_bcounts[reg],
                rev_bcounts[pct]))
    base_table.append('</table>')

    return {'table_read_count': '\n'.join(read_table),
            'table_base_count': '\n'.join(base_table),
            'css': open(prog_path('rna_metrics.css')).read()}


def aggregate_metrics(tracker, chrs):
    """Aggregates all RNA seq metrics data into a single file."""
    all_dict = {}
    keys = ['fwd', 'rev', 'mix']
    all_dict = dict.fromkeys(keys)
    for bam, stats in tracker.files.items():
        assert stats['strand'] in keys
        aggr_dict = {}
        for chr, source in stats['chrs'].items():
            aggr_dict[chr] = {
                'fileName': os.path.basename(source),
                'metrics': parse_metrics_file(source),
            }
        name = os.path.basename(os.path.splitext(bam)[0])
        all_dict[stats['strand']] = {}
        all_dict[stats['strand']]['bamFile'] = name
        all_dict[stats['strand']]['allMetrics'] = aggr_dict

    return all_dict


def prog_path(fname):
    prog_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(prog_dir, fname)


if __name__ == '__main__':
    # params:
    # main bam file
    # thread num
    # option to compare with annotation source (produces correct/incorrect counts)
    # strand specific or not strand specific
    # path to picard's collectrnaseqmetrics
    # json output path
    # samtools binary (default: environment samtools)
    # java binary (default: environment java)
    # picard options: 
    parser = argparse.ArgumentParser()

    parser.add_argument('m_bam', type=str,
            help='Path to BAM file containing both sense and antisense reads')
    parser.add_argument('--s-bam', dest='s_bam', type=str,
            help='Path to BAM file containing sense reads')
    parser.add_argument('--as-bam', dest='as_bam', type=str,
            help='Path to BAM file containing antisense reads')
    parser.add_argument('-o', '--outfile', dest='out_file', type=str,
            help='Path to output file')
    parser.add_argument('-t', '--threads', dest='threads',
            default=1, type=int, help='Number of threads to use')
    parser.add_argument('--chrs', dest='chrs',
            default=prog_path('chrs.txt'),
            help='Path to file containing chromosome names')
    parser.add_argument('-a', '--annotation', dest='annot',
            help='Annotation source')
    parser.add_argument('--html', dest='is_html',
            action='store_true',
            help='Output HTML file')
    parser.add_argument('--jar', dest='jar', type=str,
            help='Path to Picard\'s CollectRnaSeqMetrics.jar')
    parser.add_argument('--java', dest='java', type=str,
            default=EXE_JAVA,
            help='Path to java executable')
    parser.add_argument('--samtools', dest='samtools', type=str,
            default=EXE_SAMTOOLS,
            help='Path to samtools executable')

    args = parser.parse_args()

    if args.s_bam is not None and args.as_bam is not None:
        is_strand_spec = True
        in_bams = {'mix': args.m_bam, 'fwd': args.s_bam, 'rev': args.as_bam}
    elif args.s_bam is None and args.as_bam is None:
        is_strand_spec = False
        in_bams = {'mix': args.m_bam}
    else:
        raise ValueError("Incomplete argument: either sense or antisense BAM "
                "files are not specified.")

    chrs = [line.strip() for line in open(args.chrs, 'r')] + ['ALL']
    # check for paths and indices
    bam_files = prep_bam_file(in_bams, is_strand_spec, args.samtools)
    # use picard and samtools if it's strand-specific
    if is_strand_spec:
        aggr_data = picard_reads_per_region_count(bam_files, chrs, args.annot,
                args.jar, args.samtools, args.java)
        sam_data = samtools_reads_per_region_count(bam_files, chrs,
                args.samtools)
        aggr_data['mix'] = sam_data['mix']
    # otherwise use samtools only
    else:
        aggr_data = samtools_reads_per_region_count(bam_files, chrs,
                args.samtools)

    # write to output file
    if args.out_file is None:
        ext = '.html' if args.is_html else '.json'
        out_file = 'rna_metrics_out' + ext
    else:
        out_file = args.out_file

    write_func = write_html if args.is_html else write_json
    write_func(out_file, aggr_data, chrs=chrs, is_strand_spec=is_strand_spec)
