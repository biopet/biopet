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

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--input")
    parser.add_argument("--db", action="append", default=[])

    args = parser.parse_args()

    dbs = []
    for x in args.db:
        d = {}
        with open(x) as db_handle:
            for line in db_handle:
                reg = "\t".join(line.split("\t")[:3])
                d[reg] = True
            dbs.append(d)

    with open(args.input) as inhandle:
        for line in inhandle:
            reg = "\t".join(line.split("\t")[:3])
            if all([reg in x for x in dbs]):
                print(line.strip())
