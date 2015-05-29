# Titan runner for Biopet
# by wyleung 07-05-2015
# modified example from: http://compbio.bccrc.ca/software/titan/titan-running-titancna/

library(TitanCNA)
library(SNPchip)  ## use this library to plot chromosome idiogram

library(doMC)
registerDoMC()


option_list <- list(
    make_option(c("-i", "--input"), dest="input_allels"),
    make_option(c("-w", "--inputwig"), dest="input_wig"),

    make_option(c("-c", "--normalwig"), dest="normal_wig"),

    make_option(c("-g", "--gcwig"), dest="gc_wig"),
    make_option(c("-m", "--mapwig"), dest="map_wig"),

    make_option(c("-o", "--output"), dest="output"),

    make_option(c("-t", "--threads"), default=1, type="integer", dest="threads"),


    make_option(c("-c", "--clusters"), default=1, type="integer", dest="clusters"),
    make_option(c("-n", "--copynumbers"), default=1, type="integer", dest="copynumbers"),

    make_option(c("-p", "--ploidy"), default=2, type="integer", dest="ploidy"),
    make_option(c("-q", "--normalproportion"), default=0.5, type="integer", dest="normalproportion")
    )

parser <- OptionParser(usage = "%prog [options] file", option_list=option_list)
opt = parse_args(parser)

## setup threaded
options(cores= opt$threads)

# config the input

infile <- opt$input_allels
tumWig <- opt$input_wig

normWig <- opt$normal_wig
gc      <- opt$gc_wig
map     <- opt$map_wig


params <- loadDefaultParameters(copyNumber=opt$copynumbers, numberClonalClusters=numClusters, symmetric=TRUE)
params$normalParams$n_0     <- opt$normalproportion     #set initial normal proportion to 0.5
params$ploidyParams$phi_0   <- opt$ploidy               #set initial ploidy to 2 (diploid)

numClusters <- opt$clusters


# load data
data <- loadAlleleCounts(infile, genomeStyle = "UCSC")

cnData <- correctReadDepth(tumWig,normWig,gc,map, genomeStyle = "UCSC")
logR <- getPositionOverlap(data$chr,data$posn,cnData)
data$logR <- log(2^logR)
rm(logR,cnData)

chromosomes <- unique(data$chr)
chromosomes <- chromosomes[ chromosomes != "chrY" ]
chromosomes <- chromosomes[ chromosomes != "chrM" ]
chromosomes <- chromosomes[ chromosomes != "Y" ]
chromosomes <- chromosomes[ chromosomes != "M" ]

#data$chr <- gsub("chr","", data$chr)
workdata <- filterData(data,chromosomes,minDepth=10,maxDepth=200)

#mScore <- as.data.frame(wigToRangedData(map))
#mScore <- getPositionOverlap(data$chr,data$posn,mScore[,-4])
#workdata <- filterData(workdata,chromosomes,minDepth=10,maxDepth=200,map=mScore,mapThres=0.8)

convergeParams <- runEMclonalCN(workdata,gParams=params$genotypeParams,
                                nParams=params$normalParams,
                                pParams=params$ploidyParams,
                                sParams=params$cellPrevParams,
                                maxiter=20,maxiterUpdate=1500,
                                txnExpLen=1e12,txnZstrength=1e5,
                                useOutlierState=FALSE,
                                normalEstimateMethod="map",
                                estimateS=TRUE,estimatePloidy=TRUE)

options(cores=1)
optimalPath <- viterbiClonalCN(workdata,convergeParams)


outfile <- paste(opt$output,"test_cluster0",numClusters,"_titan.txt",sep="")
results <- outputTitanResults(workdata,
                                convergeParams,
                                optimalPath,
                                filename=outfile,
                                posteriorProbs=F)

outfile <- paste(opt$output,"test_subclones0",numClusters,"_titan.txt",sep="")
results <- outputTitanResults(workdata,
                                convergeParams,
                                optimalPath,
                                filename=outfile,
                                posteriorProbs=FALSE,
                                subcloneProfiles=TRUE)

outparam <- paste(opt$output,"test_cluster0",numClusters,"_params.txt",sep="")
outputModelParameters(convergeParams,
                        results,
                        outparam)


for( chr in chromosomes ) {
    outplot <- paste(opt$output,"titan_",chr,".png",sep="")

    norm <- convergeParams$n[length(convergeParams$n)]
    ploidy <- convergeParams$phi[length(convergeParams$phi)]

    png(file=outplot, width=1200, height=1000)
#    pdf(file=outplot, paper="a4")
    par(mfrow=c(4,1))
    par(omi = rep(.5, 4))                      ## 1/2 inch outer margins

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