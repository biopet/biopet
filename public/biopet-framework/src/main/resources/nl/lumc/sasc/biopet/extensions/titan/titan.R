# Titan runner for Biopet
# by wyleung 07-05-2015
# modified example from: http://compbio.bccrc.ca/software/titan/titan-running-titancna/


option_list <- list(
    make_option(c("-i", "--input"), dest="input"),
    make_option(c("-o", "--output"), dest="output"),
    make_option(c("-t", "--threads"), default=1, type="integer", dest="threads"),
    make_option(c("-c", "--clusters"), default=1, type="integer", dest="clusters"),
    make_option(c("-n", "--copynumbers"), default=1, type="integer", dest="copynumbers"),
    make_option(c("-t", "--threads"), default=1, type="integer", dest="threads"),

    make_option(c("-p", "--ploidy"), default=2, type="integer", dest="ploidy"),
    make_option(c("-q", "--normalproportion"), default=0.5, type="integer", dest="normalproportion")
    )

parser <- OptionParser(usage = "%prog [options] file", option_list=option_list)
opt = parse_args(parser)


library(TitanCNA)
library(doMC)
registerDoMC()
options(cores=opt$threads)

library(TitanCNA)

library(doMC)
registerDoMC()
options(cores=16)

#
#infile <- "/data/DIV5/SASC/project-109-hettum/data/AL12255-H0G11ALXX.merge.allels.tsv"
#tumWig <- "/data/DIV5/SASC/project-109-hettum/data/AL12255-H0G11ALXX.merge.dedup.reads.wig"
#
#infile <- "/data/DIV5/SASC/project-109-hettum/data/P0139-H0E4AALXX.merge.allels.tsv"
#tumWig <- "/data/DIV5/SASC/project-109-hettum/data/P0139-H0E4AALXX.merge.dedup.reads.wig"
#
#infile <- "/data/DIV5/SASC/project-109-hettum/data/P0141-H0E4AALXX.merge.allels.tsv"
#tumWig <- "/data/DIV5/SASC/project-109-hettum/data/P0141-H0E4AALXX.merge.dedup.reads.wig"


data <- loadAlleleCounts(infile, genomeStyle = "UCSC")
numClusters <- 2
params <- loadDefaultParameters(copyNumber=5, numberClonalClusters=numClusters, symmetric=TRUE)
params$normalParams$n_0 <- 0.5    #set initial normal proportion to 0.5
params$ploidyParams$phi_0 <- 2    #set initial ploidy to 2 (diploid)


normWig <- "/data/DIV5/SASC/project-109-hettum/data/PILS013-H0E4AALXX.merge.dedup.reads.wig"
gc <- "/data/DIV5/SASC/project-109-hettum/data/hg19.1kb.gc.wig"
map <- "/data/DIV5/SASC/project-109-hettum/data/hg19.1kb.map.wig"

cnData <- correctReadDepth(tumWig,normWig,gc,map, genomeStyle = "UCSC")
logR <- getPositionOverlap(data$chr,data$posn,cnData)
data$logR <- log(2^logR)
rm(logR,cnData)

chromosomes <- unique(data$chr)
chromosomes <- chromosomes[ chromosomes != "chrY" ]
chromosomes <- chromosomes[ chromosomes != "chrM" ]

workdata <- filterData(data,chromosomes,minDepth=10,maxDepth=200)

mScore <- as.data.frame(wigToRangedData(map))
mScore <- getPositionOverlap(data$chr,data$posn,mScore[,-4])
workdata <- filterData(workdata,chromosomes,minDepth=10,maxDepth=200,map=mScore,mapThres=0.8)

options(cores=16)
convergeParams <- runEMclonalCN(workdata,gParams=params$genotypeParams,nParams=params$normalParams,
                                pParams=params$ploidyParams,sParams=params$cellPrevParams,
                                maxiter=20,maxiterUpdate=1500,txnExpLen=1e12,txnZstrength=1e5,
                                useOutlierState=FALSE,
                                normalEstimateMethod="map",estimateS=TRUE,estimatePloidy=TRUE)

options(cores=1)
optimalPath <- viterbiClonalCN(workdata,convergeParams)


outfile <- paste("test_cluster0",numClusters,"_titan.txt",sep="")
results <- outputTitanResults(workdata,convergeParams,optimalPath,filename=outfile,posteriorProbs=F)

outfile <- paste("test_subclones0",numClusters,"_titan.txt",sep="")
results <- outputTitanResults(workdata,convergeParams,optimalPath,
                                     filename=outfile,posteriorProbs=FALSE,subcloneProfiles=TRUE)

outparam <- paste("test_cluster0",numClusters,"_params.txt",sep="")
outputModelParameters(convergeParams,results,outparam)


for( chr in chromosomes ) {
    norm <- convergeParams$n[length(convergeParams$n)]
    ploidy <- convergeParams$phi[length(convergeParams$phi)]
    library(SNPchip)  ## use this library to plot chromosome idiogram (optional)
    png(outplot,width=1200,height=1000,res=100)
    par(mfrow=c(3,1))
    plotCNlogRByChr(results, chr, ploidy=ploidy, geneAnnot=NULL, spacing=4,ylim=c(-4,6),cex=0.5,main=chr)
    plotAllelicRatio(results, chr, geneAnnot=NULL, spacing=4, ylim=c(0,1),cex=0.5,main=chr)
    plotClonalFrequency(results, chr, normal=tail(convergeParams$n,1), geneAnnot=NULL, spacing=4,ylim=c(0,1),cex=0.5,main=chr)
    if (as.numeric(numClusters) <= 2){
           ## NEW IN V1.2.0 ##
           ## users can choose to plot the subclone copy number profiles for <= 2 clusters
	    plotSubcloneProfiles(results, chr, cex = 2, spacing=6,main=chr)
    }
    pI <- plotIdiogram(chr,build="hg19",unit="bp",label.y=-4.25,new=FALSE,ylim=c(-2,-1))
    dev.off()


}







