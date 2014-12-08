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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core

object BiopetExecutablePublic extends BiopetExecutable {
  def pipelines: List[MainCommand] = List(
    nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep,
    nl.lumc.sasc.biopet.pipelines.mapping.Mapping,
    //nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap,
    nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics,
    //nl.lumc.sasc.biopet.pipelines.yamsvp.Yamsvp,
    nl.lumc.sasc.biopet.pipelines.sage.Sage
  )

  def tools: List[MainCommand] = List(
    nl.lumc.sasc.biopet.tools.WipeReads,
    nl.lumc.sasc.biopet.tools.ExtractAlignedFastq,
    nl.lumc.sasc.biopet.tools.BiopetFlagstat,
    nl.lumc.sasc.biopet.tools.CheckAllelesVcfInBam,
    nl.lumc.sasc.biopet.tools.VcfToTsv,
    nl.lumc.sasc.biopet.tools.VcfFilter,
    nl.lumc.sasc.biopet.tools.FindRepeatsPacBio,
    nl.lumc.sasc.biopet.tools.BedToInterval,
    nl.lumc.sasc.biopet.tools.MpileupToVcf,
    nl.lumc.sasc.biopet.tools.FastqSplitter,
    nl.lumc.sasc.biopet.tools.BedtoolsCoverageToCounts,
    nl.lumc.sasc.biopet.tools.SageCountFastq,
    nl.lumc.sasc.biopet.tools.SageCreateLibrary,
    nl.lumc.sasc.biopet.tools.SageCreateTagCounts,
    nl.lumc.sasc.biopet.tools.BastyGenerateFasta,
    //nl.lumc.sasc.biopet.tools.MergeAlleles,
    nl.lumc.sasc.biopet.tools.SamplesTsvToJson)
}
