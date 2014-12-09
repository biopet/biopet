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


# pdf_report.py

# TeX template generation script for Gentrap pipeline.


import argparse
import json
import locale
import os
import re
from collections import namedtuple
from datetime import datetime
from os.path import join

from jinja2 import Environment, FileSystemLoader

from rig import RigRun, __version__

# base pdflatex jinja template name
_BASE_TEMPLATE = 'base.tex'
# dictionary for storing proper names of tools
_TOOL_NAMES = {
    'fastqc': 'FastQC',
    'cutadapt': 'cutadapt',
    'sickle': 'sickle',
    'gsnap': 'GSNAP',
    'tophat': 'TopHat',
    'star': 'STAR',
    'gemtools': 'gemtools',
    'samtools': 'SAMtools',
    'picard': 'Picard',
    'cufflinks': 'Cufflinks',
    'cuffcompare': 'Cuffcompare',
    'bedtools': 'BEDtools',
    'r': 'R',
    'htseq': 'HTSeq',
}

# set locale for digit grouping
locale.setlocale(locale.LC_ALL, '')

# HACK: remove this and use jinja2 only for templating
class LongTable(object):

    """Class representing a longtable in LaTeX."""

    def __init__(self, caption, label, header, aln, colnum):
        self.lines = [
            '\\begin{center}',
            '\\captionof{table}{%s}' % caption,
            '\\label{%s}' % label,
            '\\begin{longtable}{%s}' % aln,
            '\\hline',
            '%s' % header,
            '\\hline \\hline',
            '\\endhead',
            '\\hline \\multicolumn{%i}{c}{\\textit{Continued on next page}}\\\\' % \
                    colnum,
            '\\hline',
            '\\endfoot',
            '\\hline',
            '\\endlastfoot',
        ]

    def __str__(self):
        return '\n'.join(self.lines)

    def add_row(self, row):
        self.lines.append(row)

    def end(self):
        self.lines.extend(['\\end{longtable}', '\\end{center}',
            '\\addtocounter{table}{-1}'])

# filter functions for the jinja environment
def nice_int(num):
    return locale.format('%i', int(num), grouping=True)

def nice_flt(num):
    return locale.format('%.2f', float(num), grouping=True)

