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
# gc_dist.py
#
# Given a path to a FASTQ file, create plots of GC percentages.
#
# Part of the Gentrap pipeline.
#
# (c) 2013 Wibowo Arindrarto [SASC - LUMC]

import argparse
import locale
import os
import textwrap

import numpy as np
# for headless matplotlib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.gridspec as gs

from matplotlib.ticker import FuncFormatter, MultipleLocator


# set locale and formatter to do digit grouping
locale.setlocale(locale.LC_ALL, '')
groupdig = lambda x, pos: locale.format('%d', x, grouping=True)
major_formatter = FuncFormatter(groupdig)


def read_seq(fp):
    """Given a FASTQ file, yield its sequences."""
    if isinstance(fp, basestring):
        assert os.path.exists(fp)
        fp = open(fp, 'r')
    for counter, line in enumerate(fp):
        if (counter + 3) % 4 == 0:
            yield line.strip()


def drange(start, stop, step):
    """Like `range` but for floats."""
    cur = start
    while cur < stop:
        yield cur
        cur += step


def graph_gc(fname, outname='test.png'):
    """Graphs the GC percentages of the given FASTQ file."""
    # count GC percentages per sequence
    gcs = []
    for seq in read_seq(fname):
        gc = sum(seq.lower().count(x) for x in ('g', 'c', 's'))
        gcs.append(gc * 100.0 / len(seq))
    # grab mean and std dev for plotting
    mean = np.mean(gcs)
    stdev = np.std(gcs)

    # set the subplots in the figure; top is histogram, bottom is boxplot
    fig = plt.figure(figsize=(8, 8))
    grids = gs.GridSpec(2, 1, height_ratios=[5, 1])

    ax0 = plt.subplot(grids[0])
    # set title and adjust distance to plot
    title = 'Distribution of GC Percentage'
    t = plt.title('\n'.join([title] + textwrap.wrap('%r' %
        os.path.basename(fname), 50)), fontsize=15)
    t.set_y(1.05)

    # start counting bins for width measurement
    total = len(gcs)
    min_hist = min(gcs)
    max_hist = max(gcs)
    low = high = np.median(gcs)
    step = 1
    widths = dict.fromkeys(range(20, 100, 20) + [99], (0, 0))

    while low >= min_hist or high <= max_hist:
        # cap the width marker at min or max gc values
        if high > max_hist: high = max_hist
        if low < min_hist: low = min_hist

        range_count = len([x for x in gcs if low < x < high])
        coverage = float(range_count) / total

        if coverage >= 0.2 and not any(widths[20]):
            widths[20] = (low, high)
        if coverage >= 0.4 and not any(widths[40]):
            widths[40] = (low, high)
        if coverage >= 0.6 and not any(widths[60]):
            widths[60] = (low, high)
        if coverage >= 0.8 and not any(widths[80]):
            widths[80] = (low, high)
        if coverage >= 0.99 and not any(widths[99]):
            widths[99] = (low, high)

        low -= step
        high += step

    # use the bin coordinates for partial background coloring
    for hstart, hend in widths.values():
        plt.axvspan(hstart, hend, facecolor='#0099ff', linestyle='dotted',
                linewidth=2.0, edgecolor='black', alpha=0.2)

    # plot the histogram
    bins = [0] + list(drange(2.5, 100, 5)) + [100]
    n, bins, patches = ax0.hist(gcs, bins=bins, facecolor='#009933', alpha=0.9)
    # set Y-axis ticks label formatting
    ax0.yaxis.set_major_formatter(major_formatter)
    ax0.yaxis.grid(True)
    plt.ylabel('Read count')
    ax0.text(0.02, 0.9, 'Mean: %.2f\nStdev: %.2f' % (mean, stdev),
            transform=ax0.transAxes, bbox=dict(facecolor='grey', alpha=0.5,
                edgecolor='none'), size=14)

    # plot the boxplot
    # shared X-axis, but invisible
    ax1 = plt.subplot(grids[1], sharex=ax0)
    plt.setp(ax1.get_xticklabels(), visible=False)
    # and set the Y-axis to be invisible completely
    ax1.axes.get_yaxis().set_visible(False)
    plot = ax1.boxplot(gcs, vert=False, widths=0.6, sym='r.')
    # line width and color settings for boxplot
    plot['fliers'][0].set_color('#e62e00')
    plot['fliers'][1].set_color('#e62e00')
    plot['boxes'][0].set_color('black')
    plot['boxes'][0].set_linewidth(1.2)
    plot['medians'][0].set_linewidth(1.2)
    plot['medians'][0].set_color('black')
    plot['whiskers'][0].set_color('black')
    plot['whiskers'][0].set_linewidth(1.2)
    plot['whiskers'][1].set_color('black')
    plot['whiskers'][1].set_linewidth(1.2)
    plot['caps'][0].set_linewidth(1.2)
    plot['caps'][1].set_linewidth(1.2)
    # set X-axis label and ticks
    ax0.xaxis.set_major_locator(MultipleLocator(10))
    ax0.xaxis.set_minor_locator(MultipleLocator(5))
    plt.xlabel('% GC')

    grids.update(hspace=0.075)
    plt.savefig(outname, bbox_inches='tight')

    return gcs


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='input FASTQ file', default='reads.fq')
    parser.add_argument('output', help='output image file', default='test.png')

    args = parser.parse_args()

    gcs = graph_gc(args.input, args.output)
