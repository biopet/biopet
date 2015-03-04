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
# A dual licensing mode is applied. The source code within this project that are
# not part of GATK Queue is freely available for non-commercial use under an AGPL
# license; For commercial users or users who do not want to follow the AGPL
# license, please contact us to obtain a separate license.
#
# pdf_report.py
#
# TeX template generation script for Gentrap pipeline.

from __future__ import print_function

import argparse
import json
import locale
import os
import re
import sys
from os import path

from jinja2 import Environment, FileSystemLoader

# set locale for digit grouping
locale.setlocale(locale.LC_ALL, "")

# HACK: remove this and use jinja2 only for templating
class LongTable(object):

    """Class representing a longtable in LaTeX."""

    def __init__(self, caption, label, header, aln, colnum):
        self.lines = [
            "\\begin{center}",
            "\\captionof{table}{%s}" % caption,
            "\\label{%s}" % label,
            "\\begin{longtable}{%s}" % aln,
            "\\hline",
            "%s" % header,
            "\\hline \\hline",
            "\\endhead",
            "\\hline \\multicolumn{%i}{c}{\\textit{Continued on next page}}\\\\" % \
                    colnum,
            "\\hline",
            "\\endfoot",
            "\\hline",
            "\\endlastfoot",
        ]

    def __str__(self):
        return "\n".join(self.lines)

    def add_row(self, row):
        self.lines.append(row)

    def end(self):
        self.lines.extend(["\\end{longtable}", "\\end{center}",
            "\\addtocounter{table}{-1}"])


# filter functions for the jinja environment
def nice_int(num):
    return locale.format("%i", int(num), grouping=True)


def nice_flt(num):
    return locale.format("%.2f", float(num), grouping=True)


# and some handy functions
def natural_sort(inlist):
    key = lambda x: [int(a) if a.isdigit() else a.lower() for a in
            re.split("([0-9]+)", x)]
    inlist.sort(key=key)
    return inlist


def write_template(summary_file, template_file):

    template_file = path.abspath(path.realpath(template_file))
    template_dir = path.dirname(template_file)
    # spawn environment and create output directory
    env = Environment(loader=FileSystemLoader(template_dir))

    # change delimiters since LaTeX may use "{{", "{%", or "{#"
    env.block_start_string = "((*"
    env.block_end_string = "*))"
    env.variable_start_string = "((("
    env.variable_end_string = ")))"
    env.comment_start_string = "((="
    env.comment_end_string = "=))"

    # trim all block-related whitespaces
    env.trim_blocks = True
    env.lstrip_blocks = True

    # put in out filter functions
    env.filters["nice_int"] = nice_int
    env.filters["nice_flt"] = nice_flt

    # write tex template for pdflatex
    jinja_template = env.get_template(path.basename(template_file))
    render_vars = {
        "gentrap": {
            "version": "--testing--",
            "logo": "/home/warindrarto/devel/intellij/biopet/public/gentrap/src/main/resources/nl/lumc/sasc/biopet/pipelines/gentrap/templates/img/gentrap_front.png",
        },
    }
    rendered = jinja_template.render(**render_vars)

    print(rendered, file=sys.stdout)


if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument("summary_file", type=str,
            help="Path to Gentrap summary file")
    parser.add_argument("template_file", type=str,
            help="Path to main template file")
    args = parser.parse_args()

    write_template(args.summary_file, args.template_file)
