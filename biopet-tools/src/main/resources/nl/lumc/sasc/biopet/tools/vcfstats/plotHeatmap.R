library('gplots')
library('RColorBrewer')

args <- commandArgs(TRUE)
inputArg <- args[1]
outputArg <- args[2]
outputArgClustering <- args[3]
outputArgDendrogram <- args[4]


heat<-read.table(inputArg, header = 1, sep= '\t', stringsAsFactors = F)
#heat[heat==1] <- NA
rownames(heat) <- heat[,1]
heat<- heat[,-1]
heat<- as.matrix(heat)

colNumber <- 50
col <- rev(colorRampPalette(brewer.pal(11, "Spectral"))(colNumber))
for (i in (colNumber+1):(colNumber+round((dist(range(heat)) - dist(range(heat[heat < 1]))) / dist(range(heat[heat < 1])) * colNumber))) {
    col[i] <- col[colNumber]
}
col[length(col)] <- "#00FF00"

png(file = outputArg, width = 1200, height = 1200)
heatmap.2(heat, trace = 'none', col = col, Colv=NA, Rowv=NA, dendrogram="none", margins = c(12, 12), na.color="#00FF00")
dev.off()

hc <- hclust(d = dist(heat))
png(file = outputArgDendrogram, width = 1200, height = 1200)
plot(as.dendrogram(hc), horiz=TRUE, asp=0.02)
dev.off()

png(file = outputArgClustering, width = 1200, height = 1200)
heatmap.2(heat, trace = 'none', col = col, Colv="Rowv", dendrogram="row",margins = c(12, 12), na.color="#00FF00")
dev.off()
