# Introduction

``TinyCap`` is an analysis pipeline meant the process smallRNA captures. We use a fixed aligner in this pipeline: `bowtie` .
By default, we allow one fragment to align up to 5 different locations on the genome. In most of the cases, the shorter the sequence, the less 'unique' the pattern is. Multiple "best" alignments is in these cases possible.
To not create a 'first-occurence found and align-to' bias towards the reference genome, we allow the aligner to report more alignment positions.

After alignment, `htseq-count` is responsible for the quantification of transcripts. One should supply 2 annotation-files for this to happen:

- mirBase GFF3 file with all annotated and curated miRNA for the genome of interest. [visit mirBase](http://www.mirbase.org/ftp.shtml)
- Ensembl (Gene sets) in GTF format. [visit Ensembl](http://www.ensembl.org/info/data/ftp/index.html) 

Count tables are generated per sample and and aggregation per (run)project is created in the top level folder of the project.


# Starting the pipeline

biopet pipelines tinycap [options] \
-config `<path-to>/settings_tinycap.json`
-config `<path-to>/sample_sheet.json` \
-l DEBUG \
-qsub \
-jobParaEnv BWA \
-run


# Example
Note that one should first create the appropriate [configs](../general/config.md).

The pipeline specific (minimum) config looks like:

```
{
    "output_dir": "<path-to>/outputdirectory",
    "reference_name": "GRCh38",
    "species": "H.sapiens",
    "annotation_gff": "<path-to>/data/annotation/mirbase-21-hsa.gff3",
    "annotation_refflat": "<path-to>/data/annotation/ucsc_ensembl_83_38.refFlat",
    "annotation_gtf": "<path-to>/data/annotation/ucsc_ensembl_83_38.gtf"
}
```

One can specify other options such as: `bowtie` (alignment) options, clipping and trimming options `sickle` and `cutadapt`.

Example:

```
    "bowtie": {
      "chunkmbs": 256,  # this is a performance option, keep it high (256) as many alternative alignments are possible
      "seedmms": 3,
      "seedlen": 25,
      "k": 5,           # take and report best 5 alignments 
      "best": true      # sort by best hit
    },
    "sickle": {
      "lengthThreshold": 8  # minimum length to keep after trimming
    },
    "cutadapt": {
      "error_rate": 0.2,    # recommended: 0.2, allow more mismatches in adapter to be clipped of (ratio)
      "minimum_length":  8, # minimum length to keep after clipping, setting lower will cause multiple alignments afterwards
      "q": 30,              # minimum quality over the read after the clipping in order to keep and report the read
      "default_clip_mode": "both",  # clip from: front/end/both (5'/3'/both). Depending on the protocol. Setting `both` makes clipping take more time, but is safest to do on short sequences such as smallRNA.
      "times": 2            # in cases of chimera reads/adapters, how many times should cutadapt try to remove am adapter-sequence
    )
```

# Examine results

## Result files

- `counttables_smallrna.tinycap.tsv`
- `counttables_mirna.tinycap.tsv`


## Best practice

# References
