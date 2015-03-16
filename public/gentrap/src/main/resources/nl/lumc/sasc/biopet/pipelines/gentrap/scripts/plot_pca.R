#!/usr/bin/env Rscript
#
# Script for plotting PCA plots for the Gentrap pipeline


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
usePackage("ggplot2")
usePackage("gplots")
usePackage("grid")
usePackage("jsonlite")
usePackage("reshape2")
usePackage("MASS")
usePackage("RColorBrewer")

# create spec for arg parsing
spec <- matrix(c(
      # input table (merge of all samples)
      'input-table',   'I', 1, 'character',
      # output plot file
      'output-plot',   'O', 1, 'character',
      # perform TMM-normalization (only if we are dealing with count data)
      'tmm-normalize', 'T', 0, 'logical'
      # help
      'help',          'H', 0, 'logical'
), byrow=TRUE, ncol=4)
opt <- getopt(spec)

# print help if requested
if (!is.null(opt[['help']])) {
  cat(getopt(spec, usage=TRUE))
  q(status=1)
}
