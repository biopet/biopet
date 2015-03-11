#!/usr/bin/env python

from __future__ import print_function

__author__="Wai Yi Leung"

import sys

if __name__ == "__main__":
    """
        Fix the mpileupformat for RNA mpileup
        Solution offered by Irina Pulyakhina (LUMC-HG)
        http://www.biostars.org/p/78542/
    """
    for line in sys.stdin:
        l = line.strip().split("\t")
        if l[3] == "0":
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
