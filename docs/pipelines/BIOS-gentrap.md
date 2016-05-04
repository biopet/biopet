# BIOS gentrap expression measures

In the BIOS pipeline we have multiple count files available.

## BIOS base counts

**DIR_name:** biosbasecounts

Inside this folder one will find a sub directory for each sample, containing 3 files.

### BIOS base raw counts

**File_name:** <SampleName>.non_stranded.raw.counts

This file contains all the values needed to create the actual base exon counts and base gene counts.

* Column1: chromosome name
* Column2: Start position
* Column3: End position
* Column4: Base counts
* Column5: Base counts divided by the length (stop - start)

### BIOS base exon counts

**File_name:** <SampleName>.non_stranded.exon.counts

* Column1: GeneID plus exon locations
* Column2: Base counts per exon

### BIOS base gene counts

**File_name:** <SampleName>.non_stranded.gene.counts

* Column1: GeneID
* Column2: Base counts per gene

# HTseq fragment counts

**DIR_name:** fragmentspergene

Inside this folder one will find:

* <SampleName>.fragmentspergene.counts
* fragmentspergene.fragments_per_gene.tsv which is the combined table of all separate count files
