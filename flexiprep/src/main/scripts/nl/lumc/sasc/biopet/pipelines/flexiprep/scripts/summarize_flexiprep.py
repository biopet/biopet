#!/usr/bin/env python

"""
Summarize results from a Flexiprep run into a gzipped JSON file.


Requirements:
    * Python == 2.7.x

Copyright (c) 2013 Wibowo Arindrarto <w.arindrarto@lumc.nl>
MIT License <http://opensource.org/licenses/MIT>
"""

RELEASE = False
__version_info__ = ('0', '2', )
__version__ = '.'.join(__version_info__)
__version__ += '-dev' if not RELEASE else ''


import argparse
import contextlib
import json
import os
import re
from datetime import datetime


def fastqc2dict(run_dir, sample, name, data_fname='fastqc_data.txt',
        image_dname='Images'):
    """Summarize FastQC results into a single dictionary object."""
    dirname = os.path.join(run_dir, sample)
    fqc_core_dir = os.path.join(dirname, os.walk(dirname).next()[1][0])
    fqc_dataf, fqc_imaged = None, None
    for p1, p2, p3 in os.walk(fqc_core_dir):
        if image_dname in p2:
            fqc_imaged = os.path.join(p1, image_dname)
        if data_fname in p3:
            fqc_dataf = os.path.join(p1, data_fname)
        if fqc_dataf is not None and fqc_imaged is not None:
            break
    if fqc_imaged is None or fqc_dataf is None:
        raise ValueError("Could not find data directory "
                "and/or image directory in FastQC result.")

    d = {
        'files': {
            'txt_fastqc_' + name: { 
                'checksum_sha1': None,
                'path': fqc_dataf
            },
        },
        'stats': {},
    }

    for img in [os.path.join(fqc_imaged, f) for f in os.listdir(fqc_imaged)]:
        key = os.path.splitext(os.path.basename(img))[0]
        d['files']['plot_' + key + '_' + name] = {
            'path': os.path.abspath(img),
            'checksum_sha1': None,
        }

    return d


def clip2dict(sample, samplea, sampleb, lib_type, run_dir):

    re_aff = re.compile(r'\s*Trimmed reads:\s*(\d+)')
    re_short = re.compile(r'\s*Too short reads:\s*(\d+)')
    re_long = re.compile(r'\s*Too long reads:\s*(\d+)')
    re_disc_1 = re.compile(r'Filtered (\d+) .+ first')
    re_disc_2 = re.compile(r'Filtered (\d+) reads from second')
    re_kept = re.compile(r'Synced read .+ (\d+) reads.')

    @contextlib.contextmanager
    def open_result(*toks):
        with open(os.path.join(run_dir, *toks)) as src:
                yield src

    def get_re_adp(seq):
        return re.compile("Adapter.+'{0}'.+trimmed (\d+) times.".format(seq))

    def get_adapters(read_pair):
        adapters = {}
        with open_result(read_pair + '.contams.txt') as src:
            for line in src:
                name, seq = line.strip().split('\t')
                # None is placeholder for trimming count
                adapters[name] = [seq, None]

        return adapters

    def get_cutadapt_stats(read_pair, adapter_dict):
        stats = {}
        stat_file = read_pair + '.clip.stats'

        try:
            with open_result(stat_file) as src:
                cutadapt_txt = src.read()
        except IOError:
            # file does not exist, no adapter contamination found
            stats['num_reads_discarded'] = 0
            stats['num_reads_affected'] = 0
            return stats

        short_count = int(re.search(re_short, cutadapt_txt).group(1))
        long_count = int(re.search(re_long, cutadapt_txt).group(1))
        stats['num_reads_discarded'] = short_count + long_count
        stats['num_reads_affected'] = int(re.search(re_aff,
            cutadapt_txt).group(1))
        for key in adapter_dict:
            re_adp = get_re_adp(adapter_dict[key][0])
            adapter_dict[key][1] = int(re.search(re_adp,
                cutadapt_txt).group(1))

        return stats

    def get_sync_stats(sample):
        stats = {}
        with open_result(sample + '.clip.sync.stats') as src:
            sync_txt = src.read()

        stats['num_reads_discarded_1'] = int(re.search(re_disc_1, sync_txt).group(1))
        stats['num_reads_discarded_2'] = int(re.search(re_disc_2, sync_txt).group(1))
        stats['num_reads_kept'] = int(re.search(re_kept, sync_txt).group(1))
        stats['num_reads_discarded_total'] = stats['num_reads_discarded_1'] + \
                stats['num_reads_discarded_2']

        return stats

    clip_stats = {'fastq_1': {}}
    clip_stats['fastq_1']['adapters'] = get_adapters(samplea)
    clip_stats['fastq_1'].update(
            get_cutadapt_stats(samplea,
                    clip_stats['fastq_1']['adapters']))

    if lib_type == 'paired':
        clip_stats['fastq_2'] = {}
        clip_stats['fastq_2']['adapters'] = get_adapters(sampleb)
        clip_stats['fastq_2'].update(
                get_cutadapt_stats(sampleb,
                        clip_stats['fastq_2']['adapters']))
        clip_stats['sync'] = get_sync_stats(sample)
    else:
        clip_stats['sync'] = {}

    return clip_stats


