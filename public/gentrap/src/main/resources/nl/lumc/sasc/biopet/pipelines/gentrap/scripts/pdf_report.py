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
def nice_int(num, default="None"):
    if num is None:
        return default
    return locale.format("%i", int(num), grouping=True)


def nice_flt(num, default="None"):
    if num is None:
        return default
    return locale.format("%.2f", float(num), grouping=True)


# and some handy functions
def natural_sort(inlist):
    key = lambda x: [int(a) if a.isdigit() else a.lower() for a in
            re.split("([0-9]+)", x)]
    inlist.sort(key=key)
    return inlist


def write_template(run, template_file, logo_file):

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
    run.logo = logo_file
    render_vars = {
        "run": run,
    }
    rendered = jinja_template.render(**render_vars)

    print(rendered, file=sys.stdout)


class GentrapLib(object):

    def __init__(self, run, sample, name, summary):
        assert isinstance(run, GentrapRun)
        assert isinstance(sample, GentrapSample)
        self.run = run
        self.sample = sample
        self.name = name
        self._raw = summary
        self.flexiprep = summary.get("flexiprep", {})
        self.clipping = not self.flexiprep["settings"]["skip_clip"]
        self.trimming = not self.flexiprep["settings"]["skip_trim"]
        self.is_paired_end = self.flexiprep["settings"]["paired"]

    def __repr__(self):
        return "{0}(sample=\"{1}\", lib=\"{2}\")".format(
                self.__class__.__name__, self.sample.name, self.name)


class GentrapSample(object):

    def __init__(self, run, name, summary):
        assert isinstance(run, GentrapRun)
        self.run = run
        self.name = name
        self._raw = summary
        self.lib_names = sorted(summary["libraries"].keys())
        self.libs = \
            {l: GentrapLib(self.run, self, l, summary["libraries"][l]) \
                for l in self.lib_names}

    def __repr__(self):
        return "{0}(\"{1}\")".format(self.__class__.__name__, self.name)


class GentrapRun(object):

    def __init__(self, summary_file):

        with open(summary_file, "r") as src:
            summary = json.load(src)

        self._raw = summary
        self.summary_file = summary_file
        self.sample_names = sorted(summary["samples"].keys())
        self.samples = \
            {s: GentrapSample(self, s, summary["samples"][s]) \
                for s in self.sample_names}

        self.files = summary["gentrap"]["files"]
        self.executables = summary["gentrap"]["executables"]
        self.settings = summary["gentrap"]["settings"]
        self.version = self.settings["version"]

    def __repr__(self):
        return "{0}(\"{1}\")".format(self.__class__.__name__,
                                        self.summary_file)


if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument("summary_file", type=str,
            help="Path to Gentrap summary file")
    parser.add_argument("template_file", type=str,
            help="Path to main template file")
    parser.add_argument("logo_file", type=str,
            help="Path to main logo file")
    args = parser.parse_args()

    run = GentrapRun(args.summary_file)
    write_template(run, args.template_file, args.logo_file)
