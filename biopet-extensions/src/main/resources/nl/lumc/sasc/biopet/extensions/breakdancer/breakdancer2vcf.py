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
# A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
# license; For commercial users or users who do not want to follow the AGPL
# license, please contact us to obtain a separate license.
#

__copyright__ = """
Copyright (C) 2013 - Tim te Beek
Copyright (C) 2013 - Wai Yi Leung
Copyright (C) 2013 AllBio (see AUTHORS file)
"""

__desc__ = """Convert breakdancer output to VCF v4.2 .file format."""
__created__ = "Mar 18, 2013"
__author__ = "tbeek,wyleung"

import argparse
import csv
import datetime


def main(tsvfile, vcffile, samplename):
    '''
    :param tsvfile: filename of input file.tsv
    :type tsvfile: string
    :param vcffile: filename of output file.vcf
    :type vcffile: string
    :param samplename: Name of the sample
    :type samplename: string
    '''
    with open(tsvfile) as reader:
        # Parse file
        dictreader = _parse_tsvfile(reader)

        # Write out file
        _format_vcffile(dictreader, vcffile, samplename)

def _parse_tsvfile(readable):
    '''
    Read readable using csv.Sniffer and csv.DictReader
    :param readable: open file.tsv handle to read with csv.DictReader
    :type readable: file
    '''
    prev, curr = 0, 0
    while True:
        line = readable.readline()
        if not line.startswith('#'):
            # lets start from prev # line, without the hash sign
            readable.seek(prev + 1)
            break
        else:
            prev = curr
            curr = readable.tell()

    # Determine dialect
    curr = readable.tell()
    #dialect = csv.Sniffer().sniff(readable.read(3000))
    dialect = 'excel-tab'
    readable.seek(curr)

    # Read file
    dictreader = csv.DictReader(readable, dialect=dialect)
    return dictreader


_tsv_fields = ('Chr1', 'Pos1', 'Orientation1',
               'Chr2', 'Pos2', 'Orientation2',
               'Type', 'Size', 'Score',
               'num_Reads', 'num_Reads_lib',
               'ERR031544.sort.bam')
# 'Chr1': '1',
# 'Pos1': '269907',
# 'Orientation1': '39+39-',
# 'Chr2': '1',
# 'Pos2': '270919',
# 'Orientation2': '39+39-',
# 'Type': 'DEL',
# 'Size': '99',
# 'Score': '99',
# 'num_Reads': '38',
# 'num_Reads_lib': '/home/allbio/ERR031544.sort.bam|38',
# 'ERR031544.sort.bam': 'NA'



_vcf_fields = ['CHROM', 'POS', 'ID', 'REF', 'ALT', 'QUAL', 'FILTER', 'INFO', 'FORMAT']

TS_NOW = datetime.datetime.now()

