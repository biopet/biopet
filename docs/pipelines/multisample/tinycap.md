# TinyCap

## Introduction

``TinyCap`` is an analysis pipeline meant to process smallRNA captures. We use a fixed aligner in this pipeline: `bowtie` .
By default, we allow one fragment to align up to 5 different locations on the genome. In most of the cases, the shorter 
the sequence, the less 'unique' the pattern is. Multiple **"best"** alignments is in these cases possible.
To avoid **'first-occurrence found and align-to'** bias towards the reference genome, we allow the aligner 
to report more alignment positions.

After alignment, `htseq-count` is responsible for the quantification of transcripts. 
One should supply 2 annotation-files for this to happen:

- mirBase GFF3 file with all annotated and curated miRNA for the genome of interest. [visit mirBase](http://www.mirbase.org/ftp.shtml)
- Ensembl (Gene sets) in GTF format. [visit Ensembl](http://www.ensembl.org/info/data/ftp/index.html) 

Count tables are generated per sample and an aggregation per (run)project is created in the top level folder of the project.


## Starting the pipeline

```bash
biopet pipelines tinycap [options] \
-config `<path-to>/settings_tinycap.yml`
-config `<path-to>/sample_sheet.yml` \
-qsub -jobParaEnv BWA -run
```

## Example

Note that one should first create the appropriate [configs](../../general/config.md).

The pipeline specific (minimum) config looks like:

```yaml
output_dir: <path-to>/output/directory
reference_name: GRCh38
species: H.sapiens
annotation_gff: <path-to>/data/annotation/mirbase-21-hsa.gff3
annotation_refflat: <path-to>/data/annotation/ucsc_ensembl_83_38.refFlat
annotation_gtf: <path-to>/data/annotation/ucsc_ensembl_83_38.gtf
```


### Advanced config:

One can specify other options such as: `bowtie` (alignment) options, clipping and trimming options `sickle` and `cutadapt`.
```yaml
bowtie: 
  chunkmbs: 256  # this is a performance option, keep it high (256) as many alternative alignments are possible
  seedmms: 3
  seedlen: 25
  k: 3           # take and report best 3 alignments 
  best: true     # sort by best hit,
  strata: true     # select from best strata
sickle: 
  lengthThreshold: 15  # minimum length to keep after trimming
cutadapt: 
  error_rate: 0.1    # recommended: 0.1, allow more mismatches in adapter to be clipped of (ratio)
  minimum_length:  15 # minimum length to keep after clipping, setting lower will cause multiple alignments afterwards
  q: 30              # minimum quality over the read after the clipping in order to keep and report the read
  default_clip_mode: 3  # clip from: front/end/both (5'/3'/both). Depending on the protocol.
  times: 1            # in cases of chimera reads/adapters, how many times should cutadapt try to remove am adapter-sequence
  ignore_fastqc_adapters: true    # by default ignore the detected adapters by FastQC. These tend to give false positive hits for smallRNA projects.
```

## Examine results

### Result files

- `counttables_smallrna.tinycap.tsv`
- `counttables_mirna.tinycap.tsv`


### Tested setups

The pipeline is designed and tested with sequences produced by: Illumina HiSeq 2000/2500, Illumina MiSeq. Both on single-end sequences.
Whenever a run is performed in Paired End mode, one should use the `R1` only. For analysis of (long) non-coding RNA, one should use `Gentrap`, this pipeline is optimized for Paired End RNA analysis.


Wetlab-Protocol: NEB SmallRNA kit and TruSeq SmallRNA kits were used for the data generated to test this pipeline.


## References

- [Cutadapt](https://github.com/marcelm/cutadapt)
- [HTSeqCount](http://www-huber.embl.de/HTSeq/doc/overview.html)
- [Bowtie1](http://bowtie-bio.sourceforge.net/index.shtml)

