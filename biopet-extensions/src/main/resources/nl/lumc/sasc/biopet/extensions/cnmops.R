#!/usr/bin/env Rscript
suppressWarnings(suppressPackageStartupMessages(library('cn.mops')))
suppressWarnings(suppressPackageStartupMessages(library('optparse')))

# Script from  https://git.lumc.nl/lgtc-bioinformatics/gapss3/blob/master/src/CNV/makeCnmops.sh
# modified to take arguments

option_list <- list(
    make_option(c("--rawoutput"), dest="rawoutput"),
    make_option(c("--cnv"), dest="cnv"),
    make_option(c("--cnr"), dest="cnr"),
    make_option(c("--chr"), dest="chr"),
    make_option(c("--threads"), dest="threads", default=8, type="integer"),
    make_option(c("--wl"), dest="wl", default=1000, type="integer"),
    make_option(c("--version"), action="store_true", default=FALSE)
    )

parser <- OptionParser(usage = "%prog [options] file", option_list=option_list)
arguments = parse_args(parser, positional_arguments=TRUE)
opt = arguments$options
args = arguments$args

if (opt$version) {
    cat(toString(packageVersion('cn.mops')))
    quit("no")
}

chromosome <- opt$chr
CNVoutput <- opt$cnv
CNRoutput <- opt$cnr
windowLength <- opt$wl
bamFile <- args

BAMFiles <- strsplit(c(bamFile), " ")[[1]]
bamDataRanges <- tryCatch(
    {
        getReadCountsFromBAM(BAMFiles, mode="paired", refSeqName=chromosome, WL=windowLength, parallel=opt$threads)
    },
    error = function(e) {
        quit("no")
    },
    warning = function(w) {
        quit("no")
    },
    finally = {

    }
)

write.table(as.data.frame( bamDataRanges ), quote = FALSE, opt$rawoutput, row.names=FALSE)

res <- cn.mops(bamDataRanges)
res <- calcIntegerCopyNumbers(res)

write.table(as.data.frame(cnvs(res)), quote = FALSE, CNVoutput, row.names=FALSE)
write.table(as.data.frame(cnvr(res)), quote = FALSE, CNRoutput, row.names=FALSE)


ppi <- 300
plot_margins <- c(3,4,1,2)+0.1
label_positions <- c(2,0.5,0)

dir.create(chromosome, showWarnings=FALSE, recursive=TRUE, mode="0744")

# Plot chromosome per sample.
for ( i in 1:length(BAMFiles)){
  png(file=paste(dirname(opt$rawoutput),"/",chromosome,"-segplot-",i,".png", sep=""),
  width = 16 * ppi, height = 10 * ppi,
    res=ppi, bg = "white"
  )
    par(mfrow = c(1,1))
    par(mar=plot_margins)
    par(mgp=label_positions)
  segplot(res,sampleIdx=i)
  dev.off()
}

# Plot cnvr regions.
for ( i in 1:nrow(as.data.frame(cnvr(res)))) {
  png(file=paste(dirname(opt$rawoutput),"/",chromosome,"-cnv-",i,".png",sep=""),
  width = 16 * ppi, height = 10 * ppi,
    res=ppi, bg = "white")
    par(mfrow = c(1,1))
    par(mar=plot_margins)
    par(mgp=label_positions)
  plot(res,which=i,toFile=TRUE)
  dev.off()
}

