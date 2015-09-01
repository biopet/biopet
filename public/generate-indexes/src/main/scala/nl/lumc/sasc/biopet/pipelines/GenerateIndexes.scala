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
package nl.lumc.sasc.biopet.pipelines

import java.io.PrintWriter
import java.util

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.bowtie.{ Bowtie2Build, BowtieBuild }
import nl.lumc.sasc.biopet.extensions.bwa.BwaIndex
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.gmap.GmapBuild
import nl.lumc.sasc.biopet.extensions.picard.CreateSequenceDictionary
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFaidx
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._

class GenerateIndexes(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Argument
  var referenceConfigFile: File = _

  var referenceConfig: Map[String, Any] = Map()

  def outputConfigFile = new File(outputDir, "reference.json")

  /** This is executed before the script starts */
  def init(): Unit = {
    referenceConfig = ConfigUtils.fileToConfigMap(referenceConfigFile)
  }

  /** Method where jobs must be added */
  def biopetScript(): Unit = {

    val outputConfig = for ((speciesName, c) <- referenceConfig) yield speciesName -> {
      val speciesConfig = ConfigUtils.any2map(c)
      val speciesDir = new File(outputDir, speciesName)
      for ((genomeName, c) <- speciesConfig) yield genomeName -> {
        val genomeConfig = ConfigUtils.any2map(c)
        val fastaUris = genomeConfig.getOrElse("fasta_uri",
          throw new IllegalArgumentException(s"No fasta_uri found for $speciesName - $genomeName")) match {
            case a: Array[_] => a.map(_.toString)
            case a           => Array(a.toString)
          }

        val genomeDir = new File(speciesDir, genomeName)
        val fastaFile = new File(genomeDir, "reference.fa")
        var outputConfig: Map[String, Any] = Map("reference_fasta" -> fastaFile)

        val fastaFiles = for (fastaUri <- fastaUris) yield {
          val curl = new Curl(this)
          curl.url = fastaUri
          curl.output = if (fastaUris.length > 1 || fastaUri.endsWith(".gz")) {
            curl.isIntermediate = true
            new File(genomeDir, new File(fastaUri).getName)
          } else fastaFile

          add(curl)
          add(Md5sum(this, curl.output, genomeDir))
          curl.output
        }

        val fastaCat = new CommandLineFunction {
          var cmds: Array[BiopetCommandLineFunction] = Array()

          @Input
          var input: List[File] = Nil

          @Output
          var output = fastaFile
          def commandLine = cmds.mkString(" && ")
        }

        if (fastaUris.length > 1 || fastaFiles.filter(_.getName.endsWith(".gz")).nonEmpty) {
          fastaFiles.foreach { file =>
            if (file.getName.endsWith(".gz")) {
              val zcat = new Zcat(this)
              zcat.appending = true
              zcat.input = file
              zcat.output = fastaFile
              fastaCat.cmds :+= zcat
              fastaCat.input :+= file
            } else {
              val cat = new Cat(this)
              cat.appending = true
              cat.input :+= file
              cat.output = fastaFile
              fastaCat.cmds :+= cat
              fastaCat.input :+= file
            }
          }
          add(fastaCat)
        }

        val faidx = SamtoolsFaidx(this, fastaFile)
        add(faidx)

        val createDict = new CreateSequenceDictionary(this)
        createDict.reference = fastaFile
        createDict.output = new File(genomeDir, fastaFile.getName.stripSuffix(".fa") + ".dict")
        createDict.species = Some(speciesName)
        createDict.genomeAssembly = Some(genomeName)
        createDict.uri = Some(fastaUris.mkString(","))
        add(createDict)

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

        val annotationDir = new File(genomeDir, "annotation")

        genomeConfig.get("vep_cache_uri").foreach { vepCacheUri =>
          val vepDir = new File(annotationDir, "vep")
          val curl = new Curl(this)
          curl.url = vepCacheUri.toString
          curl.output = new File(vepDir, new File(curl.url).getName)
          curl.isIntermediate = true
          add(curl)

          val tar = new TarExtract(this)
          tar.inputTar = curl.output
          tar.outputDir = vepDir
          add(tar)

          val regex = """.*\/(.*)_vep_(\d*)_(.*)\.tar\.gz""".r
          vepCacheUri.toString match {
            case regex(species, version, assembly) if (version.forall(_.isDigit)) => {
              outputConfig ++= Map("varianteffectpredictor" -> Map(
                "species" -> species,
                "assembly" -> assembly,
                "cache_version" -> version.toInt,
                "cache" -> vepDir,
                "fasta" -> createLinks(vepDir)))
            }
            case _ => throw new IllegalArgumentException("Cache found but no version was found")
          }
        }

        genomeConfig.get("dbsnp_vcf_uri").foreach { dbsnpUri =>
          val cv = new CombineVariants(this)
          cv.reference = fastaFile
          cv.deps ::= createDict.output
          def addDownload(uri: String): Unit = {
            val curl = new Curl(this)
            curl.url = uri
            curl.output = new File(annotationDir, new File(curl.url).getName)
            curl.isIntermediate = true
            add(curl)
            cv.inputFiles ::= curl.output

            val tabix = new Tabix(this)
            tabix.input = curl.output
            tabix.p = Some("vcf")
            tabix.isIntermediate = true
            add(tabix)
            cv.deps ::= tabix.outputIndex
          }

          dbsnpUri match {
            case l: Traversable[_]    => l.foreach(x => addDownload(x.toString))
            case l: util.ArrayList[_] => l.foreach(x => addDownload(x.toString))
            case _                    => addDownload(dbsnpUri.toString)
          }

          cv.outputFile = new File(annotationDir, "dbsnp.vcf.gz")
          add(cv)
        }

        // Bwa index
        val bwaIndex = new BwaIndex(this)
        bwaIndex.reference = createLinks(new File(genomeDir, "bwa"))
        add(bwaIndex)
        outputConfig += "bwa" -> Map("reference_fasta" -> bwaIndex.reference.getAbsolutePath)

        // Gmap index
        val gmapDir = new File(genomeDir, "gmap")
        val gmapBuild = new GmapBuild(this)
        gmapBuild.dir = gmapDir
        gmapBuild.db = genomeName
        gmapBuild.fastaFiles ::= createLinks(gmapDir)
        add(gmapBuild)
        outputConfig += "gsnap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)
        outputConfig += "gmap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)

        val starDir = new File(genomeDir, "star")
        val starIndex = new Star(this)
        starIndex.outputDir = starDir
        starIndex.reference = createLinks(starDir)
        starIndex.runmode = "genomeGenerate"
        add(starIndex)
        outputConfig += "star" -> Map(
          "reference_fasta" -> starIndex.reference.getAbsolutePath,
          "genomeDir" -> starDir.getAbsolutePath
        )

        val bowtieIndex = new BowtieBuild(this)
        bowtieIndex.reference = createLinks(new File(genomeDir, "bowtie"))
        bowtieIndex.baseName = "reference"
        add(bowtieIndex)
        outputConfig += "bowtie" -> Map("reference_fasta" -> bowtieIndex.reference.getAbsolutePath)

        val bowtie2Index = new Bowtie2Build(this)
        bowtie2Index.reference = createLinks(new File(genomeDir, "bowtie2"))
        bowtie2Index.baseName = "reference"
        add(bowtie2Index)
        outputConfig += "bowtie2" -> Map("reference_fasta" -> bowtie2Index.reference.getAbsolutePath)
        outputConfig += "tophat" -> Map(
          "bowtie_index" -> bowtie2Index.reference.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta")
        )

        outputConfig
      }
    }

    //TODO: make this a [InprocessFunction]
    val writer = new PrintWriter(outputConfigFile)
    writer.println(ConfigUtils.mapToJson(Map("references" -> outputConfig)).spaces2)
    writer.close()
  }
}

object GenerateIndexes extends PipelineCommand