def sickle2dict(run_name, qc_mode, lib_type, run_dir):

    trim_stats = {}
    if qc_mode == 'trim':
        stat_mark = '.trim.stats'
    else:
        if lib_type == 'paired':
            stat_mark = '.clip.sync.trim.stats'
        else:
            stat_mark = '.clip.trim.stats'

    if lib_type == 'paired':
        re_paired_kept = re.compile(r'paired records kept: \d+ \((\d+) pairs\)')
        re_disc = re.compile(r'single records discarded: \d+ \(from PE1: (\d+), from PE2: (\d+)\)')
        re_disc_paired = re.compile(r'paired records discarded: \d+ \((\d+) pairs\)')
        with open(os.path.join(run_dir, run_name + stat_mark)) as src:
            sickle_txt = src.read()

        discarda = int(re.search(re_disc, sickle_txt).group(1))
        discardb = int(re.search(re_disc, sickle_txt).group(2))
        discard_both = int(re.search(re_disc_paired, sickle_txt).group(1)) 

        trim_stats['num_reads_kept'] = int(re.search(re_paired_kept, sickle_txt).group(1))
        trim_stats['num_reads_discarded_1'] = discarda
        trim_stats['num_reads_discarded_2'] = discardb
        trim_stats['num_reads_discarded_both'] = discard_both
        trim_stats['num_reads_discarded_total'] = discarda + discardb + discard_both

    else:
        re_kept = re.compile(r'records kept: (\d+)')
        re_disc = re.compile(r'records discarded: (\d+)')
        with open(os.path.join(run_dir, run_name + stat_mark)) as src:
            sickle_txt = src.read()

        trim_stats['num_reads_kept'] = int(re.search(re_kept, sickle_txt).group(1))
        trim_stats['num_reads_discarded_total'] = int(re.search(re_disc, sickle_txt).group(1))

    return trim_stats


def dict2json(d_input, f_out):
    """Dump the given dictionary as a JSON file."""
    with open(f_out, 'w') as target:
        json.dump(d_input, target, sort_keys=True, indent=4,
                separators=(',', ': '))


