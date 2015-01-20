library('gplots')

args <- commandArgs(TRUE)
inputArg <- args[1]
outputArg <- args[2]
outputArgClustering <- args[3]

col <- heat.colors(250)
col[250] <- "#00FF00"

heat<-read.table(inputArg, header = 1, sep= '\t', stringsAsFactors = F)
rownames(heat) <- heat[,1]
heat<- heat[,-1]

heat<- as.matrix(heat)

png(file = outputArg, width = 1500, height = 1500)
heatmap.2(heat, trace = 'none', col = col, Colv=NA, Rowv=NA, dendrogram="none", margins = c(10, 10))
dev.off()


png(file = outputArgClustering, width = 1500, height = 1500)
heatmap.2(heat, trace = 'none', col = col, Colv="Rowv", dendrogram="row",margins = c(10, 10))
dev.off()