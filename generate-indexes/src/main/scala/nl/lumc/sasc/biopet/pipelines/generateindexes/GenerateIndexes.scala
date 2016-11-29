package nl.lumc.sasc.biopet.pipelines.generateindexes

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.bowtie.{ Bowtie2Build, BowtieBuild }
import nl.lumc.sasc.biopet.extensions.{ Ln, Star }
import nl.lumc.sasc.biopet.extensions.bwa.BwaIndex
import nl.lumc.sasc.biopet.extensions.gmap.GmapBuild
import nl.lumc.sasc.biopet.extensions.hisat.Hisat2Build
import nl.lumc.sasc.biopet.extensions.picard.CreateSequenceDictionary
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFaidx
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable.ListBuffer

/**
 * Created by pjvan_thof on 21-9-16.
 */
class GenerateIndexes(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input fasta file", shortName = "R")
  var fastaFile: File = _

  @Argument(required = true)
  var speciesName: String = _

  @Argument(required = true)
  var genomeName: String = _

  @Input(required = false)
  var gtfFile: Option[File] = None

  var fastaUris: Array[String] = Array()

  /** Init for pipeline */
  def init(): Unit = {
    if (outputDir == null) outputDir = fastaFile.getParentFile
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {

    var outputConfig: Map[String, Any] = Map("reference_fasta" -> fastaFile)
    val configDeps = new ListBuffer[File]()

    val faidx = SamtoolsFaidx(this, fastaFile)
    add(faidx)
    configDeps += faidx.output

    val createDict = new CreateSequenceDictionary(this)
    createDict.reference = fastaFile
    createDict.output = new File(outputDir, fastaFile.getName.stripSuffix(".fa") + ".dict")
    createDict.species = Some(speciesName)
    createDict.genomeAssembly = Some(genomeName)
    if (fastaUris.nonEmpty) createDict.uri = Some(fastaUris.mkString(","))
    add(createDict)
    configDeps += createDict.output

    def createLinks(dir: File): File = {
      val newFastaFile = new File(dir, fastaFile.getName)
      val newFai = new File(dir, faidx.output.getName)
      val newDict = new File(dir, createDict.output.getName)

      add(Ln(this, faidx.output, newFai))
      add(Ln(this, createDict.output, newDict))
      val lnFasta = Ln(this, fastaFile, newFastaFile)
      lnFasta.deps ++= List(newFai, newDict)
      add(lnFasta)
      newFastaFile
    }

    // Bwa index
    val bwaIndex = new BwaIndex(this)
    bwaIndex.reference = createLinks(new File(outputDir, "bwa"))
    add(bwaIndex)
    configDeps += bwaIndex.jobOutputFile
    outputConfig += "bwa" -> Map("reference_fasta" -> bwaIndex.reference.getAbsolutePath)

    // Gmap index
    val gmapDir = new File(outputDir, "gmap")
    val gmapBuild = new GmapBuild(this)
    gmapBuild.dir = gmapDir
    gmapBuild.db = genomeName
    gmapBuild.fastaFiles ::= createLinks(gmapDir)
    add(gmapBuild)
    configDeps += gmapBuild.jobOutputFile
    outputConfig += "gsnap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)
    outputConfig += "gmap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)

    // STAR index
    val starDir = new File(outputDir, "star")
    val starIndex = new Star(this)
    starIndex.outputDir = starDir
    starIndex.reference = createLinks(starDir)
    starIndex.runmode = "genomeGenerate"
    starIndex.sjdbGTFfile = gtfFile
    add(starIndex)
    configDeps += starIndex.jobOutputFile
    outputConfig += "star" -> Map(
      "reference_fasta" -> starIndex.reference.getAbsolutePath,
      "genomeDir" -> starDir.getAbsolutePath
    )

    // Bowtie index
    val bowtieIndex = new BowtieBuild(this)
    bowtieIndex.reference = createLinks(new File(outputDir, "bowtie"))
    bowtieIndex.baseName = "reference"
    add(bowtieIndex)
    configDeps += bowtieIndex.jobOutputFile
    outputConfig += "bowtie" -> Map(
      "bowtie_index" -> bowtieIndex.reference.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta"),
      "reference_fasta" -> bowtieIndex.reference.getAbsolutePath
    )

    // Bowtie2 index
    val bowtie2Index = new Bowtie2Build(this)
    bowtie2Index.reference = createLinks(new File(outputDir, "bowtie2"))
    bowtie2Index.baseName = "reference"
    add(bowtie2Index)
    configDeps += bowtie2Index.jobOutputFile
    outputConfig += "bowtie2" -> Map(
      "bowtie_index" -> bowtie2Index.reference.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta"),
      "reference_fasta" -> bowtie2Index.reference.getAbsolutePath
    )
    outputConfig += "tophat" -> Map(
      "bowtie_index" -> bowtie2Index.reference.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta")
    )

    // Hisat2 index
    val hisat2Index = new Hisat2Build(this)
    hisat2Index.inputFasta = createLinks(new File(outputDir, "hisat2"))
    hisat2Index.hisat2IndexBase = new File(new File(outputDir, "hisat2"), "reference").getAbsolutePath
    add(hisat2Index)
    configDeps += hisat2Index.jobOutputFile
    outputConfig += "hisat2" -> Map(
      "reference_fasta" -> hisat2Index.inputFasta.getAbsolutePath,
      "hisat_index" -> hisat2Index.inputFasta.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta")
    )

    val writeConfig = new WriteConfig
    writeConfig.deps = configDeps.toList
    writeConfig.out = configFile
    writeConfig.config = Map("references" -> Map(speciesName -> Map(genomeName -> outputConfig)))
    add(writeConfig)

  }

  def configFile = new File(outputDir, s"$speciesName-$genomeName.json")
}

object GenerateIndexes extends PipelineCommand