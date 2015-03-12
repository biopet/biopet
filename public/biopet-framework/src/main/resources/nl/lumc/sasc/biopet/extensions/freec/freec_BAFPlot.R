library('optparse')
# Script taken from  http://bioinfo-out.curie.fr/projects/freec/tutorial.html and modified for biopet

option_list <- list(
    make_option(c("-i", "--input"), dest="input"),
    make_option(c("-o", "--output"), dest="output")
    )

parser <- OptionParser(usage = "%prog [options] file", option_list=option_list)
opt = parse_args(parser)


#
# Load Data
#

dataTable <-read.table(opt$input, header=TRUE);
BAF<-data.frame(dataTable)
chromosomes <- levels(dataTable$Chromosome)
ppi <- 300
plot_margins <- c(3,4,1,2)+0.1
label_positions <- c(2,0.5,0)


png(filename = opt$output, width = 16 * ppi, height = 10 * ppi,
    res=ppi, bg = "white")
par(mfrow = c(6,4))
par(mar=plot_margins)
par(mgp=label_positions)


for (i in chromosomes) {
    tt <- which(BAF$Chromosome==i)
    if (length(tt)>0){
    lBAF <-BAF[tt,]
    plot(lBAF$Position,
        lBAF$BAF,
        ylim = c(-0.1,1.1),
        xlab = paste ("position, chr",i),
        ylab = "BAF",
        pch = ".",
        col = colors()[1])

    tt <- which(lBAF$A==0.5)
    points(lBAF$Position[tt],lBAF$BAF[tt],pch = ".",col = colors()[92])
    tt <- which(lBAF$A!=0.5 & lBAF$A>=0)
    points(lBAF$Position[tt],lBAF$BAF[tt],pch = ".",col = colors()[62])
    tt <- 1
    pres <- 1

    if (length(lBAF$A)>4) {
        for (j in c(2:(length(lBAF$A)-pres-1))) {
            if (lBAF$A[j]==lBAF$A[j+pres]) {
                tt[length(tt)+1] <- j
            }
        }
        points(lBAF$Position[tt],lBAF$A[tt],pch = ".",col = colors()[24],cex=4)
        points(lBAF$Position[tt],lBAF$B[tt],pch = ".",col = colors()[24],cex=4)
    }

    tt <- 1
    pres <- 1
    if (length(lBAF$FittedA)>4) {
        for (j in c(2:(length(lBAF$FittedA)-pres-1))) {
            if (lBAF$FittedA[j]==lBAF$FittedA[j+pres]) {
                tt[length(tt)+1] <- j
            }
        }
        points(lBAF$Position[tt],lBAF$FittedA[tt],pch = ".",col = colors()[463],cex=4)
        points(lBAF$Position[tt],lBAF$FittedB[tt],pch = ".",col = colors()[463],cex=4)
    }

   }

}
dev.off()
