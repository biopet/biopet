/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet

import nl.lumc.sasc.biopet.utils.{BiopetExecutable, MainCommand}

object BiopetToolsExecutable extends BiopetExecutable {

  def pipelines: List[MainCommand] = Nil

  def tools: List[MainCommand] =
    List(
      nl.lumc.sasc.biopet.tools.AnnotateVcfWithBed,
      nl.lumc.sasc.biopet.tools.bamstats.BamStats,
      nl.lumc.sasc.biopet.tools.BaseCounter,
      nl.lumc.sasc.biopet.tools.BastyGenerateFasta,
      nl.lumc.sasc.biopet.tools.BedtoolsCoverageToCounts,
      nl.lumc.sasc.biopet.tools.flagstat.BiopetFlagstat,
      nl.lumc.sasc.biopet.tools.CheckAllelesVcfInBam,
      nl.lumc.sasc.biopet.tools.ExtractAlignedFastq,
      nl.lumc.sasc.biopet.tools.FastqSplitter,
      nl.lumc.sasc.biopet.tools.FastqSync,
      nl.lumc.sasc.biopet.tools.FastqFilter,
      nl.lumc.sasc.biopet.tools.FindRepeatsPacBio,
      nl.lumc.sasc.biopet.tools.FindOverlapMatch,
      nl.lumc.sasc.biopet.tools.GvcfToBed,
      nl.lumc.sasc.biopet.tools.MergeAlleles,
      nl.lumc.sasc.biopet.tools.MergeTables,
      nl.lumc.sasc.biopet.tools.MpileupToVcf,
      nl.lumc.sasc.biopet.tools.PrefixFastq,
      nl.lumc.sasc.biopet.tools.refflatstats.RefflatStats,
      nl.lumc.sasc.biopet.tools.GtfToRefflat,
      nl.lumc.sasc.biopet.tools.SageCountFastq,
      nl.lumc.sasc.biopet.tools.SamplesTsvToConfig,
      nl.lumc.sasc.biopet.tools.SeqStat,
      nl.lumc.sasc.biopet.tools.SquishBed,
      nl.lumc.sasc.biopet.tools.SummaryToTsv,
      nl.lumc.sasc.biopet.tools.ValidateFastq,
      nl.lumc.sasc.biopet.tools.ValidateVcf,
      nl.lumc.sasc.biopet.tools.ValidateAnnotation,
      nl.lumc.sasc.biopet.tools.VcfFilter,
      nl.lumc.sasc.biopet.tools.vcfstats.VcfStats,
      nl.lumc.sasc.biopet.tools.VcfToTsv,
      nl.lumc.sasc.biopet.tools.VcfWithVcf,
      nl.lumc.sasc.biopet.tools.VepNormalizer,
      nl.lumc.sasc.biopet.tools.WipeReads,
      nl.lumc.sasc.biopet.tools.DownloadNcbiAssembly
    )

  def templates: List[MainCommand] = List()
}
