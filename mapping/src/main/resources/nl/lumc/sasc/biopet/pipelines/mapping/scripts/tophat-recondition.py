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

"""
This script modifies the reads in a TopHat unmapped.bam file to make them
compatible with downstream tools (e.g., Picard, samtools or GATK).

Homepage: https://github.com/cbrueffer/tophat-recondition
"""

from __future__ import print_function

import errno
import os
import sys
try:
    import pysam
except ImportError:
    print('Cannot import the pysam package; please make sure it is installed.\n')
    sys.exit(1)

VERSION = "0.3"


def get_index_pos(index, read):
    """Returns the position of a read in the index or None."""

    if read.qname in index:
        return index[read.qname]
    else:
        return None


def fix_unmapped_reads(path, outdir, mapped_file="accepted_hits.bam",
                       unmapped_file="unmapped.bam", cmdline=""):
    # Fix things that relate to all unmapped reads.
    unmapped_dict = {}
    unmapped_index = {}
    with pysam.Samfile(os.path.join(path, unmapped_file)) as bam_unmapped:
        unmapped_reads = list(bam_unmapped.fetch(until_eof=True))
        unmapped_header = bam_unmapped.header
        for i in range(len(unmapped_reads)):
            read = unmapped_reads[i]

            # remove /1 and /2 suffixes
            if read.qname.find("/") != -1:
                read.qname = read.qname[:-2]

            unmapped_index[read.qname] = i

            # work around "mate is unmapped" bug in TopHat
            if read.qname in unmapped_dict:
                unmapped_reads[unmapped_dict[read.qname]].mate_is_unmapped = True
                read.mate_is_unmapped = True
            else:
                unmapped_dict[read.qname] = i

            read.mapq = 0

            unmapped_reads[i] = read

        # Fix things that relate only to unmapped reads with a mapped mate.
        with pysam.Samfile(os.path.join(path, mapped_file)) as bam_mapped:
            for mapped in bam_mapped:
                if mapped.mate_is_unmapped:
                    i = get_index_pos(unmapped_index, mapped)
                    if i is not None:
                        unmapped = unmapped_reads[i]

                        # map chromosome TIDs from mapped to unmapped file
                        mapped_rname = bam_mapped.getrname(mapped.tid)
                        unmapped_new_tid = bam_unmapped.gettid(mapped_rname)

                        unmapped.tid = unmapped_new_tid
                        unmapped.rnext = unmapped_new_tid
                        unmapped.pos = mapped.pos
                        unmapped.pnext = 0

                        unmapped_reads[i] = unmapped

        # for the output file, take the headers from the unmapped file
        base, _ = os.path.splitext(unmapped_file)
        out_filename = "".join([base, "_fixup.sam"]) # since BAM bin values may be messed up after processing

    fixup_header = unmapped_header
    fixup_header['PG'].append({'ID': 'TopHat-Recondition',
                               'VN': VERSION,
                               'CL': cmdline})
    with pysam.Samfile(os.path.join(outdir, out_filename), "wh",
                       header=fixup_header) as bam_out:
        for read in unmapped_reads:
            bam_out.write(read)


def usage(scriptname, errcode=errno.EINVAL):
    print("Usage:\n")
    print(scriptname, "[-hv] tophat_output_dir [result_dir]\n")
    print("-h                 print this usage text and exit")
    print("-v                 print the script name and version, and exit")
    print("tophat_output_dir: directory containing accepted_hits.bam and unmapped.bam")
    print("result_dir:        directory to write unmapped_fixup.bam to (default: tophat_output_dir)")
    sys.exit(errcode)


if __name__ == "__main__":
    import getopt

    scriptname = os.path.basename(sys.argv[0])
    cmdline = " ".join(sys.argv)

    try:
        opts, args = getopt.gnu_getopt(sys.argv[1:], "dhv")
    except getopt.GetoptError as err:
        # print help information and exit
        print(str(err), file=sys.stderr)
        usage(scriptname, errcode=errno.EINVAL)

    debug = False
    for o, a in opts:
        if o in "-d":
            debug = True
        elif o in "-h":
            usage(scriptname, errcode=0)
        elif o in "-v":
            print(scriptname, VERSION)
            sys.exit(0)
        else:
            assert False, "unhandled option"
            sys.exit(errno.EINVAL)

    if len(args) == 0 or len(args) > 2:
        usage(scriptname, errcode=errno.EINVAL)

    bamdir = args.pop(0)
    if not os.path.isdir(bamdir):
        print("Specified tophat_output_dir does not exist or is not a directory: %s" % bamdir, file=sys.stderr)
        sys.exit(errno.EINVAL)

    if len(args) > 0:
        resultdir = args.pop(0)
        if not os.path.isdir(resultdir):
            print("Specified result_dir does not exist or is not a directory: %s" % resultdir, file=sys.stderr)
            sys.exit(errno.EINVAL)
    else:
        resultdir = bamdir

    try:
        fix_unmapped_reads(bamdir, resultdir, cmdline=cmdline)
    except KeyboardInterrupt:
        print("Program interrupted by user, exiting.")
        sys.exit(errno.EINTR)
    except Exception as e:
        if debug:
            import traceback
            print(traceback.format_exc(), file=sys.stderr)

        print("Error: %s" % str(e), file=sys.stderr)
        if hasattr(e, "errno"):
            sys.exit(e.errno)
        else:
            sys.exit(1)
