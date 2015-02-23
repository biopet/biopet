# Script taken from  http://bioinfo-out.curie.fr/projects/freec/tutorial.html and modified for biopet

#
#args <- commandArgs(TRUE)
#chromosome <- args[1]
#ploidy <- args[2]
#input <- args[3]
#output <- args[4]


args <- commandArgs()

dataTable <-read.table(args[5], header=TRUE);

ratio<-data.frame(dataTable)
ploidy <- type.convert(args[4])

png(filename = paste(args[5],".png",sep = ""), width = 1180, height = 1180,
    units = "px", pointsize = 20, bg = "white", res = NA)
plot(1:10)
op <- par(mfrow = c(5,5))

maxLevelToPlot <- 3
for (i in c(1:length(ratio$Ratio))) {
	if (ratio$Ratio[i]>maxLevelToPlot) {
		ratio$Ratio[i]=maxLevelToPlot;
	}
}

for (i in c(1:22,'X','Y')) {
	tt <- which(ratio$Chromosome==i)
	if (length(tt)>0) {
	 plot(ratio$Start[tt],ratio$Ratio[tt]*ploidy,ylim = c(0,maxLevelToPlot*ploidy),xlab = paste ("position, chr",i),ylab = "normalized copy number profile",pch = ".",col = colors()[88])
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
	tt <- which(ratio$Chromosome==i)
	
	#UNCOMMENT HERE TO SEE THE EVALUATED MEDIAN LEVEL PER SEGMENT:
	#points(ratio$Start[tt],ratio$MedianRatio[tt]*ploidy, pch = ".", col = colors()[463],cex=4)
	
}

dev.off()