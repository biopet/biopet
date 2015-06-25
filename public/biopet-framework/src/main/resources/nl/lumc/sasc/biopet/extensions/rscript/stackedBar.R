library(reshape2)
library(ggplot2)
library(argparse)

parser <- ArgumentParser(description='Process some integers')
parser$add_argument('--input', dest='input', type='character', help='Input tsv file', required=TRUE)
parser$add_argument('--output', dest='output', type='character', help='Output png file', required=TRUE)
parser$add_argument('--width', dest='width', type='integer', default = 500)
parser$add_argument('--height', dest='height', type='integer', default = 500)
parser$add_argument('--xlabel', dest='xlabel', type='character')
parser$add_argument('--ylabel', dest='ylabel', type='character', required=TRUE)
parser$add_argument('--llabel', dest='llabel', type='character')
parser$add_argument('--title', dest='title', type='character')

arguments <- parser$parse_args()

png(filename = arguments$output, width = arguments$width, height = arguments$height)

DF <- read.table(arguments$input, header=TRUE)

if (is.null(arguments$xlabel)) xlab <- colnames(DF)[1] else xlab <- arguments$xlabel

colnames(DF)[1] <- "Rank"

DF1 <- melt(DF, id.var="Rank")

ggplot(DF1, aes(x = Rank, y = value, fill = variable)) + 
  xlab(xlab) +
  ylab(arguments$ylabel) + 
  guides(fill=guide_legend(title=arguments$llabel)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1, size = 8)) +
  geom_bar(stat = "identity", width=1) +
  ggtitle(arguments$title)

dev.off()