def summarize_flexiprep(run_name, qc_mode, samplea, sampleb, outf, run_dir):

    def load_json(fname):
        with open(os.path.join(run_dir, fname), 'r') as target:
            return json.load(target)

    def load_chksum(fname):
        with open(os.path.join(run_dir, fname), 'r') as src:
            return src.readline().strip().split()

    def gc_from_fastqc(fname):
        # other quick statistics
        with open(fname, 'r') as src:
            return int([x for x in src if
                x.startswith('%GC')].pop().split('\t')[1])


    sumd = {
        '_meta': {
            'module': 'flexiprep',
            'run_name': run_name,
            'run_time': datetime.utcnow().isoformat(),
        },
        'stats': {
            'qc_mode': qc_mode,
        },
        'dirs': {
            'run': run_dir,
        },
        'files': {
        },
    }

    if sampleb is None:
        lib_type = 'single'
    else:
        lib_type = 'paired'
    sumd['stats']['lib_type'] = lib_type

    # gather checksum files
    chksums = [c for c in os.listdir(run_dir) if c.endswith('.sha1')]
    # gather fastqc directories
    fastqcs = [d for d in os.listdir(run_dir) if d.endswith('.fastqc')]
    # gather seqstat files
    sstats = [s for s in os.listdir(run_dir) if s.endswith('.seqstats.json')]

    if lib_type == 'paired':
        if qc_mode == 'clip':
            stat_mark = '.clip.sync'
        elif qc_mode == 'trim':
            stat_mark = '.trim'
        elif qc_mode == 'cliptrim':
            stat_mark = '.clip.sync.trim'
        else:
            assert qc_mode == 'none'
    else:
        if qc_mode == 'clip':
            stat_mark = '.clip'
        elif qc_mode == 'trim':
            stat_mark == '.trim'
        elif qc_mode == 'cliptrim':
            stat_mark == '.clip.trim'
        else:
            assert qc_mode == 'none'

    if lib_type == 'single':
        if qc_mode == 'none':
            assert len(chksums) == len(fastqcs) == len(sstats) == 1
            chksum_r1 = load_chksum(chksums[0])
            sumd['files']['fastq_raw_1'] = {
                    'checksum_sha1': chksum_r1[0],
                    'path': os.path.join(args.run_dir,
                        os.path.basename(chksum_r1[1]))}
            fqd_r1 = fastqc2dict(args.run_dir, fastqcs[0], 'raw_1')
            seqstat_r1 = load_json(sstats[0])['stats']
            seqstat_r1['mean_gc'] = gc_from_fastqc(
                    fqd_r1['files']['txt_fastqc_raw_1']['path'])
            sumd['stats']['fastq_raw_1'] = seqstat_r1

            for k in ('files', 'stats'):
                sumd[k].update(fqd_r1[k])
        else:
            assert len(chksums) == len(fastqcs) == len(sstats) == 2
            fqp1_name = [x for x in fastqcs if x.endswith('.qc.fastqc')][0]

            chksum_p1 = load_chksum([x for x in chksums if
                    x.endswith('.qc.sha1')][0])
            sumd['files']['fastq_proc_1'] = {
                    'checksum_sha1': chksum_p1[0],
                    'path': os.path.join(args.run_dir,
                        os.path.basename(chksum_p1[1]))}
            fqd_p1 = fastqc2dict(args.run_dir,
                    [x for x in fastqcs if x.endswith('.qc.fastqc')][0],
                    'proc_1')
            seqstat_p1 = load_json([x for x in sstats if
                x.endswith(stat_mark + '.seqstats.json')][0])['stats']
            seqstat_p1['mean_gc'] = gc_from_fastqc(
                    fqd_p1['files']['txt_fastqc_proc_1']['path'])
            sumd['stats']['fastq_proc_1'] = seqstat_p1

            chksum_r1 = load_chksum([x for x in chksums if not
                    x.endswith('.qc.sha1')][0])
            sumd['files']['fastq_raw_1'] = {
                    'checksum_sha1': chksum_r1[0],
                    'path': os.path.join(args.run_dir,
                        os.path.basename(chksum_r1[1]))}
            fqd_r1 = fastqc2dict(args.run_dir,
                    [x for x in fastqcs if x != fqp1_name][0],
                    'raw_1')
            seqstat_r1 = load_json([x for x in sstats if not
                    x.endswith(stat_mark + '.seqstats.json')][0])['stats']
            seqstat_r1['mean_gc'] = gc_from_fastqc(
                    fqd_r1['files']['txt_fastqc_raw_1']['path'])
            sumd['stats']['fastq_raw_1'] = seqstat_r1

            for k in ('files', 'stats'):
                sumd[k].update(fqd_p1[k])
                sumd[k].update(fqd_r1[k])
    else:
        if qc_mode == 'none':
            assert len(chksums) == len(fastqcs) == len(sstats) == 2

            chksum_r1 = load_chksum([x for x in chksums if
                x.startswith(samplea)][0])
            sumd['files']['fastq_raw_1'] = {
                'checksum_sha1': chksum_r1[0],
                'path': os.path.join(args.run_dir,
                    os.path.basename(chksum_r1[1]))}
            fqd_r1 = fastqc2dict(args.run_dir,
                    [x for x in fastqcs if x.startswith(samplea)][0],
                    'raw_1')
            seqstat_r1 = load_json([x for x in sstats if
                x.startswith(samplea)][0])['stats']
            seqstat_r1['mean_gc'] = gc_from_fastqc(
                    fqd_r1['files']['txt_fastqc_raw_1']['path'])
            sumd['stats']['fastq_raw_1'] = seqstat_r1

            chksum_r2 = load_chksum([x for x in chksums if
                not x.startswith(samplea)][0])
            sumd['files']['fastq_raw_2'] = {
                    'checksum_sha1': chksum_r2[0],
                    'path': os.path.join(args.run_dir,
                        os.path.basename(chksum_r2[1]))}
            fqd_r2 = fastqc2dict(args.run_dir,
                    [x for x in fastqcs if not x.startswith(samplea)][0],
                    'raw_2')
            seqstat_r2 = load_json([x for x in sstats if
                not x.startswith(samplea)][0])['stats']
            seqstat_r2['mean_gc'] = gc_from_fastqc(
                    fqd_r2['files']['txt_fastqc_raw_2']['path'])
            sumd['stats']['fastq_raw_2'] = seqstat_r2

            for k in ('files', 'stats'):
                sumd[k].update(fqd_r1[k])
                sumd[k].update(fqd_r2[k])
        else:
            assert len(fastqcs) == len(sstats) == 4
            proc_chksums = [x for x in chksums if x.endswith('.qc.sha1')]
            proc_fastqcs = [x for x in fastqcs if x.endswith('.qc.fastqc')]
            proc_sstats = [x for x in sstats if x.endswith(stat_mark + '.seqstats.json')]
            raw_chksums = [x for x in chksums if x not in proc_chksums]
            raw_fastqcs = [x for x in fastqcs if x not in proc_fastqcs]
            raw_sstats = [x for x in sstats if x not in proc_sstats]

            chksum_r1 = load_chksum([x for x in raw_chksums if
                x.startswith(samplea)][0])
            sumd['files']['fastq_raw_1'] = {
                'checksum_sha1': chksum_r1[0],
                'path': os.path.join(args.run_dir,
                    os.path.basename(chksum_r1[1]))}
            fqd_r1 = fastqc2dict(args.run_dir,
                [x for x in raw_fastqcs if x.startswith(samplea)][0],
                'raw_1')
            seqstat_r1 = load_json([x for x in raw_sstats if
                x.startswith(samplea)][0])['stats']
            seqstat_r1['mean_gc'] = gc_from_fastqc(
                    fqd_r1['files']['txt_fastqc_raw_1']['path'])
            sumd['stats']['fastq_raw_1'] = seqstat_r1

            chksum_r2 = load_chksum([x for x in raw_chksums if
                not x.startswith(samplea)][0])
            sumd['files']['fastq_raw_2'] = {
                    'checksum_sha1': chksum_r2[0],
                    'path': os.path.join(args.run_dir,
                        os.path.basename(chksum_r2[1]))}
            fqd_r2 = fastqc2dict(args.run_dir,
                    [x for x in raw_fastqcs if not x.startswith(samplea)][0],
                    'raw_2')
            seqstat_r2 = load_json([x for x in raw_sstats if
                not x.startswith(samplea)][0])['stats']
            seqstat_r2['mean_gc'] = gc_from_fastqc(
                    fqd_r2['files']['txt_fastqc_raw_2']['path'])
            sumd['stats']['fastq_raw_2'] = seqstat_r2

            chksum_p1 = load_chksum([x for x in proc_chksums if
                x.startswith(samplea)][0])
            sumd['files']['fastq_proc_1'] = {
                'checksum_sha1': chksum_p1[0],
                'path': os.path.join(args.run_dir,
                    os.path.basename(chksum_p1[1]))}
            fqd_p1 = fastqc2dict(args.run_dir,
                [x for x in proc_fastqcs if x.startswith(samplea)][0],
                'proc_1')
            seqstat_p1 = load_json([x for x in proc_sstats if
                x.startswith(samplea)][0])['stats']
            seqstat_p1['mean_gc'] = gc_from_fastqc(
                    fqd_p1['files']['txt_fastqc_proc_1']['path'])
            sumd['stats']['fastq_proc_1'] = seqstat_p1

            chksum_p2 = load_chksum([x for x in proc_chksums if
                not x.startswith(samplea)][0])
            sumd['files']['fastq_proc_2'] = {
                'checksum_sha1': chksum_p2[0],
                'path': os.path.join(args.run_dir,
                    os.path.basename(chksum_p2[1]))}
            fqd_p2 = fastqc2dict(args.run_dir,
                [x for x in proc_fastqcs if  not x.startswith(samplea)][0],
                'proc_2')
            seqstat_p2 = load_json([x for x in proc_sstats if
                not x.startswith(samplea)][0])['stats']
            seqstat_p2['mean_gc'] = gc_from_fastqc(
                    fqd_p2['files']['txt_fastqc_proc_2']['path'])
            sumd['stats']['fastq_proc_2'] = seqstat_p2

            for k in ('files', 'stats'):
                for fqd in (fqd_r1, fqd_r2, fqd_p1, fqd_p2):
                    sumd[k].update(fqd[k])

    if 'clip' in qc_mode:
        sumd['stats']['clip'] = clip2dict(run_name, samplea, sampleb, lib_type, run_dir)

    if 'trim' in qc_mode:
        sumd['stats']['trim'] = sickle2dict(run_name, qc_mode, lib_type, run_dir)

    dict2json(sumd, outf)


if __name__ == '__main__':

    usage = __doc__.split('\n\n\n')
    parser = argparse.ArgumentParser(
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description=usage[0], epilog=usage[1])

    parser.add_argument('run_name', type=str, help='FastQC run name')
    parser.add_argument('qc_mode', type=str, choices=['clip', 'cliptrim',
        'trim', 'none'], help='Flexiprep QC mode')
    parser.add_argument('samplea', type=str, help='Sample A name.')
    parser.add_argument('--sampleb', type=str, help='Sample B name.')
    parser.add_argument('out_file', type=str, help='Path to output file')
    parser.add_argument('--run-dir', type=str, default=os.getcwd(),
            help='Path to Flexiprep output directory.')
    parser.add_argument('--version', action='version', version='%(prog)s ' +
            __version__)

    args = parser.parse_args()

    summarize_flexiprep(args.run_name, args.qc_mode,
            args.samplea, args.sampleb, args.out_file, args.run_dir)
