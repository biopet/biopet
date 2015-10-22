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
package nl.lumc.sasc.biopet.extensions.gmap

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction, Reference }
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Wrapper for the gsnap command line tool
 * Written based on gsnap version 2014-05-15
 */
class Gsnap(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {

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
  var output: File = null

  /** genome directory */
  @Argument(doc = "Directory of genome database")
  var dir: Option[File] = config("dir")

  /** genome database */
  @Argument(doc = "Genome database name", required = true)
  var db: String = config("db")

  /** whether to use a suffix array, which will give increased speed */
  var use_sarray: Option[Int] = config("use_sarray")

  /** kmer size to use in genome database (allowed values: 16 or less) */
  var kmer: Option[Int] = config("kmer")

  /** sampling to use in genome database */
  var sampling: Option[Int] = config("sampling")

  /** process only the i-th out of every n sequences */
  var part: Option[String] = config("part")

  /** size of input buffer (program reads this many sequences at a time)*/
  var input_buffer_size: Option[Int] = config("input_buffer_size")

  /** amount of barcode to remove from start of read */
  var barcode_length: Option[Int] = config("barcode_length")

  /** orientation of paired-end reads */
  var orientation: Option[String] = config("orientation")

  /** starting position of identifier in fastq header, space-delimited (>= 1) */
  var fastq_id_start: Option[Int] = config("fastq_id_start")

  /** ending position of identifier in fastq header, space-delimited (>= 1) */
  var fastq_id_end: Option[Int] = config("fastq_id_end")

  /** when multiple fastq files are provided on the command line, gsnap assumes */
  var force_single_end: Boolean = config("force_single_end", default = false)

  /** skips reads marked by the illumina chastity program.  expecting a string */
  var filter_chastity: Option[String] = config("filter_chastity")

  /** allows accession names of reads to mismatch in paired-end files */
  var allow_pe_name_mismatch: Boolean = config("allow_pe_name_mismatch", default = false)

  /** uncompress gzipped input files */
  var gunzip: Boolean = config("gunzip", default = false)

  /** uncompress bzip2-compressed input files */
  var bunzip2: Boolean = config("bunzip2", default = false)

  /** batch mode (default = 2) */
  var batch: Option[Int] = config("batch")

  /** whether to expand the genomic offsets index */
  var expand_offsets: Option[Int] = config("expand_offsets")

  /** maximum number of mismatches allowed (if not specified, then */
  var max_mismatches: Option[Float] = config("max_mismatches")

  /** whether to count unknown (n) characters in the query as a mismatch */
  var query_unk_mismatch: Option[Int] = config("query_unk_mismatch")

  /** whether to count unknown (n) characters in the genome as a mismatch */
  var genome_unk_mismatch: Option[Int] = config("genome_unk_mismatch")

  /** maximum number of alignments to find (default 1000) */
  var maxsearch: Option[Int] = config("maxsearch")

  /** threshold for computing a terminal alignment (from one end of the */
  var terminal_threshold: Option[Int] = config("terminal_threshold")

  /** threshold alignment length in bp for a terminal alignment result to be printed (in bp) */
  var terminal_output_minlength: Option[Int] = config("terminal_output_minlength")

  /** penalty for an indel (default 2) */
  var indel_penalty: Option[Int] = config("indel_penalty")

  /** minimum length at end required for indel alignments (default 4) */
  var indel_endlength: Option[Int] = config("indel_endlength")

  /** maximum number of middle insertions allowed (default 9) */
  var max_middle_insertions: Option[Int] = config("max_middle_insertions")

  /** maximum number of middle deletions allowed (default 30) */
  var max_middle_deletions: Option[Int] = config("max_middle_deletions")

  /** maximum number of end insertions allowed (default 3) */
  var max_end_insertions: Option[Int] = config("max_end_insertions")

  /** maximum number of end deletions allowed (default 6) */
  var max_end_deletions: Option[Int] = config("max_end_deletions")

  /** report suboptimal hits beyond best hit (default 0) */
  var suboptimal_levels: Option[Int] = config("suboptimal_levels")

  /** method for removing adapters from reads.  currently allowed values: off, paired */
  var adapter_strip: Option[String] = config("adapter_strip")

  /** score to use for mismatches when trimming at ends (default is -3; */
  var trim_mismatch_score: Option[Int] = config("trim_mismatch_score")

  /** score to use for indels when trimming at ends (default is -4; */
  var trim_indel_score: Option[Int] = config("trim_indel_score")

  /** directory for snps index files (created using snpindex) (default is */
  var snpsdir: Option[String] = config("snpsdir")

  /** use database containing known snps (in <string>.iit, built */
  var use_snps: Option[String] = config("use_snps")

  /** directory for methylcytosine index files (created using cmetindex) */
  var cmetdir: Option[String] = config("cmetdir")

  /** directory for a-to-i rna editing index files (created using atoiindex) */
  var atoidir: Option[String] = config("atoidir")

  /** alignment mode: standard (default), cmet-stranded, cmet-nonstranded, */
  var mode: Option[String] = config("mode")

  /** directory for tally iit file to resolve concordant multiple results (default is */
  var tallydir: Option[String] = config("tallydir")

  /** use this tally iit file to resolve concordant multiple results */
  var use_tally: Option[String] = config("use_tally")

  /** directory for runlength iit file to resolve concordant multiple results (default is */
  var runlengthdir: Option[String] = config("runlengthdir")

  /** use this runlength iit file to resolve concordant multiple results */
  var use_runlength: Option[String] = config("use_runlength")

  /** cases to use gmap for complex alignments containing multiple splices or indels */
  var gmap_mode: Option[String] = config("gmap_mode")

  /** try gmap pairsearch on nearby genomic regions if best score (the total */
  var trigger_score_for_gmap: Option[Int] = config("trigger_score_for_gmap")

  /** keep gmap hit only if it has this many consecutive matches (default 20) */
  var gmap_min_match_length: Option[Int] = config("gmap_min_match_length")

  /** extra mismatch/indel score allowed for gmap alignments (default 3) */
  var gmap_allowance: Option[Int] = config("gmap_allowance")

  /** perform gmap pairsearch on nearby genomic regions up to this many */
  var max_gmap_pairsearch: Option[Int] = config("max_gmap_pairsearch")

  /** perform gmap terminal on nearby genomic regions up to this many */
  var max_gmap_terminal: Option[Int] = config("max_gmap_terminal")

  /** perform gmap improvement on nearby genomic regions up to this many */
  var max_gmap_improvement: Option[Int] = config("max_gmap_improvement")

  /** allow microexons only if one of the splice site probabilities is */
  var microexon_spliceprob: Option[Float] = config("microexon_spliceprob")

  /** look for novel splicing (0=no (default), 1=yes) */
  var novelsplicing: Option[Int] = config("novelsplicing")

  /** directory for splicing involving known sites or known introns, */
  var splicingdir: Option[String] = config("splicingdir")

  /** look for splicing involving known sites or known introns */
  var use_splicing: Option[String] = config("use_splicing")

  /** for ambiguous known splicing at ends of the read, do not clip at the */
  var ambig_splice_noclip: Boolean = config("ambig_splice_noclip", default = false)

  /** definition of local novel splicing event (default 200000) */
  var localsplicedist: Option[Int] = config("localsplicedist")

  /** distance to look for novel splices at the ends of reads (default 50000) */
  var novelend_splicedist: Option[Int] = config("novelend_splicedist")

  /** penalty for a local splice (default 0).  counts against mismatches allowed */
  var local_splice_penalty: Option[Int] = config("local_splice_penalty")

  /** penalty for a distant splice (default 1).  a distant splice is one where */
  var distant_splice_penalty: Option[Int] = config("distant_splice_penalty")

  /** minimum length at end required for distant spliced alignments (default 20, min */
  var distant_splice_endlength: Option[Int] = config("distant_splice_endlength")

  /** minimum length at end required for short-end spliced alignments (default 2, */
  var shortend_splice_endlength: Option[Int] = config("shortend_splice_endlength")

  /** minimum identity at end required for distant spliced alignments (default 0.95) */
  var distant_splice_identity: Option[Float] = config("distant_splice_identity")

  /** (not currently implemented) */
  var antistranded_penalty: Option[Int] = config("antistranded_penalty")

  /** report distant splices on the same chromosome as a single splice, if possible */
  var merge_distant_samechr: Boolean = config("merge_distant_samechr", default = false)

  /** max total genomic length for dna-seq paired reads, or other reads */
  var pairmax_dna: Option[Int] = config("pairmax_dna")

  /** max total genomic length for rna-seq paired reads, or other reads */
  var pairmax_rna: Option[Int] = config("pairmax_rna")

  /** expected paired-end length, used for calling splices in medial part of */
  var pairexpect: Option[Int] = config("pairexpect")

  /** allowable deviation from expected paired-end length, used for */
  var pairdev: Option[Int] = config("pairdev")

  /** protocol for input quality scores.  allowed values: */
  var quality_protocol: Option[String] = config("quality_protocol")

  /** fastq quality scores are zero at this ascii value */
  var quality_zero_score: Option[Int] = config("quality_zero_score")

  /** shift fastq quality scores by this amount in output */
  var quality_print_shift: Option[Int] = config("quality_print_shift")

  /** maximum number of paths to print (default 100) */
  var npaths: Option[Int] = config("npaths")

  /** if more than maximum number of paths are found, */
  var quiet_if_excessive: Boolean = config("quiet_if_excessive", default = false)

  /** print output in same order as input (relevant */
  var ordered: Boolean = config("ordered", default = false)

  /** for gsnap output in snp-tolerant alignment, shows all differences */
  var show_refdiff: Boolean = config("show_refdiff", default = false)

  /** for paired-end reads whose alignments overlap, clip the overlapping region */
  var clip_overlap: Boolean = config("clip_overlap", default = false)

  /** print detailed information about snps in reads (works only if -v also selected) */
  var print_snps: Boolean = config("print_snps", default = false)

  /** print only failed alignments, those with no results */
  var failsonly: Boolean = config("failsonly", default = false)

  /** exclude printing of failed alignments */
  var nofails: Boolean = config("nofails", default = false)

  /** print completely failed alignments as input fasta or fastq format */
  var fails_as_input: Boolean = config("fails_as_input", default = false)

  /** another format type, other than default */
  var format: Option[String] = config("format")

  /** basename for multiple-file output, separately for nomapping, */
  var split_output: Option[String] = config("split_output")

  /** when --split-output is given, this flag will append output to the */
  var append_output: Boolean = config("append_output", default = false)

  /** buffer size, in queries, for output thread (default 1000).  when the number */
  var output_buffer_size: Option[Int] = config("output_buffer_size")

  /** do not print headers beginning with '@' */
  var no_sam_headers: Boolean = config("no_sam_headers", default = false)

  /** print headers only for this batch, as specified by -q */
  var sam_headers_batch: Option[Int] = config("sam_headers_batch")

  /** insert 0m in cigar between adjacent insertions and deletions */
  var sam_use_0M: Boolean = config("sam_use_0M", default = false)

  /** allows multiple alignments to be marked as primary if they */
  var sam_multiple_primaries: Boolean = config("sam_multiple_primaries", default = false)

  /** for rna-seq alignments, disallows xs:a:? when the sense direction */
  var force_xs_dir: Boolean = config("force_xs_dir", default = false)

  /** in md string, when known snps are given by the -v flag, */
  var md_lowercase_snp: Boolean = config("md_lowercase_snp", default = false)

  /** value to put into read-group id (rg-id) field */
  var read_group_id: Option[String] = config("read_group_id")

  /** value to put into read-group name (rg-sm) field */
  var read_group_name: Option[String] = config("read_group_name")

  /** value to put into read-group library (rg-lb) field */
  var read_group_library: Option[String] = config("read_group_library")

  /** value to put into read-group library (rg-pl) field */
  var read_group_platform: Option[String] = config("read_group_platform")

  def versionRegex = """.* version (.*)""".r
  def versionCommand = executable + " --version"

  def cmdLine = {
    required(executable) +
      optional("--dir", dir) +
      optional("--db", db) +
      optional("--use-sarray", use_sarray) +
      optional("--kmer", kmer) +
      optional("--sampling", sampling) +
      optional("--part", part) +
      optional("--input-buffer-size", input_buffer_size) +
      optional("--barcode-length", barcode_length) +
      optional("--orientation", orientation) +
      optional("--fastq-id-start", fastq_id_start) +
      optional("--fastq-id-end", fastq_id_end) +
      conditional(force_single_end, "--force-single-end") +
      optional("--filter-chastity", filter_chastity) +
      conditional(allow_pe_name_mismatch, "--allow-pe-name-mismatch") +
      conditional(gunzip, "--gunzip") +
      conditional(bunzip2, "--bunzip2") +
      optional("--batch", batch) +
      optional("--expand-offsets", expand_offsets) +
      optional("--max-mismatches", max_mismatches) +
      optional("--query-unk-mismatch", query_unk_mismatch) +
      optional("--genome-unk-mismatch", genome_unk_mismatch) +
      optional("--maxsearch", maxsearch) +
      optional("--terminal-threshold", terminal_threshold) +
      optional("--terminal-output-minlength", terminal_output_minlength) +
      optional("--indel-penalty", indel_penalty) +
      optional("--indel-endlength", indel_endlength) +
      optional("--max-middle-insertions", max_middle_insertions) +
      optional("--max-middle-deletions", max_middle_deletions) +
      optional("--max-end-insertions", max_end_insertions) +
      optional("--max-end-deletions", max_end_deletions) +
      optional("--suboptimal-levels", suboptimal_levels) +
      optional("--adapter-strip", adapter_strip) +
      optional("--trim-mismatch-score", trim_mismatch_score) +
      optional("--trim-indel-score", trim_indel_score) +
      optional("--snpsdir", snpsdir) +
      optional("--use-snps", use_snps) +
      optional("--cmetdir", cmetdir) +
      optional("--atoidir", atoidir) +
      optional("--mode", mode) +
      optional("--tallydir", tallydir) +
      optional("--use-tally", use_tally) +
      optional("--runlengthdir", runlengthdir) +
      optional("--use-runlength", use_runlength) +
      optional("--nthreads", threads) +
      optional("--gmap-mode", gmap_mode) +
      optional("--trigger-score-for-gmap", trigger_score_for_gmap) +
      optional("--gmap-min-match-length", gmap_min_match_length) +
      optional("--gmap-allowance", gmap_allowance) +
      optional("--max-gmap-pairsearch", max_gmap_pairsearch) +
      optional("--max-gmap-terminal", max_gmap_terminal) +
      optional("--max-gmap-improvement", max_gmap_improvement) +
      optional("--microexon-spliceprob", microexon_spliceprob) +
      optional("--novelsplicing", novelsplicing) +
      optional("--splicingdir", splicingdir) +
      optional("--use-splicing", use_splicing) +
      conditional(ambig_splice_noclip, "--ambig-splice-noclip") +
      optional("--localsplicedist", localsplicedist) +
      optional("--novelend-splicedist", novelend_splicedist) +
      optional("--local-splice-penalty", local_splice_penalty) +
      optional("--distant-splice-penalty", distant_splice_penalty) +
      optional("--distant-splice-endlength", distant_splice_endlength) +
      optional("--shortend-splice-endlength", shortend_splice_endlength) +
      optional("--distant-splice-identity", distant_splice_identity) +
      optional("--antistranded-penalty", antistranded_penalty) +
      conditional(merge_distant_samechr, "--merge-distant-samechr") +
      optional("--pairmax-dna", pairmax_dna) +
      optional("--pairmax-rna", pairmax_rna) +
      optional("--pairexpect", pairexpect) +
      optional("--pairdev", pairdev) +
      optional("--quality-protocol", quality_protocol) +
      optional("--quality-zero-score", quality_zero_score) +
      optional("--quality-print-shift", quality_print_shift) +
      optional("--npaths", npaths) +
      conditional(quiet_if_excessive, "--quiet-if-excessive") +
      conditional(ordered, "--ordered") +
      conditional(show_refdiff, "--show-refdiff") +
      conditional(clip_overlap, "--clip-overlap") +
      conditional(print_snps, "--print-snps") +
      conditional(failsonly, "--failsonly") +
      conditional(nofails, "--nofails") +
      conditional(fails_as_input, "--fails-as-input") +
      optional("--format", format) +
      optional("--split-output", split_output) +
      conditional(append_output, "--append-output") +
      optional("--output-buffer-size", output_buffer_size) +
      conditional(no_sam_headers, "--no-sam-headers") +
      optional("--sam-headers-batch", sam_headers_batch) +
      conditional(sam_use_0M, "--sam-use-0M") +
      conditional(sam_multiple_primaries, "--sam-multiple-primaries") +
      conditional(force_xs_dir, "--force-xs-dir") +
      conditional(md_lowercase_snp, "--md-lowercase-snp") +
      optional("--read-group-id", read_group_id) +
      optional("--read-group-name", read_group_name) +
      optional("--read-group-library", read_group_library) +
      optional("--read-group-platform", read_group_platform) +
      repeat(input) +
      " > " + required(output)
  }
}
