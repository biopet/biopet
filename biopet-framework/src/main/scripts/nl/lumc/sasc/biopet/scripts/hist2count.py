#!/usr/bin/env python
"""
Convert a histogram generated by coverageBed to counts per region.


A histogram file can be generated with the following command:
coverageBed -split -hist -abam sample.bam -b selection.bed > selection.hist

The output consists of four columns:
- Chromosome name.
- Start position.
- End position.
- Number of nucleotides mapped to this region.
- Normalised expression for this region.

If the -c option is used, additional columns can be added.
"""

import argparse
import sys

def hist2count(inputHandle, outputHandle, copy):
    """
    Split a fasta file on length.

    @arg inputHandle: Open readable handle to a histogram file.
    @type inputHandle: stream
    @arg outputHandle: Open writable handle to the counts file.
    @type outputHandle: stream
    @arg outputHandle: List of columns to copy to the output file.
    @type outputHandle: list[int]
    """
    def __copy():
        copyList = ""
        for i in copy:
            copyList += "\t%s" % data[i]
        return copyList
    #__copy

    def __write():
        outputHandle.write("%s\t%i\t%i\t%i\t%f%s\n" % (chromosome, start,
                        end, count, float(count) / (end - start), copyList))

    chromosome = ""
    start = 0
    end = 0
    count = 0

    for line in inputHandle.readlines():
        data = line.split()

        if not data[0] == "all":
            start_temp = int(data[1])
            end_temp = int(data[2])

            if data[0] != chromosome or start_temp != start or end_temp != end:
                if chromosome:
                    __write()
                chromosome = data[0]
                start = start_temp
                end = end_temp
                count = 0
                copyList = __copy()
            #if
            count += int(data[-4]) * int(data[-3])
        #if
    #for
    __write()
#hist2count

def main():
    """
    Main entry point.
    """
    usage = __doc__.split("\n\n\n")
    parser = argparse.ArgumentParser(
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description=usage[0], epilog=usage[1])
    parser.add_argument("-i", dest="input", type=argparse.FileType("r"),
        default=sys.stdin, help="histogram input file (default=<stdin>)")
    parser.add_argument("-o", dest="output", type=argparse.FileType("w"), 
        default=sys.stdout, help="file used as output (default=<stdout>)")
    parser.add_argument("-c", dest="copy", type=int, nargs="+", default=[],
        help="copy a column to the output file")
    args = parser.parse_args()

    hist2count(args.input, args.output, args.copy)
#main

if __name__ == '__main__':
    main()
