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

__author__="Wai Yi Leung"

import sys
import re

upacPatern = re.compile(r'[RYKMSWBDHV]')

if __name__ == "__main__":
    """
        Fix the mpileupformat for RNA mpileup
        Solution offered by Irina Pulyakhina (LUMC-HG)
        http://www.biostars.org/p/78542/
    """
    for line in sys.stdin:
        l = line.strip().split("\t")
        l[2] = upacPatern.sub("N", l[2])

        if len(l) < 4 or l[3] == "0":
            # no alignment to this position
            print("\t".join(map(str, l)))
            continue

        fix_col = l[4].replace('<', '').replace('>', '')

        new_size = len(fix_col)
        old_size = len(l[4])
        if new_size != old_size:
            l[4] = fix_col
            l[3] = "{}".format(new_size)

        if new_size == 0:
            l[5] = ""

        print("\t".join(map(str, l)))
