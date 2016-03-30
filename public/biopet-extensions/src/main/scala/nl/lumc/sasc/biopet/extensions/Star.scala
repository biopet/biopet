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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import com.beust.jcommander.Strings
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction, Reference }
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Extension for STAR
 */
class Star(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  @Input(doc = "The reference file for the bam files.", required = false)
  var reference: File = null

  @Input(doc = "Fastq file R1", required = false)
  var R1: File = _

  @Input(doc = "Fastq file R2", required = false)
  var R2: File = _

  @Output(doc = "Output SAM file", required = false)
  var outputSam: File = _

  @Output(doc = "Output tab file", required = false)
  var outputTab: File = _

  @Input(doc = "sjdbFileChrStartEnd file", required = false) /**Can be a list of file Paths TO BE CHECKED  **/
  var sjdbFileChrStartEnd: File = _

  @Output(doc = "Output genome file", required = false)
  var outputGenome: File = _

  @Output(doc = "Output SA file", required = false)
  var outputSA: File = _

  @Output(doc = "Output SAindex file", required = false)
  var outputSAindex: File = _

  executable = config("exe", "STAR")

  def versionCommand = executable + " --version"
  def versionRegex = """(.*)""".r

  @Argument(doc = "Output Directory")
  var outputDir: File = _

  var genomeDir: File = null
  var runmode: String = _
  var outFileNamePrefix: String = _
  var runThreadN: Option[Int] = config("runthreadn")

  var runDirPerm: String = config("rundirperm")
  var runRNGseed: Option[Int] = config("runrngseed")
  var genomeLoad: String = config("genomeload")

  var genomeFastaFiles: String = config("genomefastafiles")
  /** can be a list of strings **/
  var genomeChrBinNbits: Option[Int] = config("genomechrbinnbits")
  var genomeSAindexNbases: Option[Int] = config("genomesaindexnbases")
  var genomeSAsparseD: Option[Int] = config("genomesasparsed")

  var sjdbGTFfile: String = config("sjdbgtfile")
  var sjdbGTFchrPrefix: String = config("sjdbgtfchrprefix")
  var sjdbGTFfeatureExon: String = config("sjdbgtffeatureexon")
  var sjdbGTFtagExonParentTranscript: String = config("sjdbgtftagexonparenttranscript")
  var sjdbGTFtagExonParentGene: String = config("sjdbgtftagexonparentgene")
  var sjdbOverhang: Option[Int] = config("sjdboverhang")
  var sjdbScore: Option[Int] = config("sjdbscore")
  var sjdbInsertSave: String = config("sjdbinsertsave")

  var readFilesCommand: String = config("readfilescommand")
  var readMapNumber: Option[Int] = config("readmapnumber")
  var readMatesLengthsIn: String = config("readmateslengthsin")
  var readNameSeparator: String = config("readnameseparator")
  var clip3pNbases: Option[Int] = config("clip3pnbases")
  var clip5pNbases: Option[Int] = config("clip5pnbases")
  var clip3pAdapterSeq: String = config("clip3adapterseq")
  var clip3pAdapterMMp: String = config("clip3adaptermmp")
  var clip3pAfterAdapterNbases: Option[Int] = config("clip3afteradapternbases")
  var limitGenomeGenerateRAM: Option[Int] = config("limitgenomegenerateram")
  var limitIObufferSize: Option[Int] = config("limitiobuffersize")
  var limitOutSAMoneReadBytes: Option[Int] = config("limitoutsamonereadbytes")
  var limitOutSJoneRead: Option[Int] = config("limitoutsjoneread")
  var limitOutSJcollapsed: Option[Int] = config("limitoutsjcollapsed")
  var limitBAMsortRAM: Option[Int] = config("limitbamsortram")
  var limitSjdbInsertNsj: Option[Int] = config("limitsjdbinsertnsj")

  var outTmpDir: String = config("outtmpdir")
  var outStd: String = config("outstd")
  var outReadsUnmapped: String = config("outreadsunmapped")
  var outQSconversionAdd: Option[Int] = config("outqsconversionadd")
  var outMultimapperOrder: String = config("outmultimapperorder")
  var outSAMtype: String = config("outsamtype")
  var outSAMmode: String = config("outsammode")
  var outSAMstrandField: String = config("outsamstrandfield")
  var outSAMattributes: String = config("outsamattributes")
  var outSAMattrIHstart: Option[Int] = config("outsamattrihstart")
  var outSAMunmapped: String = config("outsamunmapped")
  var outSAMorder: String = config("outsamorder")
  var outSAMprimaryFlag: String = config("outsamprimaryflag")
  var outSAMreadID: String = config("outsamreadid")
  var outSAMmapqUnique: Option[Int] = config("outsammapqunique")
  var outSAMflagOR: Option[Int] = config("outsamflagor")
  var outSAMflagAND: Option[Int] = config("outsamflagand")
  var outSAMattrRGline: String = config("outsamattrrgline")
  var outSAMheaderHD: String = config("outsamheaderhd")
  var outSAMheaderPG: String = config("outsamheaderpg")
  var outSAMheaderCommentFile: String = config("outsamheadercommentfile")
  var outSAMfilter: String = config("outsamfilter")
  var outSAMmultNmax: Option[Int] = config("outsammultnmax")
  var outBAMcompression: Option[Int] = config("outbamcompression")
  var outBAMsortingThreadN: Option[Int] = config("outbamsortingthreadn")
  var bamRemoveDuplicatesType: String = config("bamremoveduplicatestype")
  var bamRemoveDuplicatesMate2basesN: Option[Int] = config("bamremoveduplicatesmate2basesn")

  var outWigType: String = config("outwigtype")
  var outWigStrand: String = config("outwigstrand")
  var outWigReferencesPrefix: String = config("outwigreferencesprefix")
  var outWigNorm: String = config("outwignorm")
  var outFilterType: String = config("outfiltertype")
  var outFilterMultimapScoreRange: Option[Int] = config("outfiltermultimapscorerange")
  var outFilterMultimapNmax: Option[Int] = config("outfiltermultimapnmax")
  var outFilterMismatchNmax: Option[Int] = config("outfiltermismatchnmax")
  var outFilterMismatchNoverLmax: Option[Int] = config("outfiltermismatchnoverlmax")
  var outFilterMismatchNoverReadLmax: Option[Int] = config("outfiltermismatchnoverreadlmax")
  var outFilterScoreMin: Option[Int] = config("outfilterscoremin")
  var outFilterScoreMinOverLread: Option[Float] = config("outfilterscoreminoverlread")
  var outFilterMatchNmin: Option[Int] = config("outfiltermatchnmin")
  var outFilterMatchNminOverLread: Option[Int] = config("outfiltermatchnminoverload")
  var outFilterIntronMotifs: String = config("outfilterintronmotifs")

  var outSJfilterReads: String = config("outsjfilterreads")
  //  var outSJfilterOverhangMin: List[String]  = config("outsjfilteroverhandmin",default = List.empty[String])
  //  var outSJfilterCountUniqueMin: List[String] = config("outsjfiltercountuniquemin",default = List.empty[String])
  //  var outSJfilterCountTotalMin: List[String] = config("outsjfiltercounttotalmin",default = List.empty[String])
  //  var outSJfilterDistToOtherSJmin: List[String] = config("outsjfilterdisttoothersjmin",default = List.empty[String])
  //  var outSJfilterIntronMaxVsReadN: List[String] = config("outsjfilterintronmaxvsreadn",default = List.empty[String])

  var scoreGap: Option[Int] = config("scoregap")
  var scoreGapNoncan: Option[Int] = config("scoregapnoncan")
  var scoreGapGCAG: Option[Int] = config("scoregapgcag")
  var scoreGapATAC: Option[Int] = config("scoregapatac")
  var scoreGenomicLengthLog2scale: Option[Float] = config("scoregenomiclengthlog2scale")
  var scoreDelOpen: Option[Int] = config("scoredelopen")
  var scoreDelBase: Option[Int] = config("scoredelbase")
  var scoreInsOpen: Option[Int] = config("scoreinsopen")
  var scoreInsBase: Option[Int] = config("scoreinsbase")
  var scoreStitchSJshift: Option[Int] = config("scorestitchsjshift")

  var seedSearchStartLmax: Option[Int] = config("seedsearchstartlmax")
  var seedSearchStartLmaxOverLread: Option[Float] = config("seedsearchstartlmaxoverlread")
  var seedSearchLmax: Option[Int] = config("seedsearchlmax")
  var seedMultimapNmax: Option[Int] = config("seedmultimapnmax")
  var seedPerReadNmax: Option[Int] = config("seedperreadnmax")
  var seedPerWindowNmax: Option[Int] = config("seedperwindownmax")
  var seedNoneLociPerWindow: Option[Int] = config("seednonlociperwindow")
  var alignIntronMin: Option[Int] = config("alignintronmin")
  var alignIntronMax: Option[Int] = config("alignintronmax")
  var alignMatesGapMax: Option[Int] = config("alignmatesgapmax")
  var alignSJoverhangMin: Option[Int] = config("alignsjoverhangmin")
  var alignSJstitchMismatchNmax: Option[Int] = config("alignsjstitchmismatchnmax")
  var alignSJDBoverhangMin: Option[Int] = config("alignsjdboverhangmin")
  var alignSplicedMateMapLmin: Option[Int] = config("alignsplicedmatemaplmin")
  var alignSplicedMateMapLminOverLmate: Option[Float] = config("alignsplicedmatemaplminoverlmate")
  var alignWindowsPerReadNmax: Option[Int] = config("alignwindowsperreadnmax")
  var alignTranscriptsPerWindowNmax: Option[Int] = config("aligntranscriptsperwindownmax")
  var alignTranscriptsPerReadNmax: Option[Int] = config("aligntranscriptsperreadnmax")
  var alignEndsType: String = config("alignendstype")
  var alignSoftClipAtReferenceEnds: String = config("alignsoftclipatreferenceends")

  var winAnchorMultimapNmax: Option[Int] = config("winanchormultimapnmax")
  var winBinNbits: Option[Int] = config("winbinnbits")
  var winAnchorDistNbins: Option[Int] = config("winanchordistnbins")
  var winFlankNbins: Option[Int] = config("winflaknbins")
  var chimOutType: String = config("chimOutType")
  var chimSegmentMin: Option[Int] = config("chimsegmentmin")
  var chimScoreMin: Option[Int] = config("chimscoremin")
  var chimScoreDropMax: Option[Int] = config("chimscoredropmax")
  var chimScoreSeparation: Option[Int] = config("chimscoreseparation")
  var chimScoreJunctionNonGTAG: Option[Int] = config("chimscorejunctionnongtag")
  var chimJunctionOverhangMin: Option[Int] = config("chimjunctionoverhangmin")
  var chimSegmentReadGapMax: Option[Int] = config("chimsegmentreadgapmax")
  var chimFilter: String = config("chimfilter")

  var quantMode: String = config("quantmode")
  var quantTranscriptomeBAMcompression: Option[Int] = config("quanttranscriptomebamcompression")
  var quantTranscriptomeBan: String = config("quanttranscriptomebam")

  var twopassMode: String = config("twopassmode")
  var twopass1readsN: Option[Int] = config("twopass1readsn")

  override def defaultCoreMemory = 6.0
  override def defaultThreads = 8

  /** Sets output files for the graph */
  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
    if (outFileNamePrefix != null && !outFileNamePrefix.endsWith(".")) outFileNamePrefix += "."
    val prefix = if (outFileNamePrefix != null) outputDir + File.separator + outFileNamePrefix else outputDir + File.separator
    if (runmode == null) {
      outputSam = new File(prefix + "Aligned.out.sam")
      outputTab = new File(prefix + "SJ.out.tab")
      genomeDir = config("genomeDir", new File(reference.getAbsoluteFile.getParent, "star"))
    } else if (runmode == "genomeGenerate") {
      genomeDir = outputDir
      outputGenome = new File(prefix + "Genome")
      outputSA = new File(prefix + "SA")
      outputSAindex = new File(prefix + "SAindex")
      sjdbOverhang = config("sjdboverhang")
    }
  }

  /** Returns command to execute */
  def cmdLine = {
    var cmd: String = required("cd", outputDir) + " && " + required(executable)
    if (runmode != null && runmode == "genomeGenerate") { // Create index
      cmd += required("--runMode", runmode) +
        required("--genomeFastaFiles", reference)
    } else { // Aligner
      cmd += required("--readFilesIn", R1) + optional(R2)
    }
    cmd += required("--genomeDir", genomeDir) +
      optional("--sjdbFileChrStartEnd", sjdbFileChrStartEnd) +
      optional("--runThreadN", threads) +
      optional("--outFileNamePrefix", outFileNamePrefix) +
      optional("--sjdbOverhang", sjdbOverhang) +
      optional("--runDirPerm", runDirPerm) +
      optional("--genomeFastaFiles", genomeFastaFiles) +
      optional("--genomeChrBinNbits", genomeChrBinNbits) +
      optional("--genomeSAindexNbases", genomeSAindexNbases) +
      optional("--genomeSAsparseD", genomeSAsparseD) +
      optional("--sjdbGTFfile", sjdbGTFfile) +
      optional("--sjdbGTFchrPrefix", sjdbGTFchrPrefix) +
      optional("--sjdbGTFfeatureExon", sjdbGTFfeatureExon) +
      optional("--sjdbGTFtagExonParentTranscript", sjdbGTFtagExonParentTranscript) +
      optional("--sjdbGTFtagExonParentGene", sjdbGTFtagExonParentGene) +
      optional("--sjdbOverhang", sjdbOverhang) +
      optional("--sjdbScore", sjdbScore) +
      optional("--sjdbInsertSave", sjdbInsertSave) +
      optional("--readFilesCommand", readFilesCommand) +
      optional("--readMapNumber", readMapNumber) +
      optional("--readMatesLengthsIn", readMatesLengthsIn) +
      optional("--readNameSeparator", readNameSeparator) +
      optional("--clip3pNbases", clip3pNbases) +
      optional("--clip5pNbases", clip5pNbases) +
      optional("--clip3pAdapterSeq", clip3pAdapterSeq) +
      optional("--clip3pAdapterMMp", clip3pAdapterMMp) +
      optional("--clip3pAfterAdapterNbases", clip3pAfterAdapterNbases) +
      optional("--limitGenomeGenerateRAM", limitGenomeGenerateRAM) +
      optional("--limitIObufferSize", limitIObufferSize) +
      optional("--limitOutSAMoneReadBytes", limitOutSAMoneReadBytes) +
      optional("--limitOutSJoneRead", limitOutSJoneRead) +
      optional("--limitOutSJcollapsed", limitOutSJcollapsed) +
      optional("--limitBAMsortRAM", limitBAMsortRAM) +
      optional("--limitSjdbInsertNsj", limitSjdbInsertNsj) +
      optional("--outTmpDir", outTmpDir) +
      optional("--outStd", outStd) +
      optional("--outReadsUnmapped", outReadsUnmapped) +
      optional("--outQSconversionAdd", outQSconversionAdd) +
      optional("--outMultimapperOrder", outMultimapperOrder) +
      optional("--outSAMtype", outSAMtype) +
      optional("--outSAMmode", outSAMmode) +
      optional("--outSAMstrandField", outSAMstrandField) +
      optional("--outSAMattributes", outSAMattributes) +
      optional("--outSAMattrIHstart", outSAMattrIHstart) +
      optional("--outSAMunmapped", outSAMunmapped) +
      optional("--outSAMorder", outSAMorder) +
      optional("--outSAMprimaryFlag", outSAMprimaryFlag) +
      optional("--outSAMreadID", outSAMreadID) +
      optional("--outSAMmapqUnique", outSAMmapqUnique) +
      optional("--outSAMflagOR", outSAMflagOR) +
      optional("--outSAMflagAND", outSAMflagAND) +
      optional("--outSAMattrRGline", outSAMattrRGline) +
      optional("--outSAMheaderHD", outSAMheaderHD) +
      optional("--outSAMheaderPG", outSAMheaderPG) +
      optional("--outSAMheaderCommentFile", outSAMheaderCommentFile) +
      optional("--outSAMfilter", outSAMfilter) +
      optional("--outSAMmultNmax", outSAMmultNmax) +
      optional("--outBAMcompression", outBAMcompression) +
      optional("--outBAMsortingThreadN", outBAMsortingThreadN) +
      optional("--bamRemoveDuplicatesType", bamRemoveDuplicatesType) +
      optional("--bamRemoveDuplicatesMate2basesN", bamRemoveDuplicatesMate2basesN) +
      optional("--outWigType", outWigType) +
      optional("--outWigStrand", outWigStrand) +
      optional("--outWigReferencesPrefix", outWigReferencesPrefix) +
      optional("--outWigNorm", outWigNorm) +
      optional("--outFilterType", outFilterType) +
      optional("--outFilterMultimapScoreRange", outFilterMultimapScoreRange) +
      optional("--outFilterMultimapNmax", outFilterMultimapNmax) +
      optional("--outFilterMismatchNmax", outFilterMismatchNmax) +
      optional("--outFilterMismatchNoverLmax", outFilterMismatchNoverLmax) +
      optional("--outFilterMismatchNoverReadLmax", outFilterMismatchNoverReadLmax) +
      optional("--outFilterScoreMin", outFilterScoreMin) +
      optional("--outFilterScoreMinOverLread", outFilterScoreMinOverLread) +
      optional("--outFilterMatchNmin", outFilterMatchNmin) +
      optional("--outFilterMatchNminOverLread", outFilterMatchNminOverLread) +
      optional("--outFilterIntronMotifs", outFilterIntronMotifs) +
      optional("--outSJfilterReads", outSJfilterReads) +
      optional("--scoreGap", scoreGap) +
      optional("--scoreGapNoncan", scoreGapNoncan) +
      optional("--scoreGapGCAG", scoreGapGCAG) +
      optional("--scoreGapATAC", scoreGapATAC) +
      optional("--scoreGenomicLengthLog2scale", scoreGenomicLengthLog2scale) +
      optional("--scoreDelOpen", scoreDelOpen) +
      optional("--scoreDelBase", scoreDelBase) +
      optional("--scoreInsOpen", scoreInsOpen) +
      optional("--scoreInsBase", scoreInsBase) +
      optional("--scoreStitchSJshift", scoreStitchSJshift) +
      optional("--seedSearchStartLmax", seedSearchStartLmax) +
      optional("--seedSearchStartLmaxOverLread", seedSearchStartLmaxOverLread) +
      optional("--seedSearchLmax", seedSearchLmax) +
      optional("--seedMultimapNmax", seedMultimapNmax) +
      optional("--seedPerReadNmax", seedPerReadNmax) +
      optional("--seedPerWindowNmax", seedPerWindowNmax) +
      optional("--seedNoneLociPerWindow", seedNoneLociPerWindow) +
      optional("--alignIntronMin", alignIntronMin) +
      optional("--alignIntronMax", alignIntronMax) +
      optional("--alignMatesGapMax", alignMatesGapMax) +
      optional("--alignSJoverhangMin", alignSJoverhangMin) +
      optional("--alignSJstitchMismatchNmax", alignSJstitchMismatchNmax) +
      optional("--alignSJDBoverhangMin", alignSJDBoverhangMin) +
      optional("--alignSplicedMateMapLmin", alignSplicedMateMapLmin) +
      optional("--alignSplicedMateMapLminOverLmate", alignSplicedMateMapLminOverLmate) +
      optional("--alignWindowsPerReadNmax", alignWindowsPerReadNmax) +
      optional("--alignTranscriptsPerWindowNmax", alignTranscriptsPerWindowNmax) +
      optional("--alignTranscriptsPerReadNmax", alignTranscriptsPerReadNmax) +
      optional("--alignEndsType", alignEndsType) +
      optional("--alignSoftClipAtReferenceEnds", alignSoftClipAtReferenceEnds) +
      optional("--winAnchorMultimapNmax", winAnchorMultimapNmax) +
      optional("--winBinNbits", winBinNbits) +
      optional("--winAnchorDistNbins", winAnchorDistNbins) +
      optional("--winFlankNbins", winFlankNbins) +
      optional("--chimOutType", chimOutType) +
      optional("--chimSegmentMin", chimSegmentMin) +
      optional("--chimScoreMin", chimScoreMin) +
      optional("--chimScoreDropMax", chimScoreDropMax) +
      optional("--chimScoreSeparation", chimScoreSeparation) +
      optional("--chimScoreJunctionNonGTAG", chimScoreJunctionNonGTAG) +
      optional("--chimJunctionOverhangMin", chimJunctionOverhangMin) +
      optional("--chimSegmentReadGapMax", chimSegmentReadGapMax) +
      optional("--chimFilter", chimFilter) +
      optional("--quantMode", quantMode) +
      optional("--quantTranscriptomeBAMcompression", quantTranscriptomeBAMcompression) +
      optional("--quantTranscriptomeBan", quantTranscriptomeBan) +
      optional("--twopassMode", twopassMode) +
      optional("--twopass1readsN", twopass1readsN)

    cmd
  }
}

