library('gplots')

args <- commandArgs(TRUE)
inputArg <- args[1]
outputArg <- args[2]
outputArgClustering <- args[3]

col <- heat.colors(250)
col[250] <- "#00FF00"

heat<-read.table(inputArg, header = 1, sep= '\t')
rownames(heat) <- heat[,1]
heat<- heat[,-1]

heat<- as.matrix(heat)

png(file = outputArg, width = 1000, height = 1000)
heatmap.2(heat, trace = 'none', col = col, Colv=NA, Rowv=NA, dendrogram="none")
dev.off()


png(file = outputArgClustering, width = 1000, height = 1000)
heatmap.2(heat, trace = 'none', col = col, Colv="Rowv", dendrogram="row")
dev.off()