def trans_tables(cuffcmp, strand, atype):

    if strand == 'fwd':
        sname = 'Sense'
    elif strand == 'rev':
        sname = 'Antisense'
    elif strand == 'mix':
        sname = 'Mix'
    else:
        raise ValueError("Unexpected strand type: %r" % strand)

    if atype == 'strict':
        aname = 'Strict'
    elif atype == 'guided':
        aname = 'Guided'
    elif atype == 'blind':
        aname = 'Blind'
    else:
        raise ValueError("Unexpected transcript assembly mode: %r" % atype)

    snsp_table = LongTable(
        caption='Transcript Assembly Summary, %s Strand, %s Mode' % \
                (sname, aname),
        label='tab:transum%s%s' % (sname.lower(), atype.lower()),
        header='Level & Sn & Sp & fSn & fSp\\\\',
        aln = ' l r r r r ',
        colnum=5)

    snsp_table.add_row('Base & %.2f & %.2f & n/a & n/a\\\\' % (
            cuffcmp['baseLevelSn'], cuffcmp['baseLevelSp']))
    snsp_table.add_row('Exon & %.2f & %.2f & %.2f & %.2f\\\\' % (
            cuffcmp['exonLevelSn'], cuffcmp['exonLevelSp'],
            cuffcmp['exonLevelFSn'], cuffcmp['exonLevelFSp']))
    snsp_table.add_row('Intron & %.2f & %.2f & %.2f & %.2f\\\\' % (
            cuffcmp['intronLevelSn'], cuffcmp['intronLevelSp'],
            cuffcmp['intronLevelFSn'], cuffcmp['intronLevelFSp']))
    snsp_table.add_row('Intron chain & %.2f & %.2f & %.2f & %.2f\\\\' % (
            cuffcmp['intronChainLevelSn'], cuffcmp['intronChainLevelSp'],
            cuffcmp['intronChainLevelFSn'], cuffcmp['intronChainLevelFSp']))
    snsp_table.add_row('Transcript & %.2f & %.2f & %.2f & %.2f\\\\' % (
            cuffcmp['transcriptLevelSn'], cuffcmp['transcriptLevelSp'],
            cuffcmp['transcriptLevelFSn'], cuffcmp['transcriptLevelFSp']))
    snsp_table.add_row('Locus & %.2f & %.2f & %.2f & %.2f\\\\' % (
            cuffcmp['locusLevelSn'], cuffcmp['locusLevelSp'],
            cuffcmp['locusLevelFSn'], cuffcmp['locusLevelFSp']))

    snsp_table.end()

    pa_table = LongTable(
        caption='Transcript Assembly Counts, %s Strand, %s Mode' % \
                (sname, aname),
        label='tab:transcount%s%s' % (sname.lower(), atype.replace('.',
            '').lower()),
        header='Type \& Level & Not Present in Counterpart & Total\\\\',
        aln = ' l r r ',
        colnum=3)

    pa_table.add_row('Assembled exons & %s & %s \\\\' % (
            nice_int(cuffcmp['queryExonsNotInRef']),
            nice_int(cuffcmp['queryExonsTotal'])))
    pa_table.add_row('Reference exons & %s & %s \\\\' % (
            nice_int(cuffcmp['refExonsNotInQuery']),
            nice_int(cuffcmp['refExonsTotal'])))
    pa_table.add_row('Assembled introns & %s & %s \\\\' % (
            nice_int(cuffcmp['queryIntronsNotInRef']),
            nice_int(cuffcmp['queryIntronsTotal'])))
    pa_table.add_row('Reference introns & %s & %s \\\\' % (
            nice_int(cuffcmp['refIntronsNotInQuery']),
            nice_int(cuffcmp['refIntronsTotal'])))
    pa_table.add_row('Assembled loci & %s & %s \\\\' % (
            nice_int(cuffcmp['queryLociNotInRef']),
            nice_int(cuffcmp['queryLociTotal'])))
    pa_table.add_row('Reference loci & %s & %s \\\\' % (
            nice_int(cuffcmp['refLociNotInQuery']),
            nice_int(cuffcmp['refLociTotal'])))

    pa_table.end()

    return str(snsp_table) + '\n\n' + str(pa_table)


# and some handy functions
def natural_sort(inlist):
    key = lambda x: [int(a) if a.isdigit() else a.lower() for a in
            re.split('([0-9]+)', x)]
    inlist.sort(key=key)
    return inlist

