#!/usr/bin/env Rscript

# aggr_base_count.R
#
# Given a count file, write tab-delimited file(s) aggregating the counts
# at gene and/or exon level.
#
# (c) 2013 by Wibowo Arindrarto [LUMC - SASC]
# Adapted from Peter-Bram 't Hoen's script: 'merge_table_script_shark_PH3.r'


## FLAGS ##
LEVELS <- c('gene', 'exon')
OUT.DIR <- getwd()
DEBUG <- FALSE
if (DEBUG) {
  message("## DEBUG MODE ##")
}


## FUNCTIONS ##
CheckCountFiles <- function(count.files, DEBUG=FALSE) {
  # Given a vector of sample names, checks whether the .count files exist.
  #
  # Count files are the input files used to analyze the RNA-Seq expression
  # levels. They must conform to the following file name:
  # '{sample_name}/{sample_name}.count'
  #
  # Args:
  #   - paths: string vector of file paths
  for (cfile in count.files) {

    if (!file.exists(cfile)) {
      stop(paste("Path '", cfile, "' does not exist. Exiting.", sep=""))
    }

    if (DEBUG) {
      message("Path '", cfile, "' exists.", sep="")
    }
  }
}

CountBaseExons <- function(count.files, count.names,
                            col.header=c("gene", "chr", "start", "stop")) {
  # Given a list of count files, return a data frame containing their values.
  #
  # The count files must be a tab-separate file containing the following
  # columns in the same order:
  #   1. chromosome
  #   2. start position
  #   3. stop position
  #   4. total nucleotide counts
  #   5. nucleotide counts per exon
  #   6. gene name
  #
  # The returned data frame has the following columns:
  #
  #   1. gene name
  #   2. chromosome
  #   3. start position
  #   4. stop position
  #   5... total nucleotide counts for each sample
  #
  # This function assumes that for all count files, the values of the first
  # three columns are the same for each row.
  #
  # Args:
  #   - count.files: string vector of count file paths
  #   - col.headers: string vector of default data frame output headers

  # given a count file path, extract its fourth column
  GetNucCount <- function(x) {
    read.table(x, as.is=TRUE)[4]
  }

  # initial data frame is from the first file
  exon.counts <- read.table(count.files[1], as.is=TRUE)
  exon.counts <- exon.counts[, c(6, 1:3, 4)]
  colnames(exon.counts)[1:5] <- append(col.header, count.names[1])

  if (length(count.files) > 1) {
    # why doesn't R provide a nice way to slice remaining items??
    remaining.files <- count.files[2: length(count.files)]
    remaining.names <- count.names[2: length(count.names)]
    # append all nucleotide counts from remaining files to exon.counts
    exon.counts <- cbind(exon.counts, lapply(remaining.files, GetNucCount))
    # and rename these columns accordingly
    end.idx <- 5 + length(remaining.files)
    colnames(exon.counts)[6:end.idx] <- remaining.names
  }

  return(exon.counts)
}

CountExons <- function(exon.df) {
  # Given a data frame containing exon counts, return a data frame consisting of
  # compacted exon counts.
  #
  # In a compacted exon count data frame, each exon has its own unique name
  # consisting of its gene source and its start-stop coordinates.
  #
  # Args:
  #     - exon.df: data frame of complete exon counts


  # create new data frame of the exon counts, concatenating gene name, and the
  # exon start-stop coordinates
  exon.dis.counts <- cbind(paste(paste(exon.df$gene, exon.df$start,
                                   sep=":"), exon.df$stop, sep="-"),
                           exon.df[5: length(exon.df)])
  colnames(exon.dis.counts)[1] <- "exon"
  counts.in.samples <- as.matrix(exon.dis.counts[2:ncol(exon.dis.counts)])
  exon.counts <- aggregate(counts.in.samples ~ exon, data=exon.dis.counts, FUN=sum,
                           na.rm=TRUE)
  colnames(exon.counts)[2:ncol(exon.counts)] <- colnames(counts.in.samples)

  return (exon.counts)
}

CountGenes  <- function(exon.df) {
  # Given a data frame containing exon counts, return a data frame of gene
  # counts.
  #
  # See CountBaseExons for the input data frame format.
  #
  # Args:
  #  - exon.df: data frame of complete exon counts

  # basically an aggregate of exon counts with the same gene name
  counts.in.samples <- as.matrix(exon.df[5:ncol(exon.df)])
  gene.counts <- aggregate(counts.in.samples ~ gene, data=exon.df, FUN=sum,
                           na.rm=TRUE)
  # first column is gene
  colnames(gene.counts)[2:ncol(gene.counts)] <- colnames(counts.in.samples)

  return(gene.counts)
}


# load package for arg parsing
library('getopt')

# create spec for arg parsing
spec <- matrix(c(
      # colon-separated paths to each count files
      'count-file',   'I', 1, 'character',
      # colon-separated paths of each count file label; order must be the same
      # as the count files
      'count-name',   'N', 1, 'character',
      # output file for gene level counts
      'gene-count',   'G', 1, 'character',
      # output file for exon level counts
      'exon-count',   'E', 1, 'character',
      # help
      'help',         'H', 0, 'logical'
), byrow=TRUE, ncol=4)
opt <- getopt(spec)

# print help if requested
if (!is.null(opt[['help']])) {
  cat(getopt(spec, usage=TRUE))
  q(status=1)
}

# we need gene-count and/or exon-count flag
if (is.null(opt[['gene-count']]) & is.null(opt[['exon-count']])) {
  message("Error: Either '--gene-count' and/or '--exon-count' must have a value.")
  q(status=1)
}

# set fallback values for optional args
if (!is.null(opt[['output-dir']])) {
  OUT.DIR <- normalizePath(opt[['output-dir']])
  # create directory if it doesn't exist
  dir.create(OUT.DIR, showWarnings=FALSE)
}

# parse the input file paths and check their presence
if (!is.null(opt[['count-file']])) {
  count.files <- opt[['count-file']]
  count.files <- unlist(strsplit(gsub(' ', '', count.files), ':'))
  CheckCountFiles(count.files, DEBUG)
} else {
  stop("Required input count file path(s) not present. Exiting.")
}

# parse the input count labels and check if its length is the same as the input
# files
if (!is.null(opt[['count-name']])) {
  count.names <- opt[['count-name']]
  count.names <- unlist(strsplit(gsub(' ', '', count.names), ':'))
  if (length(count.names) != length(count.files)) {
    stop("Mismatched count file paths and labels. Exiting.")
  }
} else {
  stop("Required input count file label(s) not present. Exiting.")
}

# set output file name for gene counts
if (!is.null(opt[['gene-count']])) {
  gene.out <- opt[['gene-count']]
} else {
  gene.out <- NULL
}

# set output file name for exon counts
if (!is.null(opt[['exon-count']])) {
  exon.out <- opt[['exon-count']]
} else {
  exon.out <- NULL
}

# count base exons (complete with coordinates)
base.exon.counts <- CountBaseExons(count.files, count.names)

# and write output files, depending on the flags
if (!is.null(gene.out)) {
  gene.counts <- CountGenes(base.exon.counts)
  write.table(gene.counts, file = gene.out, sep = "\t", quote = FALSE, row.names = FALSE)
}
if (!is.null(exon.out)) {
  exon.counts <- CountExons(base.exon.counts)
  write.table(exon.counts, file = exon.out, sep = "\t", quote = FALSE, row.names = FALSE)
}
