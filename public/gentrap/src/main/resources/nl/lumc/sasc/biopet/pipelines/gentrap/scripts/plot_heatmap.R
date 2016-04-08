#!/usr/bin/env Rscript
#
# Script for plotting heatmap for the Gentrap pipeline


# General function to install package if it does not exist
# Otherwise, it only loads the package
usePackage <- function(p) {
    r <- getOption("repos")
    r["CRAN"] <- "http://cran.us.r-project.org"
    options(repos = r)
    rm(r)
    if (!is.element(p, installed.packages()[,1]))
        install.packages(p, dep = TRUE)
    require(p, character.only = TRUE)
}

usePackage("getopt")
usePackage("edgeR")
usePackage("gplots")
usePackage("RColorBrewer")

# create spec for arg parsing
spec <- matrix(c(
      # input table (merge of all samples)
      "input-table",   "I", 1, "character",
      # output plot file
      "output-plot",   "O", 1, "character",
      # count type
      "count-type",    "C", 1, "character",
      # use log2 values for htseq cpm or not
      "use-log",       "L", 0, "logical",
      # perform TMM-normalization (only if we are dealing with count data)
      "tmm-normalize", "T", 0, "logical",
      # help
      "help",          "H", 0, "logical"
), byrow=TRUE, ncol=4)
opt <- getopt(spec)

# print help if requested
if (!is.null(opt[["help"]])) {
  cat(getopt(spec, usage=TRUE))
  q(status=0)
}

# make sure all required arguments are defined
if (is.null(opt[["input-table"]]) || is.null(opt[["output-plot"]])) {
  cat(getopt(spec, usage=TRUE))
  q(status=1)
}

INPUT.PATH <- opt[["input-table"]]
OUTPUT.PLOT <- opt[["output-plot"]]
COUNT.TYPE <- if (is.null(opt[["count-type"]])) { "FragmentsPerGene" } else { opt[["count-type"]] }
TMM.NORM <- if (is.null(opt[["tmm-normalize"]])) { FALSE } else { TRUE }
USE.LOG <- if (is.null(opt[["use-log"]])) { FALSE } else { TRUE }

HTSEQ.STATS <- c("__alignment_not_unique", "__ambiguous", "__no_feature", "__not_aligned", "__too_low_aQual")
# NOTE: These are string representations of the enums in the Gentrap Biopet pipeline
VALID.COUNT.TYPES <- c("FragmentsPerGene", "FragmentsPerExon", "BasesPerGene", "BasesPerExon",
                       "CufflinksGene", "CufflinksIsoform")

# make sure count type is valid
if (!(COUNT.TYPE %in% VALID.COUNT.TYPES)) {
  cat("Invalid count-type ", COUNT.TYPE, sep="", file=stderr())
  q(status=2)
}

# Function for parsing and formatting input table to something we can use
prepTable <- function(input.path, count.type=COUNT.TYPE, tmm.normalize=TMM.NORM, use.log=USE.LOG) {
  parsed <- read.table(input.path, header=TRUE, sep="\t", stringsAsFactors=FALSE)
  # remove htseq-count stat rows, if exist
  is.count <- count.type %in% c("FragmentsPerGene", "FragmentsPerExon", "BasesPerGene", "BasesPerExon")
  if (is.count) {
    parsed <- parsed[!parsed[[colnames(parsed)[1]]] %in% HTSEQ.STATS,]
  }
  # set gene IDs as row names
  rownames(parsed) <- parsed[,1]
  parsed[1] <- NULL

  if (is.count) {
    dge <- DGEList(parsed)
    if (tmm.normalize) {
      dge <- calcNormFactors(dge)
    }
    cpm(dge, log=USE.LOG)
  } else {
    parsed
  }
}

# Function for plotting into a heatmap output
plotHeatmap <- function(in.data, out.name=OUTPUT.PLOT, count.type=COUNT.TYPE, tmm.normalize=TMM.NORM) {

  var.title <-
    if (count.type == "FragmentsPerGene") {
      "Fragments per Gene (CPM)"
    } else if (count.type == "FragmentsPerExon") {
      "Fragments per Exon (CPM)"
    } else if (count.type == "BasesPerGene") {
      "Bases per Gene (CPM)"
    } else if (count.type == "BasesPerExon") {
      "Bases per Exon (CPM)"
    } else if (count.type == "CufflinksGene") {
      "Cufflinks - Gene (FPKM)"
    } else if (count.type == "CufflinksIsoform") {
      "Cufflinks - Isoform (FPKM)"
    }
  title <-
    if (tmm.normalize) {
      paste("Spearman Correlation of\n", var.title, " (TMM-normalized)", sep="")
    } else {
      paste("Spearman Correlation of\n", var.title, sep="")
    }

  if (nrow(in.data) > 20) {
    img.len <- 1200
    img.margin <- 8
  } else {
    img.len <- 800
    img.margin <- min(16, max(sapply(rownames(in.data), nchar)))
}

  png(out.name, height=img.len, width=img.len, res=100)
  heatmap.2(in.data, col=brewer.pal(9, "YlGnBu"), trace="none", density.info="histogram", main=title, margins=c(img.margin, img.margin))
  dev.off()
}

plotPlaceholder <- function(text.display, out.name=OUTPUT.PLOT) {
  png(out.name, height=800, width=800, res=100)
  par(mar=c(0,0,0,0))
  plot(c(0, 1), c(0, 1), ann=F, bty='n', type='n', xaxt='n', yaxt='n')
  text(x=0.5, y=0.5, paste(text.display),
       cex=1.6, col="black")
  dev.off()
}


parsed <- tryCatch(
    prepTable(INPUT.PATH),
    error=function (e) {
        plotPlaceholder(text.display="Error occured during table prep")
        q(status=0)
    })

if (nrow(parsed) > 0) {
  cors <- cor(parsed, method="spearman")
  tryCatch(plotHeatmap(cors), error=function (e) plotPlaceholder(text.display="Error occured during plotting"))
} else {
  plotPlaceholder(text.display="Not enough data points for plotting")
}
