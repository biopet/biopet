
args <- commandArgs()
#Script taken from  http://bioinfo-out.curie.fr/projects/freec/tutorial.html and modified for biopet

dataTable <-read.table(args[4], header=TRUE);
BAF<-data.frame(dataTable)

png(filename = args[5], sep = ""), width = 1180, height = 1180,
    units = "px", pointsize = 20, bg = "white", res = NA)
plot(1:10)
op <- par(mfrow = c(5,5))

for (i in c(1:22,'X','Y')) {
    tt <- which(BAF$Chromosome==i)
    if (length(tt)>0){
    lBAF <-BAF[tt,]
    plot(lBAF$Position,lBAF$BAF,ylim = c(-0.1,1.1),xlab = paste ("position, chr",i),ylab = "BAF",pch = ".",col = colors()[1])

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
