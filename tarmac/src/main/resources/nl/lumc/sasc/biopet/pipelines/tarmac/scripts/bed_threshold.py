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
import numpy as np


class Thresholder(object):
    def __init__(self, filename, threshold):
        self.__handle = open(filename)
        self.threshold = threshold
        self.chrom = None
        self.start = None
        self.end = None
        self.vals = []

    def flush(self):
        if all([x is not None for x in [self.chrom, self.start, self.end]]):
            v = np.median(self.vals)
            print("{0}\t{1}\t{2}\t{3}".format(
                self.chrom, self.start, self.end, v
            ))
        self.chrom = None
        self.start = None
        self.end = None
        self.vals = []

    def next(self):
        self.__next__()

    def __next__(self):
        line = next(self.__handle)
        chrom, start, end, value = line.strip().split("\t")
        if abs(float(value)) >= self.threshold:
            if chrom != self.chrom:
                self.flush()
                self.chrom = chrom
                self.start = start
                self.end = end
                self.vals = [float(value)]
            else:
                self.end = end
                self.vals.append(float(value))
        else:
            self.flush()
        pass

    def __iter__(self):
        return self

    def close(self):
        self.__handle.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', "--input", required=True)
    parser.add_argument("-t", "--threshold", type=int, default=5)

    args = parser.parse_args()
    t = Thresholder(args.input, args.threshold)
    for _ in t:
        pass
    t.flush()