#!/usr/bin/env python2

"""
Overlapping regions removal in a single BED file.


The script will adjust feature coordinates so that no overlaps are present. If
a feature is enveloped entirely by another feature, the smaller feature will be
removed and the enveloping feature split into two.

Input BED files must be position-sorted and only contain the first six fields.
Strands are taken into account when removing regions.

Requirements:
    * Python == 2.7.x
    * track >= 1.1.0 <http://xapple.github.io/track/>

Copyright (c) 2013 Wibowo Arindrarto <w.arindrarto@lumc.nl>
Copyright (c) 2013 LUMC Sequencing Analysis Support Core <sasc@lumc.nl>
MIT License <http://opensource.org/licenses/MIT>

"""

import argparse
import os

import track


class BEDCoord(object):

    """Class representing a BED feature coordinate."""

    __slots__ = ('feature', 'kind', 'point', 'start', 'strand')

    def __init__(self, feature, kind, point, start, strand):
        """

        :param feature: name of BED feature
        :type feature: str
        :param kind: type of coordinate, 'start' or 'end'
        :type kind: str
        :param point: coordinate point
        :type point: int
        :param start: start coordinate of the coordinate with this feature
        :type start: int
        :param strand: strand of the feature, 1 or -1
        :type strand: int

        """
        self.feature = feature
        assert kind in  ('start', 'end')
        self.kind = kind
        self.point = point
        self.start = start
        self.strand = strand

    def __repr__(self):
        return '{0}{1}'.format(self.point, self.kind[0].upper())

    def __gt__(self, other):
        if self.point == other.point:
            return self.start > other.start
        return self.point > other.point

    def __lt__(self, other):
        if self.point == other.point:
            return self.start < other.start
        return self.point < other.point

    def __ge__(self, other):
        return self.point >= other.point

    def __le__(self, other):
        return self.point <= other.point


def squish_track_records(chrom_recs):
    """Given an iterator for `track` records, yield squished `track` features.

    :param chrom_feats: iterator returning `track` records for one chromosome
    :type chrom_feats: iterator
    :returns: (generator) single `track` records
    :rtype: `track.pyrow.SuperRow`

    """
    # Algorithm:
    #   1. Flatten all coordinate points into a single list
    #   2. Sort by point, resolve same point by comparing feature starts
    #      (already defined in BEDCoord's `__lt__` and `__gt__`)
    #   3. Walk through the sorted points while keeping track of overlaps using
    #      a level' counter for each strand
    #   4. Start coordinates increase level counters, end coordinates decrease
    #      them
    #   5. Start coordinates of the squished features are:
    #      * start coordinates in the array when level == 1
    #      * end coordinates in the array when level == 1
    #   6. End coordinates of the squished features are:
    #      * start coordinates in the array when level == 2
    #      * end coordinates in the array when level == 0
    #   7. As additional checks, make sure that:
    #      * when yielding a record, its start coordinate <= its end coordinate
    #      * the level counter never falls below 0 (this doesn't make sense)
    #      * after all iterations are finished, the level counter == 0
    # Assumes:
    #   1. Input BED file is position-sorted
    #   2. Coordinate points all denote closed intervals (this is handled by
    #      `track` for BED files already)
    flat_coords = []
    for rec in chrom_recs:
        flat_coords.append(BEDCoord(rec[2], 'start', rec[0], rec[0], rec[4]))
        flat_coords.append(BEDCoord(rec[2], 'end', rec[1], rec[0], rec[4]))

    flat_coords.sort()

    plus_level, minus_level = 0, 0
    plus_row = [0, 0, "", 0, 1]
    minus_row = [0, 0, "", 0, -1]

    for coord in flat_coords:

        if coord.strand == 1:

            if coord.kind == 'start':
                plus_level += 1
                if plus_level == 1:
                    plus_row[0] = coord.point
                    plus_row[2] = coord.feature
                elif plus_level == 2:
                    plus_row[1] = coord.point
                    # track uses closed coordinates already
                    assert plus_row[0] <= plus_row[1]
                    yield plus_row
            else:
                plus_level -= 1
                if plus_level == 0:
                    plus_row[1] = coord.point
                    plus_row[2] = coord.feature
                    assert plus_row[0] <= plus_row[1]
                    yield plus_row
                elif plus_level == 1:
                    plus_row[0] = coord.point

            assert plus_level >= 0, 'Unexpected feature level: {0}'.format(
                    plus_level)

        elif coord.strand == -1:

            if coord.kind == 'start':
                minus_level += 1
                if minus_level == 1:
                    minus_row[0] = coord.point
                    minus_row[2] = coord.feature
                elif minus_level == 2:
                    minus_row[1] = coord.point
                    assert minus_row[0] <= minus_row[1]
                    yield minus_row
            else:
                minus_level -= 1
                if minus_level == 0:
                    minus_row[1] = coord.point
                    minus_row[2] = coord.feature
                    assert minus_row[0] <= minus_row[1]
                    yield minus_row
                elif minus_level == 1:
                    minus_row[0] = coord.point

            assert minus_level >= 0, 'Unexpected feature level: {0}'.format(
                    minus_level)

    assert plus_level == 0, 'Unexpected end plus feature level: ' \
        '{0}'.format(plus_level)
    assert minus_level == 0, 'Unexpected end minus feature level: ' \
        '{0}'.format(minus_level)


def squish_bed(in_file, out_file):
    """Removes all overlapping regions in the input BED file, writing to the
    output BED file.
    
    :param in_file: path to input BED file
    :type in_file: str
    :param out_file: path to output BED file
    :type out_file: str
    
    """
    # check for input file presence, remove output file if it already exists
    assert os.path.exists(in_file), 'Required input file {0} does not ' \
        'exist'.format(in_file)
    if os.path.exists(out_file):
        os.unlink(out_file)

    with track.load(in_file, readonly=True) as in_track, \
            track.new(out_file, format='bed') as out_track:

        for chrom in in_track.chromosomes:
            chrom_rec = in_track.read(chrom)
            out_track.write(chrom, squish_track_records(chrom_rec))


if __name__ == '__main__':

    usage = __doc__.split('\n\n\n')
    parser = argparse.ArgumentParser(
            formatter_class=argparse.RawDescriptionHelpFormatter,
            description=usage[0], epilog=usage[1])

    parser.add_argument('input', type=str, help='Path to input BED file')
    parser.add_argument('output', type=str, help='Path to output BED file')

    args = parser.parse_args()

    squish_bed(args.input, args.output)
