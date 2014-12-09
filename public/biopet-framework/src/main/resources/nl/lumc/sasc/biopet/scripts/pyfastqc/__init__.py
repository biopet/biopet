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


"""
--------
pyfastqc
--------

Parser for FastQC results.

Provides a method and classes for parsing a FastQC run results.

Tested with FastQC 0.9.5 output.

"""

import os


def load_file(fname, mode='r', **kwargs):
    """Given a path to a FastQC data file or an open file object pointing to it,
    return a `FastQC` object.
    """
    if isinstance(fname, basestring):
        with open(fname, mode, **kwargs) as fp:
            return FastQC(fp)
    else:
        return FastQC(fname)


def load_from_dir(dirname, data_fname='fastqc_data.txt', mode='r', **kwargs):
    """Given a path to a FastQC results directory, return a `FastQC` object."""
    assert os.path.exists(dirname), "Directory %r does not exist" % dirname
    fqc_path = os.path.join(dirname, os.walk(dirname).next()[1][0], data_fname)
    return load_file(fqc_path, mode, **kwargs)


class FastQCModule(object):

    """Class representing a FastQC analysis module."""

    def __init__(self, raw_lines, end_mark='>>END_MODULE'):
        self.raw_lines = raw_lines
        self.end_mark = end_mark
        self._status = None
        self._name = None
        self._data = self.parse()

    def __repr__(self):
        return '%s(%s)' % (self.__class__.__name__,
                '[%r, ...]' % self.raw_lines[0])

    def __str__(self):
        return ''.join(self.raw_lines)

    @property
    def name(self):
        """Name of the module."""
        return self._name

    @property
    def columns(self):
        """Columns in the module."""
        return self._columns

    @property
    def data(self):
        """FastQC data."""
        return self._data

    @property
    def status(self):
        """FastQC run status."""
        return self._status

    def parse(self):
        """Common parser for a FastQC module."""
        # check that the last line is a proper end mark
        assert self.raw_lines[-1].startswith(self.end_mark)
        # parse name and status from first line
        tokens = self.raw_lines[0].strip().split('\t')
        name = tokens[0][2:]
        self._name = name
        status = tokens[-1]
        assert status in ('pass', 'fail', 'warn'), "Unknown module status: %r" \
            % status
        self._status = status
        # and column names from second line
        columns = self.raw_lines[1][1:].strip().split('\t')
        self._columns = columns
        # the rest of the lines except the last one
        data = []
        for line in self.raw_lines[2:-1]:
            cols = line.strip().split('\t')
            data.append(cols)

        # optional processing for different modules
        if self.name == 'Basic Statistics':
            data = {k: v for k, v in data}

        return data


class FastQC(object):

    """Class representing results from a FastQC run."""

    # module name -- attribute name mapping
    _mod_map = {
        '>>Basic Statistics': 'basic_statistics',
        '>>Per base sequence quality': 'per_base_sequence_quality',
        '>>Per sequence quality scores': 'per_sequence_quality_scores',
        '>>Per base sequence content': 'per_base_sequence_content',
        '>>Per base GC content': 'per_base_gc_content',
        '>>Per sequence GC content': 'per_sequence_gc_content',
        '>>Per base N content': 'per_base_n_content',
        '>>Sequence Length Distribution': 'sequence_length_distribution',
        '>>Sequence Duplication Levels': 'sequence_duplication_levels',
        '>>Overrepresented sequences': 'overrepresented_sequences',
        '>>Kmer content': 'kmer_content',
    }

    def __init__(self, fp):
        # get file name
        self.fname = fp.name
        self._modules = {}

        line = fp.readline()
        while True:

            tokens = line.strip().split('\t')
            # break on EOF
            if not line:
                break
            # parse version
            elif line.startswith('##FastQC'):
                self.version = line.strip().split()[1]
            # parse individual modules
            elif tokens[0] in self._mod_map:
                attr = self._mod_map[tokens[0]]
                raw_lines = self.read_module(fp, line, tokens[0])
                self._modules[attr] = FastQCModule(raw_lines)

            line = fp.readline()

    def __repr__(self):
        return '%s(%r)' % (self.__class__.__name__, self.fname)

    def _filter_by_status(self, status):
        return [x.name for x in self._modules.values() if x.status == status]

    def read_module(self, fp, line, start_mark):
        raw = [line]
        while not line.startswith('>>END_MODULE'):
            line = fp.readline()
            raw.append(line)

            if not line:
                raise ValueError("Unexpected end of file in module %r" % line)

        return raw

    @property
    def modules(self):
        return self._modules

    @property
    def passes(self):
        return self._filter_by_status('pass')

    @property
    def passes_num(self):
        return len(self.passes)

    @property
    def warns(self):
        return self._filter_by_status('warn')

    @property
    def warns_num(self):
        return len(self.warns)

    @property
    def fails(self):
        return self._filter_by_status('fail')

    @property
    def fails_num(self):
        return len(self.fails)

    @property
    def basic_statistics(self):
        return self._modules['basic_statistics']

    @property
    def per_base_sequence_quality(self):
        return self._modules['per_base_sequence_quality']

    @property
    def per_sequence_quality_scores(self):
        return self._modules['per_sequence_quality_scores']

    @property
    def per_base_sequence_content(self):
        return self._modules['per_base_sequence_content']

    @property
    def per_base_gc_content(self):
        return self._modules['per_base_gc_content']

    @property
    def per_sequence_gc_content(self):
        return self._modules['per_sequence_gc_content']

    @property
    def per_base_n_content(self):
        return self._modules['per_base_n_content']

    @property
    def sequence_length_distribution(self):
        return self._modules['sequence_length_distribution']

    @property
    def sequence_duplication_levels(self):
        return self._modules['sequence_duplication_levels']

    @property
    def overrepresented_sequences(self):
        return self._modules['overrepresented_sequences']

    @property
    def kmer_content(self):
        return self._modules['kmer_content']
