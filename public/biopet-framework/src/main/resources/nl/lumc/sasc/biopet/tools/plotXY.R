library('ggplot2')
library('reshape2')

args <- commandArgs(TRUE)
inputArg <- args[1]
outputArg <- args[2]

tsv<-read.table(inputArg, header = 1, sep= '\t', stringsAsFactors = F)

data <- melt(tsv)

data$X <- as.numeric(data$X)
data <- na.omit(data)
data <- data[data$value > 0,]

print("Starting to plot")
png(file = outputArg, width = 1500, height = 1500)
ggplot(data, aes(x=X, y=value, color=variable, group=variable)) + geom_line()
dev.off()
print("plot done")
