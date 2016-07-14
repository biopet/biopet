library('optparse')
library('naturalsort')

# Script taken from  http://bioinfo-out.curie.fr/projects/freec/tutorial.html and modified for biopet

option_list <- list(
    make_option(c("-p", "--ploidy"), default=2, type="integer", dest="ploidy"),
    make_option(c("-i", "--input"), dest="input"),
    make_option(c("-o", "--output"), dest="output")
    )

parser <- OptionParser(usage = "%prog [options] file", option_list=option_list)
opt = parse_args(parser)


#
# Load Data
#


dataTable <- read.table( opt$input , header=TRUE)
input_ratio <- data.frame(dataTable)

chromosomes <- naturalsort(levels(input_ratio$Chromosome))
input_ratio$Chromosome <- factor(input_ratio$Chromosome, levels=chromosomes, ordered=T)

sorted_ratio <- input_ratio[order(input_ratio$Chromosome),]
ratio <- input_ratio[order(input_ratio$Chromosome, input_ratio$Start),]

ploidy <- opt$ploidy
ppi <- 300
plot_margins <- c(3,4,1,2)+0.1
label_positions <- c(2,0.5,0)


maxLevelToPlot <- 3
for (i in c(1:length(ratio$Ratio))) {
	if (ratio$Ratio[i]>maxLevelToPlot) {
		ratio$Ratio[i]=maxLevelToPlot
	}
}

#
# Plot the graphs per chromosome
#

for (i in chromosomes) {

    png(filename = paste(opt$output, ".", i,".png",sep=""), width = 4 * ppi, height = 2.5 * ppi,
        res=ppi, bg = "white")
    par(mfrow = c(1,1))
    par(mar=plot_margins)
    par(mgp=label_positions)


    tt <- which(ratio$Chromosome==i)
    if (length(tt)>0) {
        plot(ratio$Start[tt],
                ratio$Ratio[tt]*ploidy,
                ylim = c(0,maxLevelToPlot*ploidy),
                xlab = paste ("position, chr",i),
                ylab = "normalized CN",
                pch = ".",
                col = colors()[88])

        title(outer=TRUE)
        tt <- which(ratio$Chromosome==i  & ratio$CopyNumber>ploidy )
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[136])

        tt <- which(ratio$Chromosome==i  & ratio$Ratio==maxLevelToPlot & ratio$CopyNumber>ploidy)
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[136],cex=4)

        tt <- which(ratio$Chromosome==i  & ratio$CopyNumber<ploidy & ratio$CopyNumber!= -1)
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[461])
        tt <- which(ratio$Chromosome==i)

        #UNCOMMENT HERE TO SEE THE PREDICTED COPY NUMBER LEVEL:
        #points(ratio$Start[tt],ratio$CopyNumber[tt], pch = ".", col = colors()[24],cex=4)
    }
    #tt <- which(ratio$Chromosome==i)

	#UNCOMMENT HERE TO SEE THE EVALUATED MEDIAN LEVEL PER SEGMENT:
	#points(ratio$Start[tt],ratio$MedianRatio[tt]*ploidy, pch = ".", col = colors()[463],cex=4)

    dev.off()
}

png(filename = paste(opt$output, ".png",sep=""), width = 16 * ppi, height = 10 * ppi,
    res=ppi, bg = "white")
par(mfrow = c(6,4))
par(mar=plot_margins)
par(mgp=label_positions)

for (i in chromosomes) {
    tt <- which(ratio$Chromosome==i)
    if (length(tt)>0) {
        plot(ratio$Start[tt],
                ratio$Ratio[tt]*ploidy,
                ylim = c(0,maxLevelToPlot*ploidy),
                xlab = paste ("position, chr",i),
                ylab = "normalized CN",
                pch = ".",
                col = colors()[88])

        tt <- which(ratio$Chromosome==i  & ratio$CopyNumber>ploidy )
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[136])

        tt <- which(ratio$Chromosome==i  & ratio$Ratio==maxLevelToPlot & ratio$CopyNumber>ploidy)
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[136],cex=4)

        tt <- which(ratio$Chromosome==i  & ratio$CopyNumber<ploidy & ratio$CopyNumber!= -1)
        points(ratio$Start[tt],ratio$Ratio[tt]*ploidy,pch = ".",col = colors()[461])
        tt <- which(ratio$Chromosome==i)

        #UNCOMMENT HERE TO SEE THE PREDICTED COPY NUMBER LEVEL:
        #points(ratio$Start[tt],ratio$CopyNumber[tt], pch = ".", col = colors()[24],cex=4)
	}
	#tt <- which(ratio$Chromosome==i)

	#UNCOMMENT HERE TO SEE THE EVALUATED MEDIAN LEVEL PER SEGMENT:
	#points(ratio$Start[tt],ratio$MedianRatio[tt]*ploidy, pch = ".", col = colors()[463],cex=4)

}

dev.off()



# Export the whole genome graph

png(filename = paste(opt$output, ".wg.png",sep=""), width = 50 * ppi, height = 10 * ppi,
res=ppi, bg = "white")

plot_margins <- c(3,4,2,2)+0.1
label_positions <- c(2,0.5,0)

par(mfrow = c(1,1))
par(mar=plot_margins)
par(mgp=label_positions)
par(xaxs="i", yaxs="i")


maxLevelToPlot <- 3
for (i in c(1:length(ratio$Ratio))) {
    if (ratio$Ratio[i]>maxLevelToPlot) {
        ratio$Ratio[i]=maxLevelToPlot
    }
}

for (i in c(1:length(ratio$Start))) {
    ratio$Position[i] = (i-1) *5000 +1
}


plotRatioLT <- 0.10

filteredSet <- ratio[ ratio$score > plotRatioLT, ]

plot(filteredSet$Position,
filteredSet$Ratio*ploidy,
ylim = c(0,maxLevelToPlot*ploidy),
xlab = paste ("Chr. on genome"),
ylab = "normalized CN",
pch = ".",
col = colors()[88])


title(outer=TRUE)
tt <- which(filteredSet$CopyNumber>ploidy)
points(filteredSet$Position[tt],filteredSet$Ratio[tt]*ploidy,pch = ".",col = colors()[136])

tt <- which(filteredSet$Ratio==maxLevelToPlot & filteredSet$CopyNumber>ploidy)
points(filteredSet$Position[tt],filteredSet$Ratio[tt]*ploidy,pch = ".",col = colors()[136],cex=4)

tt <- which(filteredSet$CopyNumber<ploidy & filteredSet$CopyNumber!= -1)
points(filteredSet$Position[tt],filteredSet$Ratio[tt]*ploidy,pch = ".",col = colors()[461], bg="black")


for (chrom in chromosomes) {
    tt <- which(filteredSet$Chromosome == chrom)
    print(filteredSet[tt[1],])
    xpos <- filteredSet$Position[tt][1]
    abline(v=xpos, col="grey")
    axis(3, at=xpos, labels=chrom , las=2)
}


dev.off()
