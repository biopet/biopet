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


from __future__ import print_function

__author__="Peter van 't Hof"

import sys
import re

upacPatern = re.compile(r'[RYKMSWBDHV]')

if __name__ == "__main__":
    for line in sys.stdin:
        l = line.strip().split("\t")
        if len(l) >= 3:
            l[3] = upacPatern.sub("N", l[3])

        print("\t".join(map(str, l)))