# subclass of RigRun that contains list of all programs in the pipeline
# and functions specific for processing Gentrap run data
class GentrapRun(RigRun):

    def __init__(self, *args, **kwargs):
        super(GentrapRun, self).__init__(*args, **kwargs)
        Program = namedtuple('Program', ['name', 'version', 'desc'])
        self.programs = [
           Program('Gentrap', self.root.vars['GENTRAP_VER'],
               """the complete pipeline"""),
           Program('FastQC', self.root.vars['FASTQC_VER'],
               """raw and preprocessed sequence metrics collection"""),
           Program('cutadapt', self.root.vars['CUTADAPT_VER'],
               """clipping of known adapter and/or primer sequences"""),
           Program('sickle', self.root.vars['SICKLE_VER'],
               """base quality trimming"""),
           Program('GSNAP', self.root.vars['GSNAP_VER'], """alignment"""),
           Program('TopHat', self.root.vars['TOPHAT_VER'], """alignment"""),
           Program('STAR', self.root.vars['STAR_VER'], """alignment"""),
           Program('gemtools', self.root.vars['GEMTOOLS_VER'], """alignment"""),
           Program('SAMtools', self.root.vars['SAMTOOLS_VER'],
               """sorting, indexing, and merging of BAM files"""),
           Program('Picard', self.root.vars['PICARD_VER'],
               """insert size and annotation metrics collection"""),
           Program('Cufflinks', self.root.vars['CUFFLINKS_VER'],
               """transcript assembly"""),
           Program('Cuffcompare', self.root.vars['CUFFLINKS_VER'],
               """transcript assembly evaluation"""),
           Program('BEDtools', self.root.vars['BEDTOOLS_VER'],
               """base count table creation"""),
           Program('R', self.root.vars['RSCRIPT_VER'],
               """exon and/or gene base count table creation"""),
           Program('HTSeq', self.root.vars['HTSEQ_COUNT_VER'],
               """gene read count table creation"""),
        ]
        # store names of all chromosomes listed in file
        with open(self.root.vars['GENTRAP_CHR_NAMES_DATA']) as src:
            chrs = [line.strip() for line in src]
        self.chrs = natural_sort(chrs)
        # parse various statistics
        self.get_clip_stats()
        self.get_trim_stats()
        self.get_fastqc_stats()
        self.annot_stats = self.get_json_stats(self.root.vars['SAMPLE'] +
                '.rnametrics.json')
        self.get_annot_read_stats()
        self.get_annot_region_stats()
        self.map_stats = self.get_json_stats(self.root.vars['SAMPLE'] +
                '.bamrna.json')
        self.get_asm_stat_fnames()

    @property
    def is_paired_end(self):
        return 'SAMPLEB' in self.root.vars

    @property
    def is_strand_specific(self):
        return self.root.vars['GENTRAP_STRAND_SPECIFIC'] != 'no'

    def get_clip_stats(self):

        if self.root.vars['GENTRAP_QC_MODE'] == "none" or \
                self.root.vars['GENTRAP_QC_MODE'] == "trim":
            self.clip_stats = {}
            return

        re_aff = re.compile(r'\s*Trimmed reads:\s*(\d+)')
        re_short = re.compile(r'\s*Too short reads:\s*(\d+)')
        re_long = re.compile(r'\s*Too long reads:\s*(\d+)')
        re_disc_1 = re.compile(r'Filtered (\d+) .+ first')
        re_disc_2 = re.compile(r'Filtered (\d+) reads from second')
        re_kept = re.compile(r'Synced read .+ (\d+) reads.')

        def get_re_adp(seq):
            return re.compile("Adapter.+'{0}'.+trimmed (\d+) times.".format(seq))

        def get_adapters(read_pair):
            adapters = {}
            with self.open_result(read_pair + '.contams.txt') as src:
                for line in src:
                    _, seq, name = line.strip().split('\t')
                    # None is placeholder for trimming count
                    adapters[name] = [seq, None]

            return adapters

        def get_cutadapt_stats(read_pair, adapter_dict):
            stats = {}
            stat_file = read_pair + '.clipstat'

            try:
                with self.open_result(stat_file) as src:
                    cutadapt_txt = src.read()
            except IOError:
                # file does not exist, no adapter contamination found
                stats['discard_count'] = 0
                stats['affected_count'] = 0
                return stats

            short_count = int(re.search(re_short, cutadapt_txt).group(1))
            long_count = int(re.search(re_long, cutadapt_txt).group(1))
            stats['discard_count'] = short_count + long_count
            stats['affected_count'] = int(re.search(re_aff,
                cutadapt_txt).group(1))
            for key in adapter_dict:
                re_adp = get_re_adp(adapter_dict[key][0])
                adapter_dict[key][1] = int(re.search(re_adp,
                    cutadapt_txt).group(1))

            return stats

        def get_sync_stats(sample):
            stats = {}
            with self.open_result(sample + '.clipsync.stats') as src:
                sync_txt = src.read()

            stats['discarda'] = int(re.search(re_disc_1, sync_txt).group(1))
            stats['discardb'] = int(re.search(re_disc_2, sync_txt).group(1))
            stats['kept'] = int(re.search(re_kept, sync_txt).group(1))
            stats['discard_total'] = stats['discarda'] + stats['discardb']

            return stats

        clip_stats = {'samplea': {}}
        clip_stats['samplea']['adapters'] = get_adapters(self.root.vars['SAMPLEA'])
        clip_stats['samplea']['cutadapt'] = \
                get_cutadapt_stats(self.root.vars['SAMPLEA'],
                        clip_stats['samplea']['adapters'])

        if self.is_paired_end:
            clip_stats['sampleb'] = {}
            clip_stats['sampleb']['adapters'] = get_adapters(self.root.vars['SAMPLEB'])
            clip_stats['sampleb']['cutadapt'] = \
                    get_cutadapt_stats(self.root.vars['SAMPLEB'],
                            clip_stats['sampleb']['adapters'])
            clip_stats['sync'] = get_sync_stats(self.root.vars['SAMPLE'])
        else:
            clip_stats['sync'] = {}

        self.clip_stats = clip_stats

    def get_trim_stats(self):

        if self.root.vars['GENTRAP_QC_MODE'] == "none" or \
                self.root.vars['GENTRAP_QC_MODE'] == "clip":
            self.trim_stats = {}
            return

        trim_stats = {}

        suffix = '.trimsync.stats'
        if 'clip' in self.root.vars['GENTRAP_QC_MODE']:
            suffix = '.clipsync' + suffix

        if self.is_paired_end:
            re_paired_kept = re.compile(r'paired records kept: \d+ \((\d+) pairs\)')
            re_disc = re.compile(r'single records discarded: \d+ \(from PE1: (\d+), from PE2: (\d+)\)')
            re_disc_paired = re.compile(r'paired records discarded: \d+ \((\d+) pairs\)')
            with self.open_result(self.root.vars['SAMPLE'] + suffix) as src:
                sickle_txt = src.read()

            discarda = int(re.search(re_disc, sickle_txt).group(1))
            discardb = int(re.search(re_disc, sickle_txt).group(2))
            discard_both = int(re.search(re_disc_paired, sickle_txt).group(1)) 

            trim_stats['kept'] = int(re.search(re_paired_kept, sickle_txt).group(1))
            trim_stats['discarda'] = discarda
            trim_stats['discardb'] = discardb
            trim_stats['discard_both'] = discard_both
            trim_stats['discard_total'] = discarda + discardb + discard_both

        else:
            re_kept = re.compile(r'records kept: (\d+)')
            re_disc = re.compile(r'records discarded: (\d+)')
            with self.open_result(self.root.vars['SAMPLE'] + suffix) as src:
                sickle_txt = src.read()

            trim_stats['kept'] = int(re.search(re_kept, sickle_txt).group(1))
            trim_stats['discard_total'] = int(re.search(re_disc, sickle_txt).group(1))

        self.trim_stats = trim_stats

    def get_fastqc_stats(self):

        re_enc = re.compile(r'Encoding\s+(.*)\nTotal Sequen')
        re_count = re.compile(r'Total Sequences\s+(\d+)')
        re_gc = re.compile(r'%GC\s+(\d+)')
        re_length = re.compile(r'Sequence length\s+([\w-]+)')

        def get_fq_fastqc_stats(fq_name):
            stats = {}
            # top fastqc results directory
            fq_dir = os.path.join(self.path, fq_name)
            assert os.path.exists(fq_dir), "Directory %s does not exist." % fq_dir
            # fastqc directory containing actual results
            core_fq_dir = os.path.join(fq_dir, os.walk(fq_dir).next()[1][0])
            assert os.path.exists(core_fq_dir), "Directory %s does not exist." % core_fq_dir
            with open(os.path.join(core_fq_dir, 'fastqc_data.txt')) as src:
                fq_txt = src.read()

            stats['res_dir'] = core_fq_dir
            stats['enc'] = re.search(re_enc, fq_txt).group(1).strip()
            stats['count'] = int(re.search(re_count, fq_txt).group(1))
            stats['gc'] = float(re.search(re_gc, fq_txt).group(1))
            stats['length'] = re.search(re_length, fq_txt).group(1)
            return stats

        fastqc_stats = {}

        fastqc_stats['samplea'] = {}
        if self.is_paired_end:
            fastqc_stats['sampleb'] = {}

        if self.root.vars['GENTRAP_QC_MODE'] != "none":
            fastqc_stats['samplea']['raw'] = get_fq_fastqc_stats(self.root.vars['SAMPLEA']
                    + '.fastqc')
            fastqc_stats['samplea']['proc'] = get_fq_fastqc_stats(self.root.vars['SAMPLEA']
                    + '.qc.fastqc')
            if self.is_paired_end:
                fastqc_stats['sampleb']['raw'] = \
                        get_fq_fastqc_stats(self.root.vars['SAMPLEB'] + '.fastqc')
                fastqc_stats['sampleb']['proc'] = \
                        get_fq_fastqc_stats(self.root.vars['SAMPLEB'] + '.qc.fastqc')
        else:
            fastqc_stats['samplea']['raw'] = get_fq_fastqc_stats(self.root.vars['SAMPLEA']
                    + '.qc.fastqc')
            if self.is_paired_end:
                fastqc_stats['sampleb']['raw'] = \
                        get_fq_fastqc_stats(self.root.vars['SAMPLEB'] + '.qc.fastqc')

        self.fastqc_stats = fastqc_stats

    def get_annot_read_stats(self):

        raw = self.annot_stats
        annot_read_stats = []
        mix = raw['mix']['allMetrics']

        if self.is_strand_specific:

            fwd, rev = raw['fwd']['allMetrics'], raw['rev']['allMetrics']

            AnnotCounts = namedtuple('AnnotCounts', ['chr', 'mix', 'sense_ok',
                'asense_no', 'asense_ok', 'sense_no'])
            for chr in self.chrs:
                annot_read_stats.append(AnnotCounts(chr, mix[chr]['metrics']['countMapped'],
                        fwd[chr]['metrics']['correctStrandReads'],
                        fwd[chr]['metrics']['incorrectStrandReads'],
                        rev[chr]['metrics']['correctStrandReads'],
                        rev[chr]['metrics']['incorrectStrandReads']))
            annot_read_stats.append(AnnotCounts('Total', sum([x.mix for x in annot_read_stats]),
                    sum([x.sense_ok for x in annot_read_stats]),
                    sum([x.asense_no for x in annot_read_stats]),
                    sum([x.asense_ok for x in annot_read_stats]),
                    sum([x.sense_no for x in annot_read_stats])))
        else:

            AnnotCounts = namedtuple('AnnotCounts', ['chr', 'mix'])
            for chr in self.chrs:
                annot_read_stats.append(AnnotCounts(chr,
                    mix[chr]['metrics']['countMapped']))
            annot_read_stats.append(AnnotCounts('Total', sum([x.mix for x in
                annot_read_stats])))

        self.annot_read_stats = annot_read_stats

    def get_annot_region_stats(self):

        if not self.is_strand_specific:
            self.annot_region_stats = {}
            return

        def compute_annot_bases(data):
            utr = data['ALL']['metrics']['utrBases']
            coding = data['ALL']['metrics']['codingBases']

            res = {
                'total': data['ALL']['metrics']['pfAlignedBases'],
                'exonic': utr + coding,
                'intronic': data['ALL']['metrics']['intronicBases'],
                'intergenic': data['ALL']['metrics']['intergenicBases'],
            }
            # count percentages
            for reg in ('exonic', 'intronic', 'intergenic'):
                res[reg + '_pct'] = res[reg] * 100.0 / res['total']

            return res
            
        reg_stats = {
            'fwd': compute_annot_bases(self.annot_stats['fwd']['allMetrics']),
            'rev': compute_annot_bases(self.annot_stats['rev']['allMetrics']),
        }
        reg_stats['total'] = {
            'exonic': reg_stats['fwd']['exonic'] + reg_stats['rev']['exonic'],
            'intronic': reg_stats['fwd']['intronic'] + reg_stats['rev']['intronic'],
            'intergenic': reg_stats['fwd']['intergenic'] + reg_stats['rev']['intergenic'],
        }

        self.annot_region_stats = reg_stats

    def get_asm_stat_fnames(self):

        fnames = {}
        if "none" not in self.root.vars['GENTRAP_ASM']:
            for mode in self.root.vars['GENTRAP_ASM'].split(','):
                fnames[mode] = {}
                if self.is_strand_specific:
                    fnames[mode]['fwd'] = self.get_json_stats(
                            self.root.vars['SAMPLE'] + \
                            '.f.asm_%s.cuffcmp.json' % mode)
                    fnames[mode]['rev'] = self.get_json_stats(
                            self.root.vars['SAMPLE'] + \
                            '.r.asm_%s.cuffcmp.json' % mode)
                else:
                    fnames[mode]['mix'] = self.get_json_stats(
                            self.root.vars['SAMPLE'] + \
                            '.asm_%s.cuffcmp.json' % mode)

        self.asm_stat_fnames = fnames

    def get_json_stats(self, fname):
        with self.open_result(fname) as src:
            return json.load(src)


