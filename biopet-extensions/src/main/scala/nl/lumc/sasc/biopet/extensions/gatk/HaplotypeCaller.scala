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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Input, Output }

class HaplotypeCaller(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "HaplotypeCaller"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** What likelihood calculation engine to use to calculate the relative likelihood of reads vs haplotypes */
  @Argument(fullName = "likelihoodCalculationEngine", shortName = "likelihoodEngine", doc = "What likelihood calculation engine to use to calculate the relative likelihood of reads vs haplotypes", required = false, exclusiveOf = "", validation = "")
  var likelihoodCalculationEngine: String = _

  /** How to solve heterogeneous kmer situations using the fast method */
  @Argument(fullName = "heterogeneousKmerSizeResolution", shortName = "hksr", doc = "How to solve heterogeneous kmer situations using the fast method", required = false, exclusiveOf = "", validation = "")
  var heterogeneousKmerSizeResolution: String = _

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = config("dbsnp")

  /** If specified, we will not trim down the active region from the full region (active + extension) to just the active interval for genotyping */
  @Argument(fullName = "dontTrimActiveRegions", shortName = "dontTrimActiveRegions", doc = "If specified, we will not trim down the active region from the full region (active + extension) to just the active interval for genotyping", required = false, exclusiveOf = "", validation = "")
  var dontTrimActiveRegions: Boolean = config("dontTrimActiveRegions", default = false)

  /** the maximum extent into the full active region extension that we're willing to go in genotyping our events for discovery */
  @Argument(fullName = "maxDiscARExtension", shortName = "maxDiscARExtension", doc = "the maximum extent into the full active region extension that we're willing to go in genotyping our events for discovery", required = false, exclusiveOf = "", validation = "")
  var maxDiscARExtension: Option[Int] = config("maxDiscARExtension")

  /** the maximum extent into the full active region extension that we're willing to go in genotyping our events for GGA mode */
  @Argument(fullName = "maxGGAARExtension", shortName = "maxGGAARExtension", doc = "the maximum extent into the full active region extension that we're willing to go in genotyping our events for GGA mode", required = false, exclusiveOf = "", validation = "")
  var maxGGAARExtension: Option[Int] = config("maxGGAARExtension")

  /** Include at least this many bases around an event for calling indels */
  @Argument(fullName = "paddingAroundIndels", shortName = "paddingAroundIndels", doc = "Include at least this many bases around an event for calling indels", required = false, exclusiveOf = "", validation = "")
  var paddingAroundIndels: Option[Int] = config("paddingAroundIndels")

  /** Include at least this many bases around an event for calling snps */
  @Argument(fullName = "paddingAroundSNPs", shortName = "paddingAroundSNPs", doc = "Include at least this many bases around an event for calling snps", required = false, exclusiveOf = "", validation = "")
  var paddingAroundSNPs: Option[Int] = config("paddingAroundSNPs")

  /** Comparison VCF file */
  @Input(fullName = "comp", shortName = "comp", doc = "Comparison VCF file", required = false, exclusiveOf = "", validation = "")
  var comp: List[File] = config("comp", default = Nil)

  /** One or more specific annotations to apply to variant calls */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var annotation: List[String] = config("annotation", default = Nil, freeVar = false)

  /** One or more specific annotations to exclude */
  @Argument(fullName = "excludeAnnotation", shortName = "XA", doc = "One or more specific annotations to exclude", required = false, exclusiveOf = "", validation = "")
  var excludeAnnotation: List[String] = config("excludeAnnotation", default = Nil, freeVar = false)

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var group: List[String] = config("group", default = Nil, freeVar = false)

  /** Print out very verbose debug information about each triggering active region */
  @Argument(fullName = "debug", shortName = "debug", doc = "Print out very verbose debug information about each triggering active region", required = false, exclusiveOf = "", validation = "")
  var debug: Boolean = config("debug", default = false, freeVar = false)

  /** Use the contamination-filtered read maps for the purposes of annotating variants */
  @Argument(fullName = "useFilteredReadsForAnnotations", shortName = "useFilteredReadsForAnnotations", doc = "Use the contamination-filtered read maps for the purposes of annotating variants", required = false, exclusiveOf = "", validation = "")
  var useFilteredReadsForAnnotations: Boolean = config("useFilteredReadsForAnnotations", default = false)

  /** Mode for emitting reference confidence scores */
  @Argument(fullName = "emitRefConfidence", shortName = "ERC", doc = "Mode for emitting reference confidence scores", required = false, exclusiveOf = "", validation = "")
  var emitRefConfidence: Option[String] = config("emitRefConfidence")

  /** File to which assembled haplotypes should be written */
  @Output(fullName = "bamOutput", shortName = "bamout", doc = "File to which assembled haplotypes should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[BamGatherFunction])
  var bamOutput: File = _

  /** Automatically generated md5 for bamOutput */
  @Output(fullName = "bamOutputMD5", shortName = "", doc = "Automatically generated md5 for bamOutput", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var bamOutputMD5: File = _

  /** Which haplotypes should be written to the BAM */
  @Argument(fullName = "bamWriterType", shortName = "bamWriterType", doc = "Which haplotypes should be written to the BAM", required = false, exclusiveOf = "", validation = "")
  var bamWriterType: String = _

  /** Don't skip calculations in ActiveRegions with no variants */
  @Argument(fullName = "disableOptimizations", shortName = "disableOptimizations", doc = "Don't skip calculations in ActiveRegions with no variants", required = false, exclusiveOf = "", validation = "")
  var disableOptimizations: Boolean = config("disableOptimizations", default = false)

  /** If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site */
  @Argument(fullName = "annotateNDA", shortName = "nda", doc = "If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site", required = false, exclusiveOf = "", validation = "")
  var annotateNDA: Boolean = config("annotateNDA", default = false)

  /** Heterozygosity value used to compute prior likelihoods for any locus */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false, exclusiveOf = "", validation = "")
  var heterozygosity: Option[Double] = config("heterozygosity")

  /** Format string for heterozygosity */
  @Argument(fullName = "heterozygosityFormat", shortName = "", doc = "Format string for heterozygosity", required = false, exclusiveOf = "", validation = "")
  var heterozygosityFormat: String = "%s"

  /** Heterozygosity for indel calling */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosity: Option[Double] = config("indel_heterozygosity")

  /** Format string for indel_heterozygosity */
  @Argument(fullName = "indel_heterozygosityFormat", shortName = "", doc = "Format string for indel_heterozygosity", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosityFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be called */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be called", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_calling: Option[Double] = config("stand_call_conf")

  /** Format string for standard_min_confidence_threshold_for_calling */
  @Argument(fullName = "standard_min_confidence_threshold_for_callingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_callingFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold) */
  @Argument(fullName = "standard_min_confidence_threshold_for_emitting", shortName = "stand_emit_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold)", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emitting: Option[Double] = config("stand_emit_conf")

  /** Format string for standard_min_confidence_threshold_for_emitting */
  @Argument(fullName = "standard_min_confidence_threshold_for_emittingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_emitting", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emittingFormat: String = "%s"

  /** Maximum number of alternate alleles to genotype */
  @Argument(fullName = "max_alternate_alleles", shortName = "maxAltAlleles", doc = "Maximum number of alternate alleles to genotype", required = false, exclusiveOf = "", validation = "")
  var max_alternate_alleles: Option[Int] = config("max_alternate_alleles")

  /** Input prior for calls */
  @Argument(fullName = "input_prior", shortName = "inputPrior", doc = "Input prior for calls", required = false, exclusiveOf = "", validation = "")
  var input_prior: List[Double] = config("input_prior", default = Nil)

  /** Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy). */
  @Argument(fullName = "sample_ploidy", shortName = "ploidy", doc = "Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy).", required = false, exclusiveOf = "", validation = "")
  var sample_ploidy: Option[Int] = config("sample_ploidy")

  /** Specifies how to determine the alternate alleles to use for genotyping */
  @Argument(fullName = "genotyping_mode", shortName = "gt_mode", doc = "Specifies how to determine the alternate alleles to use for genotyping", required = false, exclusiveOf = "", validation = "")
  var genotyping_mode: Option[String] = config("genotyping_mode")

  /** The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES */
  @Input(fullName = "alleles", shortName = "alleles", doc = "The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES", required = false, exclusiveOf = "", validation = "")
  var alleles: Option[File] = None

  /** Fraction of contamination in sequencing data (for all samples) to aggressively remove */
  @Argument(fullName = "contamination_fraction_to_filter", shortName = "contamination", doc = "Fraction of contamination in sequencing data (for all samples) to aggressively remove", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filter: Option[Double] = config("contamination_fraction_to_filter")

  /** Format string for contamination_fraction_to_filter */
  @Argument(fullName = "contamination_fraction_to_filterFormat", shortName = "", doc = "Format string for contamination_fraction_to_filter", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filterFormat: String = "%s"

  /** Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header. */
  @Input(fullName = "contamination_fraction_per_sample_file", shortName = "contaminationFile", doc = "Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header.", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_per_sample_file: Option[File] = config("contamination_fraction_per_sample_file")

  /** Non-reference probability calculation model to employ */
  @Argument(fullName = "p_nonref_model", shortName = "pnrm", doc = "Non-reference probability calculation model to employ", required = false, exclusiveOf = "", validation = "")
  var p_nonref_model: Option[String] = config("p_nonref_model")

  /** x */
  @Argument(fullName = "exactcallslog", shortName = "logExactCalls", doc = "x", required = false, exclusiveOf = "", validation = "")
  var exactcallslog: Option[File] = config("exactcallslog")

  /** Specifies which type of calls we should output */
  @Argument(fullName = "output_mode", shortName = "out_mode", doc = "Specifies which type of calls we should output", required = false, exclusiveOf = "", validation = "")
  var output_mode: Option[String] = config("output_mode")

  /** Annotate all sites with PLs */
  @Argument(fullName = "allSitePLs", shortName = "allSitePLs", doc = "Annotate all sites with PLs", required = false, exclusiveOf = "", validation = "")
  var allSitePLs: Boolean = config("allSitePLs", default = false)

  /** Flat gap continuation penalty for use in the Pair HMM */
  @Argument(fullName = "gcpHMM", shortName = "gcpHMM", doc = "Flat gap continuation penalty for use in the Pair HMM", required = false, exclusiveOf = "", validation = "")
  var gcpHMM: Option[Int] = config("gcpHMM")

  /** The PairHMM implementation to use for genotype likelihood calculations */
  @Argument(fullName = "pair_hmm_implementation", shortName = "pairHMM", doc = "The PairHMM implementation to use for genotype likelihood calculations", required = false, exclusiveOf = "", validation = "")
  var pair_hmm_implementation: org.broadinstitute.gatk.utils.pairhmm.PairHMM.HMM_IMPLEMENTATION = _

  /** The PairHMM machine-dependent sub-implementation to use for genotype likelihood calculations */
  @Argument(fullName = "pair_hmm_sub_implementation", shortName = "pairHMMSub", doc = "The PairHMM machine-dependent sub-implementation to use for genotype likelihood calculations", required = false, exclusiveOf = "", validation = "")
  var pair_hmm_sub_implementation: org.broadinstitute.gatk.utils.pairhmm.PairHMM.HMM_SUB_IMPLEMENTATION = _

  /** Load the vector logless PairHMM library each time a GATK run is initiated in the test suite */
  @Argument(fullName = "always_load_vector_logless_PairHMM_lib", shortName = "alwaysloadVectorHMM", doc = "Load the vector logless PairHMM library each time a GATK run is initiated in the test suite", required = false, exclusiveOf = "", validation = "")
  var always_load_vector_logless_PairHMM_lib: Boolean = config("always_load_vector_logless_PairHMM_lib", default = false)

  /** The global assumed mismapping rate for reads */
  @Argument(fullName = "phredScaledGlobalReadMismappingRate", shortName = "globalMAPQ", doc = "The global assumed mismapping rate for reads", required = false, exclusiveOf = "", validation = "")
  var phredScaledGlobalReadMismappingRate: Option[Int] = config("phredScaledGlobalReadMismappingRate")

  /** Disable the use of the FPGA HMM implementation */
  @Argument(fullName = "noFpga", shortName = "noFpga", doc = "Disable the use of the FPGA HMM implementation", required = false, exclusiveOf = "", validation = "")
  var noFpga: Boolean = config("noFpga", default = false)

  /** Name of single sample to use from a multi-sample bam */
  @Argument(fullName = "sample_name", shortName = "sn", doc = "Name of single sample to use from a multi-sample bam", required = false, exclusiveOf = "", validation = "")
  var sample_name: Option[String] = config("sample_name")

  /** Kmer size to use in the read threading assembler */
  @Argument(fullName = "kmerSize", shortName = "kmerSize", doc = "Kmer size to use in the read threading assembler", required = false, exclusiveOf = "", validation = "")
  var kmerSize: List[Int] = config("kmerSize", default = Nil)

  /** Disable iterating over kmer sizes when graph cycles are detected */
  @Argument(fullName = "dontIncreaseKmerSizesForCycles", shortName = "dontIncreaseKmerSizesForCycles", doc = "Disable iterating over kmer sizes when graph cycles are detected", required = false, exclusiveOf = "", validation = "")
  var dontIncreaseKmerSizesForCycles: Boolean = config("dontIncreaseKmerSizesForCycles", default = false)

  /** Allow graphs that have non-unique kmers in the reference */
  @Argument(fullName = "allowNonUniqueKmersInRef", shortName = "allowNonUniqueKmersInRef", doc = "Allow graphs that have non-unique kmers in the reference", required = false, exclusiveOf = "", validation = "")
  var allowNonUniqueKmersInRef: Boolean = config("allowNonUniqueKmersInRef", default = false)

  /** Number of samples that must pass the minPruning threshold */
  @Argument(fullName = "numPruningSamples", shortName = "numPruningSamples", doc = "Number of samples that must pass the minPruning threshold", required = false, exclusiveOf = "", validation = "")
  var numPruningSamples: Option[Int] = config("numPruningSamples")

  /** Disable dangling head and tail recovery */
  @Argument(fullName = "doNotRecoverDanglingBranches", shortName = "doNotRecoverDanglingBranches", doc = "Disable dangling head and tail recovery", required = false, exclusiveOf = "", validation = "")
  var doNotRecoverDanglingBranches: Boolean = config("doNotRecoverDanglingBranches", default = false)

  /** Minimum length of a dangling branch to attempt recovery */
  @Argument(fullName = "minDanglingBranchLength", shortName = "minDanglingBranchLength", doc = "Minimum length of a dangling branch to attempt recovery", required = false, exclusiveOf = "", validation = "")
  var minDanglingBranchLength: Option[Int] = config("minDanglingBranchLength")

  /** 1000G consensus mode */
  @Argument(fullName = "consensus", shortName = "consensus", doc = "1000G consensus mode", required = false, exclusiveOf = "", validation = "")
  var consensus: Boolean = config("consensus", default = false)

  /** Maximum number of haplotypes to consider for your population */
  @Argument(fullName = "maxNumHaplotypesInPopulation", shortName = "maxNumHaplotypesInPopulation", doc = "Maximum number of haplotypes to consider for your population", required = false, exclusiveOf = "", validation = "")
  var maxNumHaplotypesInPopulation: Option[Int] = config("maxNumHaplotypesInPopulation")

  /** Use an exploratory algorithm to error correct the kmers used during assembly */
  @Argument(fullName = "errorCorrectKmers", shortName = "errorCorrectKmers", doc = "Use an exploratory algorithm to error correct the kmers used during assembly", required = false, exclusiveOf = "", validation = "")
  var errorCorrectKmers: Boolean = _

  /** Minimum support to not prune paths in the graph */
  @Argument(fullName = "minPruning", shortName = "minPruning", doc = "Minimum support to not prune paths in the graph", required = false, exclusiveOf = "", validation = "")
  var minPruning: Option[Int] = config("minPruning")

  /** Write DOT formatted graph files out of the assembler for only this graph size */
  @Argument(fullName = "debugGraphTransformations", shortName = "debugGraphTransformations", doc = "Write DOT formatted graph files out of the assembler for only this graph size", required = false, exclusiveOf = "", validation = "")
  var debugGraphTransformations: Boolean = config("debugGraphTransformations", default = false)

  /** Allow cycles in the kmer graphs to generate paths with multiple copies of the path sequenece rather than just the shortest paths */
  @Argument(fullName = "allowCyclesInKmerGraphToGeneratePaths", shortName = "allowCyclesInKmerGraphToGeneratePaths", doc = "Allow cycles in the kmer graphs to generate paths with multiple copies of the path sequenece rather than just the shortest paths", required = false, exclusiveOf = "", validation = "")
  var allowCyclesInKmerGraphToGeneratePaths: Boolean = config("allowCyclesInKmerGraphToGeneratePaths", default = false)

  /** Write debug assembly graph information to this file */
  @Output(fullName = "graphOutput", shortName = "graph", doc = "Write debug assembly graph information to this file", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var graphOutput: File = _

  /** Use an exploratory algorithm to error correct the kmers used during assembly */
  @Argument(fullName = "kmerLengthForReadErrorCorrection", shortName = "kmerLengthForReadErrorCorrection", doc = "Use an exploratory algorithm to error correct the kmers used during assembly", required = false, exclusiveOf = "", validation = "")
  var kmerLengthForReadErrorCorrection: Option[Int] = config("kmerLengthForReadErrorCorrection")

  /** A k-mer must be seen at least these times for it considered to be solid */
  @Argument(fullName = "minObservationsForKmerToBeSolid", shortName = "minObservationsForKmerToBeSolid", doc = "A k-mer must be seen at least these times for it considered to be solid", required = false, exclusiveOf = "", validation = "")
  var minObservationsForKmerToBeSolid: Option[Int] = config("minObservationsForKmerToBeSolid")

  /** GQ thresholds for reference confidence bands */
  @Argument(fullName = "GVCFGQBands", shortName = "GQB", doc = "GQ thresholds for reference confidence bands", required = false, exclusiveOf = "", validation = "")
  var GVCFGQBands: List[Int] = config("GVCFGQBands", default = Nil)

  /** The size of an indel to check for in the reference model */
  @Argument(fullName = "indelSizeToEliminateInRefModel", shortName = "ERCIS", doc = "The size of an indel to check for in the reference model", required = false, exclusiveOf = "", validation = "")
  var indelSizeToEliminateInRefModel: Option[Int] = config("indelSizeToEliminateInRefModel")

  /** Minimum base quality required to consider a base for calling */
  @Argument(fullName = "min_base_quality_score", shortName = "mbq", doc = "Minimum base quality required to consider a base for calling", required = false, exclusiveOf = "", validation = "")
  var min_base_quality_score: Option[Int] = config("min_base_quality_score")

  /** Include unmapped reads with chromosomal coordinates */
  @Argument(fullName = "includeUmappedReads", shortName = "unmapped", doc = "Include unmapped reads with chromosomal coordinates", required = false, exclusiveOf = "", validation = "")
  var includeUmappedReads: Boolean = config("includeUmappedReads", default = false)

  /** Use additional trigger on variants found in an external alleles file */
  @Argument(fullName = "useAllelesTrigger", shortName = "allelesTrigger", doc = "Use additional trigger on variants found in an external alleles file", required = false, exclusiveOf = "", validation = "")
  var useAllelesTrigger: Boolean = config("useAllelesTrigger", default = false)

  /** Disable physical phasing */
  @Argument(fullName = "doNotRunPhysicalPhasing", shortName = "doNotRunPhysicalPhasing", doc = "Disable physical phasing", required = false, exclusiveOf = "", validation = "")
  var doNotRunPhysicalPhasing: Boolean = config("doNotRunPhysicalPhasing", default = false)

  /** Only use reads from this read group when making calls (but use all reads to build the assembly) */
  @Argument(fullName = "keepRG", shortName = "keepRG", doc = "Only use reads from this read group when making calls (but use all reads to build the assembly)", required = false, exclusiveOf = "", validation = "")
  var keepRG: Option[String] = config("keepRG")

  /** Just determine ActiveRegions, don't perform assembly or calling */
  @Argument(fullName = "justDetermineActiveRegions", shortName = "justDetermineActiveRegions", doc = "Just determine ActiveRegions, don't perform assembly or calling", required = false, exclusiveOf = "", validation = "")
  var justDetermineActiveRegions: Boolean = config("justDetermineActiveRegions", default = false)

  /** Perform assembly but do not genotype variants */
  @Argument(fullName = "dontGenotype", shortName = "dontGenotype", doc = "Perform assembly but do not genotype variants", required = false, exclusiveOf = "", validation = "")
  var dontGenotype: Boolean = config("dontGenotype", default = false)

  /** Do not analyze soft clipped bases in the reads */
  @Argument(fullName = "dontUseSoftClippedBases", shortName = "dontUseSoftClippedBases", doc = "Do not analyze soft clipped bases in the reads", required = false, exclusiveOf = "", validation = "")
  var dontUseSoftClippedBases: Boolean = config("dontUseSoftClippedBases", default = false)

  /** Write a BAM called assemblyFailure.bam capturing all of the reads that were in the active region when the assembler failed for any reason */
  @Argument(fullName = "captureAssemblyFailureBAM", shortName = "captureAssemblyFailureBAM", doc = "Write a BAM called assemblyFailure.bam capturing all of the reads that were in the active region when the assembler failed for any reason", required = false, exclusiveOf = "", validation = "")
  var captureAssemblyFailureBAM: Boolean = config("captureAssemblyFailureBAM", default = false)

  /** Use an exploratory algorithm to error correct the kmers used during assembly */
  @Argument(fullName = "errorCorrectReads", shortName = "errorCorrectReads", doc = "Use an exploratory algorithm to error correct the kmers used during assembly", required = false, exclusiveOf = "", validation = "")
  var errorCorrectReads: Boolean = config("errorCorrectReads", default = false)

  /** The PCR indel model to use */
  @Argument(fullName = "pcr_indel_model", shortName = "pcrModel", doc = "The PCR indel model to use", required = false, exclusiveOf = "", validation = "")
  var pcr_indel_model: Option[String] = config("pcr_indel_model")

  /** Maximum reads in an active region */
  @Argument(fullName = "maxReadsInRegionPerSample", shortName = "maxReadsInRegionPerSample", doc = "Maximum reads in an active region", required = false, exclusiveOf = "", validation = "")
  var maxReadsInRegionPerSample: Option[Int] = config("maxReadsInRegionPerSample")

  /** Minimum number of reads sharing the same alignment start for each genomic location in an active region */
  @Argument(fullName = "minReadsPerAlignmentStart", shortName = "minReadsPerAlignStart", doc = "Minimum number of reads sharing the same alignment start for each genomic location in an active region", required = false, exclusiveOf = "", validation = "")
  var minReadsPerAlignmentStart: Option[Int] = config("minReadsPerAlignmentStart")

  /** Output the raw activity profile results in IGV format */
  @Output(fullName = "activityProfileOut", shortName = "APO", doc = "Output the raw activity profile results in IGV format", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var activityProfileOut: File = _

  /** Output the active region to this IGV formatted file */
  @Output(fullName = "activeRegionOut", shortName = "ARO", doc = "Output the active region to this IGV formatted file", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var activeRegionOut: File = _

  /** Use this interval list file as the active regions to process */
  @Input(fullName = "activeRegionIn", shortName = "AR", doc = "Use this interval list file as the active regions to process", required = false, exclusiveOf = "", validation = "")
  var activeRegionIn: Seq[File] = Nil

  /** The active region extension; if not provided defaults to Walker annotated default */
  @Argument(fullName = "activeRegionExtension", shortName = "activeRegionExtension", doc = "The active region extension; if not provided defaults to Walker annotated default", required = false, exclusiveOf = "", validation = "")
  var activeRegionExtension: Option[Int] = config("activeRegionExtension")

  /** If provided, all bases will be tagged as active */
  @Argument(fullName = "forceActive", shortName = "forceActive", doc = "If provided, all bases will be tagged as active", required = false, exclusiveOf = "", validation = "")
  var forceActive: Boolean = config("forceActive", default = false)

  /** The active region maximum size; if not provided defaults to Walker annotated default */
  @Argument(fullName = "activeRegionMaxSize", shortName = "activeRegionMaxSize", doc = "The active region maximum size; if not provided defaults to Walker annotated default", required = false, exclusiveOf = "", validation = "")
  var activeRegionMaxSize: Option[Int] = config("activeRegionMaxSize")

  /** The sigma of the band pass filter Gaussian kernel; if not provided defaults to Walker annotated default */
  @Argument(fullName = "bandPassSigma", shortName = "bandPassSigma", doc = "The sigma of the band pass filter Gaussian kernel; if not provided defaults to Walker annotated default", required = false, exclusiveOf = "", validation = "")
  var bandPassSigma: Option[Double] = config("bandPassSigma")

  /** Format string for bandPassSigma */
  @Argument(fullName = "bandPassSigmaFormat", shortName = "", doc = "Format string for bandPassSigma", required = false, exclusiveOf = "", validation = "")
  var bandPassSigmaFormat: String = "%s"

  /** Region probability propagation distance beyond it's maximum size. */
  @Argument(fullName = "maxProbPropagationDistance", shortName = "maxProbPropDist", doc = "Region probability propagation distance beyond it's maximum size.", required = false, exclusiveOf = "", validation = "")
  var maxProbPropagationDistance: Option[Int] = config("maxProbPropagationDistance")

  /** Threshold for the probability of a profile state being active. */
  @Argument(fullName = "activeProbabilityThreshold", shortName = "ActProbThresh", doc = "Threshold for the probability of a profile state being active.", required = false, exclusiveOf = "", validation = "")
  var activeProbabilityThreshold: Option[Double] = config("activeProbabilityThreshold")

  /** Format string for activeProbabilityThreshold */
  @Argument(fullName = "activeProbabilityThresholdFormat", shortName = "", doc = "Format string for activeProbabilityThreshold", required = false, exclusiveOf = "", validation = "")
  var activeProbabilityThresholdFormat: String = "%s"

  /** Minimum read mapping quality required to consider a read for analysis with the HaplotypeCaller */
  @Argument(fullName = "min_mapping_quality_score", shortName = "mmq", doc = "Minimum read mapping quality required to consider a read for analysis with the HaplotypeCaller", required = false, exclusiveOf = "", validation = "")
  var min_mapping_quality_score: Option[Int] = config("min_mapping_quality_score")

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  @Output
  @Gather(enabled = false)
  private var outputIndex: File = _

  @Output
  @Gather(enabled = false)
  private var outputBamIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
    dbsnp.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    deps ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig + ".idx"))
    if (bamOutput != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(bamOutput))
      if (!disable_bam_indexing)
        outputBamIndex = new File(bamOutput.getPath.stripSuffix(".bam") + ".bai")
    if (bamOutput != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(bamOutput))
      if (generate_md5)
        bamOutputMD5 = new File(bamOutput.getPath + ".md5")
    alleles.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    num_cpu_threads_per_data_thread = Some(getThreads)
  }

  override def cmdLine = super.cmdLine +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    optional("-likelihoodEngine", likelihoodCalculationEngine, spaceSeparated = true, escape = true, format = "%s") +
    optional("-hksr", heterogeneousKmerSizeResolution, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp.getOrElse(null)), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    conditional(dontTrimActiveRegions, "-dontTrimActiveRegions", escape = true, format = "%s") +
    optional("-maxDiscARExtension", maxDiscARExtension, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxGGAARExtension", maxGGAARExtension, spaceSeparated = true, escape = true, format = "%s") +
    optional("-paddingAroundIndels", paddingAroundIndels, spaceSeparated = true, escape = true, format = "%s") +
    optional("-paddingAroundSNPs", paddingAroundSNPs, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XA", excludeAnnotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    conditional(debug, "-debug", escape = true, format = "%s") +
    conditional(useFilteredReadsForAnnotations, "-useFilteredReadsForAnnotations", escape = true, format = "%s") +
    optional("-ERC", emitRefConfidence, spaceSeparated = true, escape = true, format = "%s") +
    optional("-bamout", bamOutput, spaceSeparated = true, escape = true, format = "%s") +
    optional("-bamWriterType", bamWriterType, spaceSeparated = true, escape = true, format = "%s") +
    conditional(disableOptimizations, "-disableOptimizations", escape = true, format = "%s") +
    conditional(annotateNDA, "-nda", escape = true, format = "%s") +
    optional("-hets", heterozygosity, spaceSeparated = true, escape = true, format = heterozygosityFormat) +
    optional("-indelHeterozygosity", indel_heterozygosity, spaceSeparated = true, escape = true, format = indel_heterozygosityFormat) +
    optional("-stand_call_conf", standard_min_confidence_threshold_for_calling, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_callingFormat) +
    optional("-stand_emit_conf", standard_min_confidence_threshold_for_emitting, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_emittingFormat) +
    optional("-maxAltAlleles", max_alternate_alleles, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-inputPrior", input_prior, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ploidy", sample_ploidy, spaceSeparated = true, escape = true, format = "%s") +
    optional("-gt_mode", genotyping_mode, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-alleles", alleles.getOrElse(null)), alleles, spaceSeparated = true, escape = true, format = "%s") +
    optional("-contamination", contamination_fraction_to_filter, spaceSeparated = true, escape = true, format = contamination_fraction_to_filterFormat) +
    optional("-contaminationFile", contamination_fraction_per_sample_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pnrm", p_nonref_model, spaceSeparated = true, escape = true, format = "%s") +
    optional("-logExactCalls", exactcallslog, spaceSeparated = true, escape = true, format = "%s") +
    optional("-out_mode", output_mode, spaceSeparated = true, escape = true, format = "%s") + conditional(allSitePLs, "-allSitePLs", escape = true, format = "%s") +
    optional("-gcpHMM", gcpHMM, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pairHMM", pair_hmm_implementation, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pairHMMSub", pair_hmm_sub_implementation, spaceSeparated = true, escape = true, format = "%s") +
    conditional(always_load_vector_logless_PairHMM_lib, "-alwaysloadVectorHMM", escape = true, format = "%s") +
    optional("-globalMAPQ", phredScaledGlobalReadMismappingRate, spaceSeparated = true, escape = true, format = "%s") +
    conditional(noFpga, "-noFpga", escape = true, format = "%s") + optional("-sn", sample_name, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-kmerSize", kmerSize, spaceSeparated = true, escape = true, format = "%s") +
    conditional(dontIncreaseKmerSizesForCycles, "-dontIncreaseKmerSizesForCycles", escape = true, format = "%s") +
    conditional(allowNonUniqueKmersInRef, "-allowNonUniqueKmersInRef", escape = true, format = "%s") +
    optional("-numPruningSamples", numPruningSamples, spaceSeparated = true, escape = true, format = "%s") +
    conditional(doNotRecoverDanglingBranches, "-doNotRecoverDanglingBranches", escape = true, format = "%s") +
    optional("-minDanglingBranchLength", minDanglingBranchLength, spaceSeparated = true, escape = true, format = "%s") +
    conditional(consensus, "-consensus", escape = true, format = "%s") +
    optional("-maxNumHaplotypesInPopulation", maxNumHaplotypesInPopulation, spaceSeparated = true, escape = true, format = "%s") +
    conditional(errorCorrectKmers, "-errorCorrectKmers", escape = true, format = "%s") +
    optional("-minPruning", minPruning, spaceSeparated = true, escape = true, format = "%s") +
    conditional(debugGraphTransformations, "-debugGraphTransformations", escape = true, format = "%s") +
    conditional(allowCyclesInKmerGraphToGeneratePaths, "-allowCyclesInKmerGraphToGeneratePaths", escape = true, format = "%s") +
    optional("-graph", graphOutput, spaceSeparated = true, escape = true, format = "%s") +
    optional("-kmerLengthForReadErrorCorrection", kmerLengthForReadErrorCorrection, spaceSeparated = true, escape = true, format = "%s") +
    optional("-minObservationsForKmerToBeSolid", minObservationsForKmerToBeSolid, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-GQB", GVCFGQBands, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ERCIS", indelSizeToEliminateInRefModel, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mbq", min_base_quality_score, spaceSeparated = true, escape = true, format = "%s") +
    conditional(includeUmappedReads, "-unmapped", escape = true, format = "%s") +
    conditional(useAllelesTrigger, "-allelesTrigger", escape = true, format = "%s") +
    conditional(doNotRunPhysicalPhasing, "-doNotRunPhysicalPhasing", escape = true, format = "%s") +
    optional("-keepRG", keepRG, spaceSeparated = true, escape = true, format = "%s") +
    conditional(justDetermineActiveRegions, "-justDetermineActiveRegions", escape = true, format = "%s") +
    conditional(dontGenotype, "-dontGenotype", escape = true, format = "%s") +
    conditional(dontUseSoftClippedBases, "-dontUseSoftClippedBases", escape = true, format = "%s") +
    conditional(captureAssemblyFailureBAM, "-captureAssemblyFailureBAM", escape = true, format = "%s") +
    conditional(errorCorrectReads, "-errorCorrectReads", escape = true, format = "%s") +
    optional("-pcrModel", pcr_indel_model, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxReadsInRegionPerSample", maxReadsInRegionPerSample, spaceSeparated = true, escape = true, format = "%s") +
    optional("-minReadsPerAlignStart", minReadsPerAlignmentStart, spaceSeparated = true, escape = true, format = "%s") +
    optional("-APO", activityProfileOut, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ARO", activeRegionOut, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-AR", activeRegionIn, spaceSeparated = true, escape = true, format = "%s") +
    optional("-activeRegionExtension", activeRegionExtension, spaceSeparated = true, escape = true, format = "%s") +
    conditional(forceActive, "-forceActive", escape = true, format = "%s") +
    optional("-activeRegionMaxSize", activeRegionMaxSize, spaceSeparated = true, escape = true, format = "%s") +
    optional("-bandPassSigma", bandPassSigma, spaceSeparated = true, escape = true, format = bandPassSigmaFormat) +
    optional("-maxProbPropDist", maxProbPropagationDistance, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ActProbThresh", activeProbabilityThreshold, spaceSeparated = true, escape = true, format = activeProbabilityThresholdFormat) +
    optional("-mmq", min_mapping_quality_score, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object HaplotypeCaller {
  def apply(root: Configurable, inputFiles: List[File], outputFile: File): HaplotypeCaller = {
    val hc = new HaplotypeCaller(root)
    hc.input_file = inputFiles
    hc.out = outputFile
    hc
  }
}