object Star {
  /**
   * Create default star
   * @param configurable root object
   * @param R1 R1 fastq file
   * @param R2 R2 fastq file
   * @param outputDir Outputdir for Star
   * @param isIntermediate When set true jobs are flaged as intermediate
   * @param deps Deps to add to wait on run
   * @return Return Star
   *
   */
  def apply(configurable: Configurable, R1: File, R2: Option[File], outputDir: File, isIntermediate: Boolean = false, deps: List[File] = Nil): Star = {
    val star = new Star(configurable)
    star.R1 = R1
    R2.foreach(R2 => star.R2 = R2)
    star.outputDir = outputDir
    star.isIntermediate = isIntermediate
    star.deps = deps
    star.beforeGraph()
    star
  }

  /**
   * returns Star with 2pass star method
   * @param configurable root object
   * @param R1 R1 fastq file
   * @param R2 R2 fastq file
   * @param outputDir Outputdir for Star
   * @param isIntermediate When set true jobs are flaged as intermediate
   * @param deps Deps to add to wait on run
   * @return Return Star
   */
  def _2pass(configurable: Configurable,
             R1: File,
             R2: Option[File],
             outputDir: File,
             isIntermediate: Boolean = false,
             deps: List[File] = Nil): (File, List[Star]) = {
    val starCommandPass1 = Star(configurable, R1, R2, new File(outputDir, "aln-pass1"))
    starCommandPass1.isIntermediate = isIntermediate
    starCommandPass1.deps = deps
    starCommandPass1.beforeGraph()

    val starCommandReindex = new Star(configurable)
    starCommandReindex.sjdbFileChrStartEnd = starCommandPass1.outputTab
    starCommandReindex.outputDir = new File(outputDir, "re-index")
    starCommandReindex.runmode = "genomeGenerate"
    starCommandReindex.isIntermediate = isIntermediate
    starCommandReindex.beforeGraph()

    val starCommandPass2 = Star(configurable, R1, R2, new File(outputDir, "aln-pass2"))
    starCommandPass2.genomeDir = starCommandReindex.outputDir
    starCommandPass2.isIntermediate = isIntermediate
    starCommandPass2.deps = deps
    starCommandPass2.beforeGraph()

    (starCommandPass2.outputSam, List(starCommandPass1, starCommandReindex, starCommandPass2))
  }
}