def get_cli_flag_name(rig_flag):
    """Given a Rig tool OPT_* flag name, compute its original CLI flag name."""
    # Rig flag name is always OPT_{tool_name}_{flag_name}
    ntok = rig_flag.split('_', 2)[2].split('_')
    if len(ntok) == 1:
        if len(ntok[0]) == 1:           # e.g. '-k'
            return '-' + ntok[0]
        else:                           # e.g. '--strata'
            return '-{}-' + ntok[0]
    elif len(ntok) > 1:
        return '-{}-' + '-'.join(ntok)  # e.g. '--pairmax-rna'

    assert False, "Unexpected flag name: {0}".format(rig_flag)


def write_template(root_module, log_dir, report_dir, template_dir):

    # spawn environment and create output directory
    env = Environment(loader=FileSystemLoader(template_dir))

    # change delimiters since LaTeX may use '{{', '{%', or '{#'
    env.block_start_string = '((*'
    env.block_end_string = '*))'
    env.variable_start_string = '((('
    env.variable_end_string = ')))'
    env.comment_start_string = '((='
    env.comment_end_string = '=))'

    # trim all block-related whitespaces
    env.trim_blocks = True
    env.lstrip_blocks = True

    # put in out filter functions
    env.filters['nice_int'] = nice_int
    env.filters['nice_flt'] = nice_flt
    env.filters['trans_tables'] = trans_tables

    # parse run logs, allowing for reruns
    run = GentrapRun(root_module, log_dir, single_root_log=False)

    # set shortcut var container & its initial values
    report = {
        'time': datetime.now(),
        'rig_version': __version__,
    }

    # try to parse sample encodings
    report['samplea_enc'] = run.fastqc_stats['samplea']['raw']['enc']
    if run.is_paired_end:
        report['sampleb_enc'] = run.fastqc_stats['sampleb']['raw']['enc']

    # get all OPT_* flag overrides
    OptFlag = namedtuple('Optflag', ['program', 'cli', 'rig', 'value',])
    ovr_pat = re.compile(r'OPT_([A-Z]+)_(\w+)')
    optflags = []
    for ovr in run.root.vars['VARS_OVERRIDES'].split(' '):
        match = re.match(ovr_pat, ovr)
        if match:
            # compute original tool name, flag name, rig flag, and its value
            try:
                program = _TOOL_NAMES[match.group(1).lower()]
            except KeyError:
                program = match.group(1).lower()
            cli_name = get_cli_flag_name(ovr)
            rig_name = ovr
            value = run.root.vars[ovr]
            optflag = OptFlag(program, cli_name, rig_name, value)
            optflags.append(optflag)

    # write tex template for pdflatex
    jinja_template = env.get_template(_BASE_TEMPLATE)
    pdflatex_template = run.root.vars['SAMPLE'] + '.tex'
    render_vars = {
        'run': run,
        'report': report,
        'vars': run.root.vars,
        'optflags': optflags,
        'clip_stats': run.clip_stats,
        'trim_stats': run.trim_stats,
        'fastqc_stats': run.fastqc_stats,
        'map_stats': run.map_stats,
        'annot_read_stats': run.annot_read_stats,
        'annot_region_stats': run.annot_region_stats,
        'asm_stat_fnames': run.asm_stat_fnames,
    }
    rendered = jinja_template.render(**render_vars)

    if not os.path.exists(report_dir):
        os.makedirs(report_dir)
    with open(join(report_dir, pdflatex_template), 'w') as output:
        output.write(rendered)


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('res_dir', type=str,
            help="Path to pipeline results directory")
    parser.add_argument('tpl_dir', type=str,
            help="Path to report template directory")
    parser.add_argument('rep_dir', type=str,
            help="Path to output report directory")
    args = parser.parse_args()

    write_template('gentrap', args.res_dir, args.rep_dir, args.tpl_dir)