VCF_HEADER = """##fileformat=VCFv4.2
##fileDate={filedate}
##source=breakdancer-max
##INFO=<ID=NS,Number=1,Type=Integer,Description="Number of Samples With Data">
##INFO=<ID=DP,Number=1,Type=Integer,Description="Total Depth">
##INFO=<ID=AF,Number=A,Type=Float,Description="Allele Frequency">
##INFO=<ID=IMPRECISE,Number=0,Type=Flag,Description="Imprecise structural variation">
##INFO=<ID=NOVEL,Number=0,Type=Flag,Description="Indicates a novel structural variation">
##INFO=<ID=SVEND,Number=1,Type=Integer,Description="End position of the variant described in this record">
##INFO=<ID=END,Number=1,Type=Integer,Description="End position of the variant described in this record">
##INFO=<ID=SVMETHOD,Number=0,Type=String,Description="Program called with">
##INFO=<ID=SVTYPE,Number=1,Type=String,Description="Type of structural variant">
##INFO=<ID=SVLEN,Number=.,Type=Integer,Description="Difference in length between REF and ALT alleles">
##INFO=<ID=CIPOS,Number=2,Type=Integer,Description="Confidence interval around POS for imprecise variants">
##INFO=<ID=CIEND,Number=2,Type=Integer,Description="Confidence interval around END for imprecise variants">
##INFO=<ID=HOMLEN,Number=.,Type=Integer,Description="Length of base pair identical micro-homology at event breakpoints">
##INFO=<ID=HOMSEQ,Number=.,Type=String,Description="Sequence of base pair identical micro-homology at event breakpoints">
##INFO=<ID=BKPTID,Number=.,Type=String,Description="ID of the assembled alternate allele in the assembly file">
##INFO=<ID=MEINFO,Number=4,Type=String,Description="Mobile element info of the form NAME,START,END,POLARITY">
##INFO=<ID=METRANS,Number=4,Type=String,Description="Mobile element transduction info of the form CHR,START,END,POLARITY">
##INFO=<ID=DGVID,Number=1,Type=String,Description="ID of this element in Database of Genomic Variation">
##INFO=<ID=DBVARID,Number=1,Type=String,Description="ID of this element in DBVAR">
##INFO=<ID=DBRIPID,Number=1,Type=String,Description="ID of this element in DBRIP">
##INFO=<ID=MATEID,Number=.,Type=String,Description="ID of mate breakends">
##INFO=<ID=PARID,Number=1,Type=String,Description="ID of partner breakend">
##INFO=<ID=EVENT,Number=1,Type=String,Description="ID of event associated to breakend">
##INFO=<ID=CILEN,Number=2,Type=Integer,Description="Confidence interval around the inserted material between breakends">
##INFO=<ID=DP,Number=1,Type=Integer,Description="Read Depth of segment containing breakend">
##INFO=<ID=DPADJ,Number=.,Type=Integer,Description="Read Depth of adjacency">
##INFO=<ID=CN,Number=1,Type=Integer,Description="Copy number of segment containing breakend">
##INFO=<ID=CNADJ,Number=.,Type=Integer,Description="Copy number of adjacency">
##INFO=<ID=CICN,Number=2,Type=Integer,Description="Confidence interval around copy number for the segment">
##INFO=<ID=CICNADJ,Number=.,Type=Integer,Description="Confidence interval around copy number for the adjacency">
##FORMAT=<ID=CN,Number=1,Type=Integer,Description="Copy number genotype for imprecise events">
##FORMAT=<ID=CNQ,Number=1,Type=Float,Description="Copy number genotype quality for imprecise events">
##FORMAT=<ID=CNL,Number=.,Type=Float,Description="Copy number genotype likelihood for imprecise events">
##FORMAT=<ID=NQ,Number=1,Type=Integer,Description="Phred style probability score that the variant is novel">
##FORMAT=<ID=HAP,Number=1,Type=Integer,Description="Unique haplotype identifier">
##FORMAT=<ID=AHAP,Number=1,Type=Integer,Description="Unique identifier of ancestral haplotype">
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##FORMAT=<ID=GQ,Number=1,Type=Integer,Description="Genotype Quality">
##FORMAT=<ID=DP,Number=1,Type=Integer,Description="Read Depth">""".format( filedate=TS_NOW.strftime( "%Y%m%d" ) )

def _format_vcffile(dictreader, vcffile, samplename):
    '''
    Create a pseudo .vcf file based on values read from DictReader instance.
    :param dictreader: DictReader instance to read data from
    :type dictreader: csv.DictRedaer
    :param vcffile: output file.vcf filename
    :type vcffile: string
    '''
    FORMAT = "GT:DP"
    with open(vcffile, mode='w') as writer:
        writer.write('{header}\n#{columns}\n'.format(header=VCF_HEADER, columns='\t'.join(_vcf_fields + [samplename])))
        output_vcf = []
        for line in dictreader:
            CHROM = line['Chr1']
            # TODO Figure out whether we have zero or one based positioning
            POS = int(line['Pos1'])
            ALT = '<{}>'.format(line['Type'])
            SVEND = int(line['Pos2'])

            INFO = 'SVMETHOD=breakdancer;SVTYPE={}'.format(line['Type'])

            if line['Type'] not in ['CTX']:
                INFO += ';SVLEN={}'.format(int(line['Size']))
                INFO += ";SVEND={}".format(SVEND)
                INFO += ";END={}".format(SVEND)

            # write alternate ALT field for Intrachromosomal translocations
            if line['Type'] in ['CTX']:
                ALT = "N[{}:{}[".format(line['Chr2'], line['Pos2'])



            SAMPLEINFO = "{}:{}".format( '1/.', line['num_Reads'] )
            # Create record
            output_vcf.append([CHROM, POS, '.', 'N', ALT, '.', 'PASS', INFO, FORMAT, SAMPLEINFO])

        # Sort all results
        output_vcf.sort()
        output = "\n".join(["\t".join(map(str,vcf_row)) for vcf_row in output_vcf])
        # Write record
        writer.write(output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--breakdancertsv', dest='breakdancertsv', type=str,
                        help='Breakdancer TSV outputfile')
    parser.add_argument('-o', '--outputvcf', dest='outputvcf', type=str,
                        help='Output vcf to')
    parser.add_argument('-s', '--sample', dest='sample', type=str,
                        help='sample name')

    args = parser.parse_args()
    main(args.breakdancertsv, args.outputvcf, args.sample)
