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
parser$add_argument('--removeZero', dest='removeZero', type='character', default="false")
parser$add_argument('--xLog10', dest='xLog10', type='character', default="false")
parser$add_argument('--yLog10', dest='yLog10', type='character', default="false")

parser$add_argument('--xLog10Breaks', dest='xLog10Breaks', nargs='+', type='integer')
parser$add_argument('--xLog10Labels', dest='xLog10Labels', nargs='+', type='character')

arguments <- parser$parse_args()

png(filename = arguments$output, width = arguments$width, height = arguments$height)

DF <- read.table(arguments$input, header=TRUE)

if (is.null(arguments$xlabel)) xlab <- colnames(DF)[1] else xlab <- arguments$xlabel

colnames(DF)[1] <- "Rank"

DF1 <- melt(DF, id.var="Rank")

if (arguments$removeZero == "true") DF1 <- DF1[DF1$value > 0, ]
if (arguments$removeZero == "true") print("Removed 0 values")

plot = ggplot(DF1, aes(x = Rank, y = value, group = variable, color = variable)) +
  xlab(xlab) +
  ylab(arguments$ylabel) +
  guides(color=guide_legend(title=arguments$llabel)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1, size = 8)) +
  ggtitle(arguments$title) +
  theme_bw() +
  geom_line()


if (arguments$xLog10 == "true") {
  if (!is.null(arguments$xLog10Labels)) {
    scale_x <- scale_x_log10(breaks = arguments$xLog10Breaks, labels=arguments$xLog10Labels)
  } else {
    scale_x <- scale_x_log10()
  }
  plot <- plot + scale_x
}
if (arguments$yLog10 == "true") {
  plot <- plot + scale_y_log10()
}

plot

dev.off()
