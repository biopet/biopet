package nl.lumc.sasc.biopet.core

object BiopetExecutableProtected extends BiopetExecutable {
  val pipelines: List[MainCommand] = List(
    nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep,
    nl.lumc.sasc.biopet.pipelines.mapping.Mapping,
    nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap,
    nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration,
    nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare,
    nl.lumc.sasc.biopet.pipelines.sage.Sage,
    nl.lumc.sasc.biopet.pipelines.basty.Basty,
    nl.lumc.sasc.biopet.pipelines.yamsvp.Yamsvp)

  val tools: List[MainCommand] = List(
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
    nl.lumc.sasc.biopet.tools.MergeAlleles,
    nl.lumc.sasc.biopet.tools.SamplesTsvToJson)
}
