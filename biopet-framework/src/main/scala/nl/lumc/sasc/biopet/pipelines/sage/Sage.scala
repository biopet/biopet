package nl.lumc.sasc.biopet.pipelines.sage

import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.scripts.PrefixFastq
import nl.lumc.sasc.biopet.tools.BedtoolsCoverageToCounts
import nl.lumc.sasc.biopet.scripts.SquishBed
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._

class Sage(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)
  
  @Input(doc = "countBed", required = false)
  var countBed : File = _
  
  @Input(doc = "squishedCountBed, by suppling this file the auto squish job will be skipped", required = false)
  var squishedCountBed : File = _
  
  @Input(doc = "Transcriptome, used for generation of tag library", required = false)
  var transcriptome : File = _
  
  var tagsLibrary : File = _
  
  defaults ++= Map("bowtie" -> Map(
                  "m" -> 1,
                  "k" -> 1,
                  "best" -> true,
                  "strata" -> true,
                  "seedmms" -> 1
                )
              )
  
  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
    var prefixFastq: File = _
  }
  
  class SampleOutput extends AbstractSampleOutput {
    
  }
  
  def init() {
    if (!outputDir.endsWith("/")) outputDir += "/"
    if (countBed == null) countBed = config("count_bed")
    if (squishedCountBed == null) squishedCountBed = config("squished_count_bed")
    if (tagsLibrary == null) tagsLibrary = config("tags_library")
    if (transcriptome == null) transcriptome = config("transcriptome")
    if (transcriptome == null && tagsLibrary == null) 
      throw new IllegalStateException("No transcriptome or taglib found")
    if (countBed == null && squishedCountBed == null) 
      throw new IllegalStateException("No bedfile supplied, please add a countBed or squishedCountBed")
  }

  def biopetScript() {
    if (squishedCountBed == null) {
      val squishBed = SquishBed(this, countBed, outputDir)
      add(squishBed)
      squishedCountBed = squishBed.output
    }
    
    if (tagsLibrary == null) {
      val cdl = new CreateDeepsageLibrary(this)
      cdl.input = transcriptome
      cdl.output = outputDir + "taglib/tag.lib"
      cdl.noAntiTagsOutput = outputDir + "taglib/no_antisense_genes.txt"
      cdl.noTagsOutput = outputDir + "taglib/no_sense_genes.txt"
      cdl.allGenesOutput = outputDir + "taglib/all_genes.txt"
      add(cdl)
      tagsLibrary = cdl.output
    }
    
    runSamplesJobs
  }
  
  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var libraryFastqFiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir: String = globalSampleDir + sampleID + "/" 
    for ((library, libraryFiles) <- runLibraryJobs(sampleConfig)) {
      libraryFastqFiles +:= libraryFiles.prefixFastq
      libraryBamfiles +:= libraryFiles.mappedBamFile
    }
    
    val bamFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
                  else if (libraryBamfiles.size > 1) {
                    val mergeSamFiles = MergeSamFiles(this, libraryBamfiles, sampleDir)
                    add(mergeSamFiles)
                    mergeSamFiles.output
                  } else null
    val fastqFile: File = if (libraryFastqFiles.size == 1) libraryFastqFiles.head
                  else if (libraryFastqFiles.size > 1) {
                    val cat = Cat.apply(this, libraryFastqFiles, sampleDir + sampleID + ".fastq")
                    add(cat)
                    cat.output
                  } else null
    
    addBedtoolsCounts(bamFile, sampleID, sampleDir)
    addTablibCounts(fastqFile, sampleID, sampleDir)
    
    return sampleOutput
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"
    if (runConfig.contains("R1")) {
      val flexiprep = new Flexiprep(this)
      flexiprep.outputDir = runDir + "flexiprep/"
      flexiprep.input_R1 = new File(runConfig("R1").toString)
      flexiprep.skipClip = true
      flexiprep.skipTrim = true
      flexiprep.sampleName = sampleID
      flexiprep.libraryName = runID
      flexiprep.init
      flexiprep.biopetScript
      addAll(flexiprep.functions)
      
      val flexiprepOutput = for ((key,file) <- flexiprep.outputFiles if key.endsWith("output_R1")) yield file
      val prefixFastq = PrefixFastq.apply(this, flexiprepOutput.head, runDir)
      prefixFastq.prefix = config("sage_tag", default = "CATG")
      prefixFastq.deps +:= flexiprep.outputFiles("fastq_input_R1")
      add(prefixFastq)
      libraryOutput.prefixFastq = prefixFastq.output
      
      val mapping = new Mapping(this)
      mapping.skipFlexiprep = true
      mapping.skipMarkduplicates = true
      mapping.defaultAligner = "bowtie"
      mapping.input_R1 = prefixFastq.output
      mapping.RGLB = runConfig("ID").toString
      mapping.RGSM = sampleConfig("ID").toString
      if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
      if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
      if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
      mapping.outputDir = runDir
      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)
      
      if (config("library_counts", default = false).getBoolean) {
        addBedtoolsCounts(mapping.outputFiles("finalBamFile"), sampleID + "-" + runID, runDir)
        addTablibCounts(prefixFastq.output, sampleID + "-" + runID, runDir)
      }
      
      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return libraryOutput
  }
  
  def addBedtoolsCounts(bamFile:File, outputPrefix: String, outputDir: String) {
    val bedtoolsSense = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.sense.coverage", 
                         depth = false, sameStrand = true, diffStrand = false)
    val countSense = new BedtoolsCoverageToCounts(this)
    countSense.input = bedtoolsSense.output
    countSense.output = outputDir + outputPrefix + ".genome.sense.counts"
    
    val bedtoolsAntisense = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.antisense.coverage", 
                         depth = false, sameStrand = false, diffStrand = true)
    val countAntisense = new BedtoolsCoverageToCounts(this)
    countAntisense.input = bedtoolsAntisense.output
    countAntisense.output = outputDir + outputPrefix + ".genome.antisense.counts"
    
    val bedtools = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.coverage", 
                         depth = false, sameStrand = false, diffStrand = false)
    val count = new BedtoolsCoverageToCounts(this)
    count.input = bedtools.output
    count.output = outputDir + outputPrefix + ".genome.counts"
    
    add(bedtoolsSense, countSense, bedtoolsAntisense, countAntisense, bedtools, count)
  }
  
  def addTablibCounts(fastq:File, outputPrefix: String, outputDir: String) {
    val countFastq = new CountFastq(this)
    countFastq.input = fastq
    countFastq.output = outputDir + outputPrefix + ".raw.counts"
    add(countFastq)
    
    val createTagCounts = new CreateTagCounts(this)
    createTagCounts.input = countFastq.output
    createTagCounts.tagLib = tagsLibrary
    createTagCounts.countSense = outputDir + outputPrefix + ".tagcount.sense.counts"
    createTagCounts.countAllSense = outputDir + outputPrefix + ".tagcount.all.sense.counts"
    createTagCounts.countAntiSense = outputDir + outputPrefix + ".tagcount.antisense.counts"
    createTagCounts.countAllAntiSense = outputDir + outputPrefix + ".tagcount.all.antisense.counts"
    add(createTagCounts)
  }
}

object Sage extends PipelineCommand
