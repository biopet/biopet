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

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', "--input", required=True)
    parser.add_argument("-t", "--threshold", type=int, default=5)

    args = parser.parse_args()
    with open(args.input) as handle:
        for line in handle:
            value = float(line.strip().split("\t")[-1])
            if abs(value) >= args.threshold:
                print(line)