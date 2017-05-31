#!/usr/bin/env python3
#
# Biopet is built on top of GATK Queue for building bioinformatic
# pipelines. It is mainly intended to support LUMC SHARK cluster
# which is running SGE. But other types of HPC that are supported by
# GATK Queue (such as PBS) should also be able to execute Biopet tools and
# pipelines.
#
# Copyright 2014-2017 Sequencing Analysis Support Core - Leiden University Medical Center
#
# Contact us at: sasc@lumc.nl
#
# A dual licensing mode is applied. The source code within this project is
# freely available for non-commercial use under an AGPL license;
# For commercial users or users who do not want to follow the AGPL
# license, please contact us to obtain a separate license.
#
import argparse

import matplotlib as mpl

mpl.use('Agg')
import matplotlib.pyplot as plt
import pysam

from os.path import join, isdir
from os import makedirs
import sys


def get_middle_pos(record):
    x = (int(record[2]) - int(record[1])) // 2
    return int(record[1]) + x


def plot_call(chrom, start, end, whandle, xhandle, shandle, margin, output_loc):
    s_records = list(shandle.fetch(chrom, start - margin, end + margin))
    w_records = list(whandle.fetch(chrom, start - margin, end + margin))
    x_records = list(xhandle.fetch(chrom, start - margin, end + margin))

    s_x = [get_middle_pos(x) for x in s_records]
    s_y = list(map(float, [x[3] for x in s_records]))

    w_x = [get_middle_pos(x) for x in w_records]
    w_y = list(map(float, [x[3] for x in w_records]))

    x_x = [get_middle_pos(x) for x in x_records]
    x_y = list(map(float, [x[3] for x in x_records]))

    figure = plt.figure(figsize=(11, 6))
    figure.add_subplot(111)

    plt.plot(s_x, s_y, color='r', linewidth=3, label="Aggregated Z-score")
    plt.scatter(w_x, w_y, color='g', alpha=0.3, label="Wisecondor Z-scores")
    plt.scatter(x_x, x_y, color='black', alpha=0.3, label="XHMM Z-scores")

    plt.ylim(-30, 30)
    plt.ylabel("Z-score")
    plt.xlabel("Position along {0}".format(chrom))
    plt.legend()
    plt.savefig(output_loc, dpi=300)
    plt.cla()
    plt.close(figure)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--calls", required=True)
    parser.add_argument("-w", "--wisecondor-file", required=True)
    parser.add_argument("-x", "--xhmm-file", required=True)
    parser.add_argument("-s", "--stouff-file", required=True)
    parser.add_argument("-m", "--margin", type=int, default=5000)
    parser.add_argument("-o", "--output-dir", required=True)

    args = parser.parse_args()

    if sys.version_info[0] == 3:
        makedirs(args.output_dir, exists_ok=True)
    elif sys.version_info[0] == 2:
        try:
            makedirs(args.output_dir)
        except OSError:
            if not isdir(args.output_dir):
                raise

    c_handle = pysam.TabixFile(args.calls, parser=pysam.asTuple())
    s_handle = pysam.TabixFile(args.stouff_file, parser=pysam.asTuple())
    w_handle = pysam.TabixFile(args.wisecondor_file, parser=pysam.asTuple())
    x_handle = pysam.TabixFile(args.xhmm_file, parser=pysam.asTuple())

    contigs = c_handle.contigs
    for contig in contigs:

        for call in c_handle.fetch(contig):
            chrom, start, end, _ = call
            ofile = join(args.output_dir, "{0}_{1}-{2}.png".format(chrom, start, end))
            plot_call(chrom, int(start), int(end), whandle=w_handle,
                      shandle=s_handle, xhandle=x_handle,
                      output_loc=ofile, margin=args.margin)
