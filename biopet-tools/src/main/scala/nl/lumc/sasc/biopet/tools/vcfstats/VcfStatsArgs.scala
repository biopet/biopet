package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.File

/**
  * Commandline argument for vcfstats
  *
  * Created by pjvanthof on 18/07/2017.
  */
case class VcfStatsArgs(inputFile: File = null,
                        outputDir: File = null,
                        referenceFile: File = null,
                        intervals: Option[File] = None,
                        infoTags: List[String] = Nil,
                        genotypeTags: List[String] = Nil,
                        allInfoTags: Boolean = false,
                        allGenotypeTags: Boolean = false,
                        binSize: Int = 10000000,
                        writeBinStats: Boolean = false,
                        generalWiggle: List[String] = Nil,
                        genotypeWiggle: List[String] = Nil,
                        localThreads: Int = 1,
                        sparkMaster: Option[String] = None,
                        contigSampleOverlapPlots: Boolean = false)
