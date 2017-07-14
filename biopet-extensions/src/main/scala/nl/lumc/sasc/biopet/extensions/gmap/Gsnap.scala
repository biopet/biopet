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
package nl.lumc.sasc.biopet.extensions.gmap

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

import scala.util.matching.Regex

/**
  * Wrapper for the gsnap command line tool
  * Written based on gsnap version 2014-05-15
  */
class Gsnap(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  /** default executable */
  executable = config("exe", default = "gsnap", freeVar = false)

  /** default threads */
  override def defaultThreads = 8

  /** default vmem for cluster jobs */
  override def defaultCoreMemory = 10.0

  /** input file */
  @Input(doc = "Input FASTQ file(s)", required = true) //var input: List[File] = _
  var input: List[File] = List.empty[File]

  /** output file */
  @Output(doc = "Output alignment file", required = true)
  var output: File = _

  /** genome directory */
  @Argument(doc = "Directory of genome database")
  var dir: Option[File] = config("dir")

  /** genome database */
  @Argument(doc = "Genome database name", required = true)
  var db: String = config("db")

  /** whether to use a suffix array, which will give increased speed */
  var useSarray: Option[Int] = config("use_sarray")

  /** kmer size to use in genome database (allowed values: 16 or less) */
  var kmer: Option[Int] = config("kmer")

  /** sampling to use in genome database */
  var sampling: Option[Int] = config("sampling")

  /** process only the i-th out of every n sequences */
  var part: Option[String] = config("part")

  /** size of input buffer (program reads this many sequences at a time)*/
  var inputBufferSize: Option[Int] = config("input_buffer_size")

  /** amount of barcode to remove from start of read */
  var barcodeLength: Option[Int] = config("barcode_length")

  /** orientation of paired-end reads */
  var orientation: Option[String] = config("orientation")

  /** starting position of identifier in fastq header, space-delimited (>= 1) */
  var fastqIdStart: Option[Int] = config("fastq_id_start")

  /** ending position of identifier in fastq header, space-delimited (>= 1) */
  var fastqIdEnd: Option[Int] = config("fastq_id_end")

  /** when multiple fastq files are provided on the command line, gsnap assumes */
  var forceSingleEnd: Boolean = config("force_single_end", default = false)

  /** skips reads marked by the illumina chastity program.  expecting a string */
  var filterChastity: Option[String] = config("filter_chastity")

  /** allows accession names of reads to mismatch in paired-end files */
  var allowPeNameMismatch: Boolean = config("allow_pe_name_mismatch", default = false)

  /** uncompress gzipped input files */
  var gunzip: Boolean = config("gunzip", default = false)

  /** uncompress bzip2-compressed input files */
  var bunzip2: Boolean = config("bunzip2", default = false)

  /** batch mode (default = 2) */
  var batch: Option[Int] = config("batch")

  /** whether to expand the genomic offsets index */
  var expandOffsets: Option[Int] = config("expand_offsets")

  /** maximum number of mismatches allowed (if not specified, then */
  var maxMismatches: Option[Float] = config("max_mismatches")

  /** whether to count unknown (n) characters in the query as a mismatch */
  var queryUnkMismatch: Option[Int] = config("query_unk_mismatch")

  /** whether to count unknown (n) characters in the genome as a mismatch */
  var genomeUnkMismatch: Option[Int] = config("genome_unk_mismatch")

  /** maximum number of alignments to find (default 1000) */
  var maxsearch: Option[Int] = config("maxsearch")

  /** threshold for computing a terminal alignment (from one end of the */
  var terminalThreshold: Option[Int] = config("terminal_threshold")

  /** threshold alignment length in bp for a terminal alignment result to be printed (in bp) */
  var terminalOutputMinlength: Option[Int] = config("terminal_output_minlength")

  /** penalty for an indel (default 2) */
  var indelPenalty: Option[Int] = config("indel_penalty")

  /** minimum length at end required for indel alignments (default 4) */
  var indelEndlength: Option[Int] = config("indel_endlength")

  /** maximum number of middle insertions allowed (default 9) */
  var maxMiddleInsertions: Option[Int] = config("max_middle_insertions")

  /** maximum number of middle deletions allowed (default 30) */
  var maxMiddleDeletions: Option[Int] = config("max_middle_deletions")

  /** maximum number of end insertions allowed (default 3) */
  var maxEndInsertions: Option[Int] = config("max_end_insertions")

  /** maximum number of end deletions allowed (default 6) */
  var maxEndDeletions: Option[Int] = config("max_end_deletions")

  /** report suboptimal hits beyond best hit (default 0) */
  var suboptimalLevels: Option[Int] = config("suboptimal_levels")

  /** method for removing adapters from reads.  currently allowed values: off, paired */
  var adapterStrip: Option[String] = config("adapter_strip")

  /** score to use for mismatches when trimming at ends (default is -3; */
  var trimMismatchScore: Option[Int] = config("trim_mismatch_score")

  /** score to use for indels when trimming at ends (default is -4; */
  var trimIndelScore: Option[Int] = config("trim_indel_score")

  /** directory for snps index files (created using snpindex) (default is */
  var snpsdir: Option[String] = config("snpsdir")

  /** use database containing known snps (in <string>.iit, built */
  var useSnps: Option[String] = config("use_snps")

  /** directory for methylcytosine index files (created using cmetindex) */
  var cmetdir: Option[String] = config("cmetdir")

  /** directory for a-to-i rna editing index files (created using atoiindex) */
  var atoidir: Option[String] = config("atoidir")

  /** alignment mode: standard (default), cmet-stranded, cmet-nonstranded, */
  var mode: Option[String] = config("mode")

  /** directory for tally iit file to resolve concordant multiple results (default is */
  var tallydir: Option[String] = config("tallydir")

  /** use this tally iit file to resolve concordant multiple results */
  var useTally: Option[String] = config("use_tally")

  /** directory for runlength iit file to resolve concordant multiple results (default is */
  var runLengthDir: Option[String] = config("runlengthdir")

  /** use this runlength iit file to resolve concordant multiple results */
  var useRunlength: Option[String] = config("use_runlength")

  /** cases to use gmap for complex alignments containing multiple splices or indels */
  var gmapMode: Option[String] = config("gmap_mode")

  /** try gmap pairsearch on nearby genomic regions if best score (the total */
  var triggerScoreForGmap: Option[Int] = config("trigger_score_for_gmap")

  /** keep gmap hit only if it has this many consecutive matches (default 20) */
  var gmapMinMatchLength: Option[Int] = config("gmap_min_match_length")

  /** extra mismatch/indel score allowed for gmap alignments (default 3) */
  var gmapAllowance: Option[Int] = config("gmap_allowance")

  /** perform gmap pairsearch on nearby genomic regions up to this many */
  var maxGmapPairsearch: Option[Int] = config("max_gmap_pairsearch")

  /** perform gmap terminal on nearby genomic regions up to this many */
  var maxGmapTerminal: Option[Int] = config("max_gmap_terminal")

  /** perform gmap improvement on nearby genomic regions up to this many */
  var maxGmapImprovement: Option[Int] = config("max_gmap_improvement")

  /** allow microexons only if one of the splice site probabilities is */
  var microExonSpliceprob: Option[Float] = config("microexon_spliceprob")

  /** look for novel splicing (0=no (default), 1=yes) */
  var novelSplicing: Option[Int] = config("novelsplicing")

  /** directory for splicing involving known sites or known introns, */
  var splicingDir: Option[String] = config("splicingdir")

  /** look for splicing involving known sites or known introns */
  var useSplicing: Option[String] = config("use_splicing")

  /** for ambiguous known splicing at ends of the read, do not clip at the */
  var ambigSpliceNoclip: Boolean = config("ambig_splice_noclip", default = false)

  /** definition of local novel splicing event (default 200000) */
  var localSpliceDist: Option[Int] = config("localsplicedist")

  /** distance to look for novel splices at the ends of reads (default 50000) */
  var novelEndSplicedist: Option[Int] = config("novelend_splicedist")

  /** penalty for a local splice (default 0).  counts against mismatches allowed */
  var localSplicePenalty: Option[Int] = config("local_splice_penalty")

  /** penalty for a distant splice (default 1).  a distant splice is one where */
  var distantSplicePenalty: Option[Int] = config("distant_splice_penalty")

  /** minimum length at end required for distant spliced alignments (default 20, min */
  var distantSpliceEndlength: Option[Int] = config("distant_splice_endlength")

  /** minimum length at end required for short-end spliced alignments (default 2, */
  var shortendSpliceEndlength: Option[Int] = config("shortend_splice_endlength")

  /** minimum identity at end required for distant spliced alignments (default 0.95) */
  var distantSpliceIdentity: Option[Float] = config("distant_splice_identity")

  /** (not currently implemented) */
  var antiStrandedPenalty: Option[Int] = config("antistranded_penalty")

  /** report distant splices on the same chromosome as a single splice, if possible */
  var mergeDistantSamechr: Boolean = config("merge_distant_samechr", default = false)

  /** max total genomic length for dna-seq paired reads, or other reads */
  var pairmaxDna: Option[Int] = config("pairmax_dna")

  /** max total genomic length for rna-seq paired reads, or other reads */
  var pairmaxRna: Option[Int] = config("pairmax_rna")

  /** expected paired-end length, used for calling splices in medial part of */
  var pairExpect: Option[Int] = config("pairexpect")

  /** allowable deviation from expected paired-end length, used for */
  var pairDev: Option[Int] = config("pairdev")

  /** protocol for input quality scores.  allowed values: */
  var qualityProtocol: Option[String] = config("quality_protocol")

  /** fastq quality scores are zero at this ascii value */
  var qualityZeroScore: Option[Int] = config("quality_zero_score")

  /** shift fastq quality scores by this amount in output */
  var qualityPrintShift: Option[Int] = config("quality_print_shift")

  /** maximum number of paths to print (default 100) */
  var npaths: Option[Int] = config("npaths")

  /** if more than maximum number of paths are found, */
  var quietIfExcessive: Boolean = config("quiet_if_excessive", default = false)

  /** print output in same order as input (relevant */
  var ordered: Boolean = config("ordered", default = false)

  /** for gsnap output in snp-tolerant alignment, shows all differences */
  var showRefdiff: Boolean = config("show_refdiff", default = false)

  /** for paired-end reads whose alignments overlap, clip the overlapping region */
  var clipOverlap: Boolean = config("clip_overlap", default = false)

  /** print detailed information about snps in reads (works only if -v also selected) */
  var printSnps: Boolean = config("print_snps", default = false)

  /** print only failed alignments, those with no results */
  var failsOnly: Boolean = config("failsonly", default = false)

  /** exclude printing of failed alignments */
  var noFails: Boolean = config("nofails", default = false)

  /** print completely failed alignments as input fasta or fastq format */
  var failsAsInput: Boolean = config("fails_as_input", default = false)

  /** another format type, other than default */
  var format: Option[String] = config("format")

  /** basename for multiple-file output, separately for nomapping, */
  var splitOutput: Option[String] = config("split_output")

  /** when --split-output is given, this flag will append output to the */
  var appendOutput: Boolean = config("append_output", default = false)

  /** buffer size, in queries, for output thread (default 1000).  when the number */
  var outputBufferSize: Option[Int] = config("output_buffer_size")

  /** do not print headers beginning with '@' */
  var noSamHeaders: Boolean = config("no_sam_headers", default = false)

  /** print headers only for this batch, as specified by -q */
  var samHeadersBatch: Option[Int] = config("sam_headers_batch")

  /** insert 0m in cigar between adjacent insertions and deletions */
  var samUse0M: Boolean = config("sam_use_0M", default = false)

  /** allows multiple alignments to be marked as primary if they */
  var samMultiplePrimaries: Boolean = config("sam_multiple_primaries", default = false)

  /** for rna-seq alignments, disallows xs:a:? when the sense direction */
  var forceXsDir: Boolean = config("force_xs_dir", default = false)

  /** in md string, when known snps are given by the -v flag, */
  var mdLowercaseSnp: Boolean = config("md_lowercase_snp", default = false)

  /** value to put into read-group id (rg-id) field */
  var readGroupId: Option[String] = config("read_group_id")

  /** value to put into read-group name (rg-sm) field */
  var readGroupName: Option[String] = config("read_group_name")

  /** value to put into read-group library (rg-lb) field */
  var readGroupLibrary: Option[String] = config("read_group_library")

  /** value to put into read-group library (rg-pl) field */
  var readGroupPlatform: Option[String] = config("read_group_platform")

  def versionRegex: Regex = """.* version (.*)""".r
  def versionCommand: String = executable + " --version"

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if ((!gunzip && !bunzip2) && input.forall(_.getName.endsWith(".gz"))) {
      logger.debug("Fastq with .gz extension found, enabled --gunzip option")
      gunzip = true
    }
  }

  def cmdLine: String = {
    required(executable) +
      optional("--dir", dir) +
      optional("--db", db) +
      optional("--use-sarray", useSarray) +
      optional("--kmer", kmer) +
      optional("--sampling", sampling) +
      optional("--part", part) +
      optional("--input-buffer-size", inputBufferSize) +
      optional("--barcode-length", barcodeLength) +
      optional("--orientation", orientation) +
      optional("--fastq-id-start", fastqIdStart) +
      optional("--fastq-id-end", fastqIdEnd) +
      conditional(forceSingleEnd, "--force-single-end") +
      optional("--filter-chastity", filterChastity) +
      conditional(allowPeNameMismatch, "--allow-pe-name-mismatch") +
      conditional(gunzip, "--gunzip") +
      conditional(bunzip2, "--bunzip2") +
      optional("--batch", batch) +
      optional("--expand-offsets", expandOffsets) +
      optional("--max-mismatches", maxMismatches) +
      optional("--query-unk-mismatch", queryUnkMismatch) +
      optional("--genome-unk-mismatch", genomeUnkMismatch) +
      optional("--maxsearch", maxsearch) +
      optional("--terminal-threshold", terminalThreshold) +
      optional("--terminal-output-minlength", terminalOutputMinlength) +
      optional("--indel-penalty", indelPenalty) +
      optional("--indel-endlength", indelEndlength) +
      optional("--max-middle-insertions", maxMiddleInsertions) +
      optional("--max-middle-deletions", maxMiddleDeletions) +
      optional("--max-end-insertions", maxEndInsertions) +
      optional("--max-end-deletions", maxEndDeletions) +
      optional("--suboptimal-levels", suboptimalLevels) +
      optional("--adapter-strip", adapterStrip) +
      optional("--trim-mismatch-score", trimMismatchScore) +
      optional("--trim-indel-score", trimIndelScore) +
      optional("--snpsdir", snpsdir) +
      optional("--use-snps", useSnps) +
      optional("--cmetdir", cmetdir) +
      optional("--atoidir", atoidir) +
      optional("--mode", mode) +
      optional("--tallydir", tallydir) +
      optional("--use-tally", useTally) +
      optional("--runlengthdir", runLengthDir) +
      optional("--use-runlength", useRunlength) +
      optional("--nthreads", threads) +
      optional("--gmap-mode", gmapMode) +
      optional("--trigger-score-for-gmap", triggerScoreForGmap) +
      optional("--gmap-min-match-length", gmapMinMatchLength) +
      optional("--gmap-allowance", gmapAllowance) +
      optional("--max-gmap-pairsearch", maxGmapPairsearch) +
      optional("--max-gmap-terminal", maxGmapTerminal) +
      optional("--max-gmap-improvement", maxGmapImprovement) +
      optional("--microexon-spliceprob", microExonSpliceprob) +
      optional("--novelsplicing", novelSplicing) +
      optional("--splicingdir", splicingDir) +
      optional("--use-splicing", useSplicing) +
      conditional(ambigSpliceNoclip, "--ambig-splice-noclip") +
      optional("--localsplicedist", localSpliceDist) +
      optional("--novelend-splicedist", novelEndSplicedist) +
      optional("--local-splice-penalty", localSplicePenalty) +
      optional("--distant-splice-penalty", distantSplicePenalty) +
      optional("--distant-splice-endlength", distantSpliceEndlength) +
      optional("--shortend-splice-endlength", shortendSpliceEndlength) +
      optional("--distant-splice-identity", distantSpliceIdentity) +
      optional("--antistranded-penalty", antiStrandedPenalty) +
      conditional(mergeDistantSamechr, "--merge-distant-samechr") +
      optional("--pairmax-dna", pairmaxDna) +
      optional("--pairmax-rna", pairmaxRna) +
      optional("--pairexpect", pairExpect) +
      optional("--pairdev", pairDev) +
      optional("--quality-protocol", qualityProtocol) +
      optional("--quality-zero-score", qualityZeroScore) +
      optional("--quality-print-shift", qualityPrintShift) +
      optional("--npaths", npaths) +
      conditional(quietIfExcessive, "--quiet-if-excessive") +
      conditional(ordered, "--ordered") +
      conditional(showRefdiff, "--show-refdiff") +
      conditional(clipOverlap, "--clip-overlap") +
      conditional(printSnps, "--print-snps") +
      conditional(failsOnly, "--failsonly") +
      conditional(noFails, "--nofails") +
      conditional(failsAsInput, "--fails-as-input") +
      optional("--format", format) +
      optional("--split-output", splitOutput) +
      conditional(appendOutput, "--append-output") +
      optional("--output-buffer-size", outputBufferSize) +
      conditional(noSamHeaders, "--no-sam-headers") +
      optional("--sam-headers-batch", samHeadersBatch) +
      conditional(samUse0M, "--sam-use-0M") +
      conditional(samMultiplePrimaries, "--sam-multiple-primaries") +
      conditional(forceXsDir, "--force-xs-dir") +
      conditional(mdLowercaseSnp, "--md-lowercase-snp") +
      optional("--read-group-id", readGroupId) +
      optional("--read-group-name", readGroupName) +
      optional("--read-group-library", readGroupLibrary) +
      optional("--read-group-platform", readGroupPlatform) +
      repeat(input) +
      " > " + required(output)
  }
}
