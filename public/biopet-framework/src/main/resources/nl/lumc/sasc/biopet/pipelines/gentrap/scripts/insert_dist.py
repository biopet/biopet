#!/usr/bin/env python
#
# insert_dist.py
#
# Given path to a text file containing Picard's CollectInsertSizeMetrics
# results, create a new graph.
#
# (c) 2013 Wibowo Arindrarto [SASC - LUMC]

import argparse
import locale
import os
import re
import textwrap

from collections import namedtuple
from functools import partial

# for headless matplotlib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

from matplotlib.ticker import FuncFormatter

# set locale and formatter for axis ticks
locale.setlocale(locale.LC_ALL, '')
groupdig = lambda x, pos: locale.format('%d', x, grouping=True)
major_formatter = FuncFormatter(groupdig)
int_fmt = partial(locale.format, grouping=True, percent='%i')


def multi_annotate(ax, title, xy_arr=[], *args, **kwargs):
    """Axis annotation function that targets multiple data points."""
    ans = []
    an = ax.annotate(title, xy_arr[0], *args, **kwargs)
    ans.append(an)
    d = {}
    if 'xycoords' in kwargs:
        d['xycoords'] = kwargs['xycoords']
    if 'arrowprops' in kwargs:
        d['arrowprops'] = kwargs['arrowprops']
    for xy in xy_arr[1:]:
        an = ax.annotate(title, xy, alpha=0.0, xytext=(0, 0), textcoords=an, **d)
        ans.append(an)

    return ans


def parse_insert_sizes_histogram(fname):
    """Given a filename or a file object of a Picard COllectInsertSizeMetrics
    output, return the filename, the histogram column names, and the histogram
    data."""

    if isinstance(fname, basestring):
        fp = open(fname, 'r')
    else:
        fp = fname

    line = fp.readline()
    while True:
        if not line:
            raise ValueError("Unexpected end of file")
        # try to get the original bam file name
        elif 'net.sf.picard.analysis.CollectInsertSizeMetrics' in line:
            input = re.search('INPUT=([^\s]*)', line).group(1)
            bamname = os.path.basename(input)
        elif line.startswith('## HISTOGRAM'):
            break
        line = fp.readline()

    # get column names
    colnames = fp.readline().strip().split('\t')

    # iterate over the histogram data lines
    # and fill up missing data with 0s
    data = []
    counter = 0
    for line in fp:
        if not line.strip():
            break
        # bin number starts at 1
        tokens = [int(x) for x in line.split('\t')]
        numcol = len(tokens) - 1
        if counter == tokens[0] - 1:
            data.append(tokens[1:])
            counter += 1
        else:
            while tokens[0] - counter != 1:
                data.append([0] * numcol)
                counter += 1
            data.append(tokens[1:])
            counter += 1
        
    histogram = data

    return bamname, colnames, histogram


def graph_insert_sizes(fname, outname='test.png'):
    """Given a Picard CollectInsertSizes text output filename, write graph(s)
    for the histogram."""
    bamname, colnames, hist = parse_insert_sizes_histogram(fname)

    # map Picard's insert type (based on its column name)
    # to our own name and color
    InsType = namedtuple('InsType', ['label', 'color'])
    design_map = {
        # 5' --F-->       <--R-- 5
        'fr_count': InsType('inward', '#009933'),
        #   <--R-- 5'   5' --F-->
        'rf_count': InsType('outward', 'orange'),
        # 5' --F-->  5' --F-->  or  <--R-- 5' <--R-- 5'
        'tandem_count': InsType('same directions', '#e62e00'),
    }

    fig = plt.figure()
    ax = plt.subplot(111)
    for idx, col in enumerate(colnames[1:]):
        pcd_name = col.split('.')[-1]
        try:
            label = design_map[pcd_name].label
            color = design_map[pcd_name].color
        except KeyError:
            raise ValueError("Unexpected column name: %r" % col)

        data = [m[idx] for m in hist]
        plt.bar(range(len(hist)), data, width=1, linewidth=0, color=color,
                alpha=0.6, label=label)

        max_val = max(data)
        max_val_size = data.index(max_val)
        highest_points = [(idx, max_val) for idx, val in enumerate(data) if val == max_val]
        x_adj = int(len(data) * 0.1)
        y_adj = int(max_val * 0.1)
        bbox_props = dict(boxstyle="round", fc="w", edgecolor='black', alpha=1.0)
        multi_annotate(ax,
                'max count: {0}\nsize: {1} bp'.format(int_fmt(value=max_val),
                    ', '.join([str(x[0]) for x in highest_points])),
                xy_arr=highest_points,
                xytext=(max_val_size + x_adj, max_val + y_adj),
                fontsize=9, bbox=bbox_props,
                horizontalalignment='left', verticalalignment='center',
                arrowprops=dict(color='black', shrink=0.1, width=0.5, headwidth=2.5, ),)

    # adjust ylim to account for annotation box
    init_ylim = ax.get_ylim()
    ax.set_ylim(0, init_ylim[1] * 1.08)

    # set title and its spacing
    title = 'Insert Sizes Distribution'
    t = plt.title('\n'.join([title] + textwrap.wrap('%r' % bamname, 50)),
        fontsize=15)
    t.set_y(1.05)
    plt.legend()
    plt.xlabel("Insert Size")
    plt.ylabel("Alignment Count")
    ax.yaxis.set_major_formatter(major_formatter)
    ax.grid(True)
    plt.savefig(outname, bbox_inches='tight')


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='input file')
    parser.add_argument('output', help='output image file', default='test.png')

    args = parser.parse_args()

    graph_insert_sizes(args.input, args.output) 
