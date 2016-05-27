#!/usr/bin/env python2.7
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
Plot output of coverageBed / "bedtools coverage" when run with the -d flag.


This script plots a bar graph showing how many times a base covered X times
are found. An additional box plot is also plotted to show the general trend
of the coverage.

By default, the bar graph only shows the data up to the 98th percentile.
Counts for bases that are not covered (0x coverage) are shown in lighter
shade. The median, quartile, and whiskers values shown in the boxplot are
still computed from the complete data set (0x coverage included).

Requirements:
    * Python == 2.7.x
    * Matplotlib >= 1.3.0
    * Numpy >= 1.8.0

Copyright (c) 2013 Wibowo Arindrarto <w.arindrarto@lumc.nl>
Copyright (c) 2013 LUMC Sequencing Analysis Support Core <sasc@lumc.nl>
MIT License <http://opensource.org/licenses/MIT>
"""

RELEASE = False
__version_info__ = ('0', '1', )
__version__ = '.'.join(__version_info__)
__version__ += '-dev' if not RELEASE else ''


import argparse
import collections
import itertools
import json
import locale
import os
import sys

import matplotlib
matplotlib.use('Agg')
import matplotlib.gridspec as gs
import matplotlib.pyplot as plt
import matplotlib.ticker as tkr
import numpy as np


BLUE = '#2166AC'
RED = '#1A9850'
GREEN = '#D6604D'

locale.setlocale(locale.LC_ALL, '')
group_digits = lambda x, pos: locale.format('%d', x, grouping=True)
major_formatter = tkr.FuncFormatter(group_digits)


def cachedproperty(func):
    """Decorator for cached property loading."""
    attr_name = func.__name__
    @property
    def cached(self):
        if not hasattr(self, '_cache'):
            setattr(self, '_cache', {})
        try:
            return self._cache[attr_name]
        except KeyError:
            result = self._cache[attr_name] = func(self)
            return result
    return cached


class Coverage(object):

    """Class representing coverage metrics from a coverageBed -d output."""

    def __init__(self, cvg_list, name='?'):
        """

        :param cvg_list: iterator yielding coverage per position
        :type cvg_list: iterable

        """
        assert cvg_list
        counter = collections.Counter()
        total_bases, nonzero_bases = 0, 0
        for cvg in cvg_list:
            counter[cvg] += 1
            total_bases += 1
            if cvg > 0:
                nonzero_bases += 1

        self._counter = counter
        self.total_bases = total_bases
        self.nonzero_bases = nonzero_bases

    def __iter__(self):
        return self._counter.iteritems()

    def __repr__(self):
        return "{0}(...)".format(self.__class__.__name__)

    @cachedproperty
    def total(self):
        """Total coverage."""
        return sum([cvg * count for cvg, count in self])

    @cachedproperty
    def horizontal(self):
        """Horizontal coverage (0 - 1)."""
        return float(self.nonzero_bases) / self.total_bases

    @cachedproperty
    def mean(self):
        """Average coverage."""
        return float(self.total) / self.total_bases

    @cachedproperty
    def max(self):
        """Maximum coverage."""
        return max(self._counter.keys())

    @cachedproperty
    def median(self):
        """Median coverage."""
        return np.percentile(self.cov_counts, 50)

    @cachedproperty
    def cov_counts(self):
        """List of coverage for each base position (unsorted)."""
        # get list of each base coverage
        cov_counts = ([key] * value for key, value in self._counter.items())
        # and flatten the list
        return [item for sublist in cov_counts for item in sublist]

    def at_least(self, n):
        """Return the percentages of bases covered at least n times."""
        x = sum([count for cvg, count in self if cvg >= n])
        return float(x) / self.total_bases

    def get_quick_stats(self):
        """Returns a dictionary containing quick coverage statistics."""
        return {
            'max': self.max,
            'median': self.median,
            'mean': self.mean,
            'horizontal': self.horizontal,
            'width': self.total_bases,
            'width_nonzero': self.nonzero_bases,
            'total': self.total,
            'frac_min_10x': self.at_least(10),
            'frac_min_20x': self.at_least(20),
            'frac_min_30x': self.at_least(30),
            'frac_min_40x': self.at_least(40),
            'frac_min_50x': self.at_least(50),
        }

    def plot(self, min_cov_ok=7, percentile_show=98, title=None, out_img=None):
        """Plots the coverage object.

        :param min_cov_ok: Minimum coverage value to show in the bar graph
            (default: 7).
        :type min_cov_ok: int
        :param percentile_show: Maximum percentile to show in the bar graph
            (default: 98).
        :type percentile_show: int
        :param title: Plot title
        :type title: list of strings (one item per line)
        :param out_img: Output image filename.
        :type out_img: str

        """
        plt.figure(figsize=(8, 8))
        grids = gs.GridSpec(2, 1, height_ratios=[5, 1])

        ax0 = plt.subplot(grids[0])

        if title is None:
            title = ['Coverage Plot']
        elif isinstance(title, basestring):
            title = [title]
        t = plt.title('\n'.join(title), fontsize=20)
        t.set_y(1.05)

        x_data, y_data = [], []
        x_data_shade, y_data_shade = [], []
        for coverage, count in self:
            if coverage >= min_cov_ok:
                x_data.append(coverage)
                y_data.append(count)
            else:
                x_data_shade.append(coverage)
                y_data_shade.append(count)

        ax0.bar(x_data, y_data, width=1, linewidth=0, color=BLUE, align='center')
        ax0.yaxis.set_major_formatter(major_formatter)
        ax0.grid(True)
        plt.ylabel('Counts')
        ax0.text(0.5, 0.95, 'Mean: %.2fx     Median: %.2fx     '
                'Horizontal: %.2f%%' % (self.mean, self.median, 100.0 * \
                self.horizontal), verticalalignment='center',
                horizontalalignment='center', transform=ax0.transAxes,
                bbox=dict(facecolor='wheat', alpha=0.5, boxstyle='round'),
                size=12)

        ax1 = plt.subplot(grids[1], sharex=ax0)
        ax1.axes.get_yaxis().set_visible(False)
        bp = ax1.boxplot(self.cov_counts, vert=False, widths=0.6, sym='+')
        for x in itertools.chain(bp['boxes'], bp['medians'], bp['whiskers'],
                bp['caps']):
            x.set(color=BLUE, linewidth=1.6)
        for flier in bp['fliers']:
            plt.setp(flier, color='GREEN', alpha=0.5)
        
        upper_limit = np.percentile(self.cov_counts, percentile_show)
        if x_data:
            space = (upper_limit - min(x_data)) / 40
        else:
            space = 0
        # truncate plot if we're not displaying maximum value
        if upper_limit != self.max:
            ax1.set_xlim([min(self.cov_counts) - space - 0.5, upper_limit + 0.5])
        # otherwise, give some space
        else:
            ax1.set_xlim([min(self.cov_counts) - space - 0.5, upper_limit +
                space + 0.5])

        # plot shaded values
        if x_data_shade:
            ylim = ax0.get_ylim()
            ax0.bar(x_data_shade, y_data_shade, width=1, linewidth=0,
                    color=BLUE, alpha=0.3, align='center')
            ax0.set_ylim(ylim)

        plt.xlabel('Coverage')
        grids.update(hspace=0.075)

        if out_img is not None:
            plt.savefig(out_img, bbox_inches='tight')


if __name__ == '__main__':

    usage = __doc__.split('\n\n\n')
    parser = argparse.ArgumentParser(
            formatter_class=argparse.RawDescriptionHelpFormatter,
            description=usage[0], epilog=usage[1])

    parser.add_argument('input', type=str, help='Path to input file '
            '(coverageBed output) or \'-\' for stdin')
    parser.add_argument('--plot', dest='plot', type=str,
            help='Path to output PNG file')
    parser.add_argument('--min-cov-show', dest='min_cov_ok', type=int,
            default=6, help='Minimum coverage to show in bar graph')
    parser.add_argument('--max-percentile-show', dest='max_pct_show', type=int,
            default=98, help='Maximum percentile to show in bar graph')
    parser.add_argument('--title', dest='title', type=str,
            default='Coverage Plot', help='Plot title')
    parser.add_argument('--subtitle', dest='subtitle', type=str, help='Plot subtitle')

    args = parser.parse_args()

    if args.input == '-':
        instream = sys.stdin
    else:
        instream = open(args.input, 'r')

    title = [args.title]
    if args.subtitle is None:
        title.append("'" + args.input + "'")
    else:
        title.append(args.subtitle)

    coverages = {}
    all_covs, cur_covs = [], []
    cur_chrom, prev_chrom = None, None
    for line in (l.strip() for l in instream):
        cols = line.split('\t')
        cur_chrom = cols[0]
        cvg = int(cols[-1])
        # coverage for all positions
        all_covs.append(cvg)
        # coverage per chromosome
        if cur_chrom != prev_chrom:
            if prev_chrom is not None:
                coverages[prev_chrom] = cur_covs
            cur_covs = []
            prev_chrom = cur_chrom
        cur_covs.append(cvg)
    # also append the last chromosome from the file
    coverages[cur_chrom] = cur_covs

    coverages['_all'] = all_covs

    for cname, clist in coverages.items():
        coverages[cname] = Coverage(clist)

    if args.input != '-':
        instream.close()

    if args.plot is not None:
        coverages['_all'].plot(min_cov_ok=args.min_cov_ok, percentile_show=args.max_pct_show,
                title=title, out_img=args.plot)

    stats = {'coverage': {k: v.get_quick_stats() for k, v in coverages.items()}}
    json.dump(stats, sys.stdout, sort_keys=True, indent=4, separators=(',', ': '))