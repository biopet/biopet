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

# from 1 - 5
numClusters <- opt$clusters

# from 5 - 8
copyNumber <- opt$copynumbers

params <- loadDefaultParameters(copyNumber=copyNumber ,numberClonalClusters=numClusters)

params$normalParams$n_0 <- 0.5    #set initial normal proportion to 0.5

params$ploidyParams$phi_0 <- opt$ploidy    #set initial ploidy to 2 (diploid)
# params$ploidyParams$phi_0 <- 4    #set initial ploidy to 4 (tetraploid)

extractAlleleReadCounts(bamFile, bamIndex, positions, outputFilename = NULL, pileupParam = PileupParam())

id <- "test"
infile <- system.file("extdata", "test_alleleCounts_chr2.txt", package = "TitanCNA")
tumWig <- system.file("extdata", "test_tum_chr2.wig", package = "TitanCNA")
normWig <- system.file("extdata", "test_norm_chr2.wig", package = "TitanCNA")
gc <- system.file("extdata", "gc_chr2.wig", package = "TitanCNA")
map <- system.file("extdata", "map_chr2.wig", package = "TitanCNA")

data <- loadAlleleCounts(infile, genomeStyle = "UCSC")
cnData <- correctReadDepth(tumWig,normWig,gc,map, genomeStyle = "UCSC")

logR <- getPositionOverlap(data$chr,data$posn,cnData)
data$logR <- log(2^logR)
rm(logR,cnData)

mScore <- as.data.frame(wigToRangedData(map))
mScore <- getPositionOverlap(data$chr,data$posn,mScore[,-4])
data <- filterData(data,c(1:22,"X"),minDepth=10,maxDepth=200,map=mScore,mapThres=0.8)

convergeParams <- runEMclonalCN(data,gParams=params$genotypeParams,nParams=params$normalParams,
                                pParams=params$ploidyParams,sParams=params$cellPrevParams,
                                maxiter=20,maxiterUpdate=1500,txnExpLen=1e15,txnZstrength=1e5,
                                useOutlierState=FALSE,
                                normalEstimateMethod="map",estimateS=TRUE,estimatePloidy=TRUE)



convergeParams$txnExpLen <- 1e12
optimalPath <- viterbiClonalCN(data,convergeParams)

outfile <- paste("test_cluster0",numClusters,"_titan.txt",sep="")
results <- outputTitanResults(data,convergeParams,optimalPath,filename=outfile,posteriorProbs=F)
results <- outputTitanResults(data,convergeParams,optimalPath,
                                     filename=outfile,posteriorProbs=FALSE,subcloneProfiles=TRUE)

outparam <- paste("test_cluster0",numClusters,"_params.txt",sep="")
outputModelParameters(convergeParams,results,outparam)


norm <- convergeParams$n[length(convergeParams$n)]
ploidy <- convergeParams$phi[length(convergeParams$phi)]
#library(SNPchip)  ## use this library to plot chromosome idiogram (optional)
png(outplot,width=1200,height=1000,res=100)
par(mfrow=c(3,1))
plotCNlogRByChr(results, chr, ploidy=ploidy, geneAnnot=NULL, spacing=4,ylim=c(-4,6),cex=0.5,main="Chr 2")
plotAllelicRatio(results, chr, geneAnnot=NULL, spacing=4, ylim=c(0,1),cex=0.5,main="Chr 2")
plotClonalFrequency(results, chr, normal=tail(convergeParams$n,1), geneAnnot=NULL, spacing=4,ylim=c(0,1),cex=0.5,main="Chr 2")
if (as.numeric(numClusters) <= 2){
       ## NEW IN V1.2.0 ##
       ## users can choose to plot the subclone copy number profiles for <= 2 clusters
	plotSubcloneProfiles(results, chr, cex = 2, spacing=6, main="Chr 2")
}
#pI <- plotIdiogram(chr,build="hg19",unit="bp",label.y=-4.25,new=FALSE,ylim=c(-2,-1))
dev.off()







