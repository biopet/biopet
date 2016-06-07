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
package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.{ File, PrintWriter }
import java.util

import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.bowtie.{ Bowtie2Build, BowtieBuild }
import nl.lumc.sasc.biopet.extensions.bwa.BwaIndex
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.gmap.GmapBuild
import nl.lumc.sasc.biopet.extensions.hisat.Hisat2Build
import nl.lumc.sasc.biopet.extensions.picard.CreateSequenceDictionary
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFaidx
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls

class GenerateIndexes(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Argument(required = true)
  var referenceConfigFiles: List[File] = Nil

  var referenceConfig: Map[String, Any] = null

  protected var configDeps: List[File] = Nil

  /** This is executed before the script starts */
  def init(): Unit = {
    if (referenceConfig == null)
      referenceConfig = referenceConfigFiles.foldLeft(Map[String, Any]())((a, b) => ConfigUtils.mergeMaps(a, ConfigUtils.fileToConfigMap(b)))
  }

  /** Method where jobs must be added */
  def biopetScript(): Unit = {

    val outputConfig = for ((speciesName, c) <- referenceConfig) yield speciesName -> {
      val speciesConfig = ConfigUtils.any2map(c)
      val speciesDir = new File(outputDir, speciesName)
      for ((genomeName, c) <- speciesConfig) yield genomeName -> {
        var configDeps: List[File] = Nil
        val genomeConfig = ConfigUtils.any2map(c)
        val fastaUris = genomeConfig.getOrElse("fasta_uri",
          throw new IllegalArgumentException(s"No fasta_uri found for $speciesName - $genomeName")) match {
            case a: Traversable[_]    => a.map(_.toString).toArray
            case a: util.ArrayList[_] => a.map(_.toString).toArray
            case a                    => Array(a.toString)
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
          configDeps :+= curl.output
          curl.output
        }

        val fastaCat = new FastaMerging(this)
        fastaCat.output = fastaFile

        if (fastaUris.length > 1 || fastaFiles.exists(_.getName.endsWith(".gz"))) {
          fastaFiles.foreach { file =>
            if (file.getName.endsWith(".gz")) {
              val zcat = new Zcat(this)
              zcat.appending = true
              zcat.input :+= file
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
          configDeps :+= fastaCat.output
        }

        val faidx = SamtoolsFaidx(this, fastaFile)
        add(faidx)
        configDeps :+= faidx.output

        val createDict = new CreateSequenceDictionary(this)
        createDict.reference = fastaFile
        createDict.output = new File(genomeDir, fastaFile.getName.stripSuffix(".fa") + ".dict")
        createDict.species = Some(speciesName)
        createDict.genomeAssembly = Some(genomeName)
        createDict.uri = Some(fastaUris.mkString(","))
        add(createDict)
        configDeps :+= createDict.output

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
            case regex(species, version, assembly) if version.forall(_.isDigit) =>
              outputConfig ++= Map("varianteffectpredictor" -> Map(
                "species" -> species,
                "assembly" -> assembly,
                "cache_version" -> version.toInt,
                "cache" -> vepDir,
                "fasta" -> createLinks(vepDir)))
            case _ => throw new IllegalArgumentException("Cache found but no version was found")
          }
        }

        genomeConfig.get("dbsnp_vcf_uri").foreach { dbsnpUri =>
          val cv = new CombineVariants(this)
          cv.reference_sequence = fastaFile
          cv.deps ::= createDict.output
          def addDownload(uri: String): Unit = {
            val curl = new Curl(this)
            curl.url = uri
            curl.output = new File(annotationDir, new File(curl.url).getName)
            curl.isIntermediate = true
            add(curl)
            cv.variant :+= curl.output

            if (curl.output.getName.endsWith(".vcf.gz")) {
              val tabix = new Tabix(this)
              tabix.input = curl.output
              tabix.p = Some("vcf")
              tabix.isIntermediate = true
              add(tabix)
              configDeps :+= tabix.outputIndex
            }
          }

          dbsnpUri match {
            case l: Traversable[_]    => l.foreach(x => addDownload(x.toString))
            case l: util.ArrayList[_] => l.foreach(x => addDownload(x.toString))
            case _                    => addDownload(dbsnpUri.toString)
          }

          cv.out = new File(annotationDir, "dbsnp.vcf.gz")
          add(cv)
          outputConfig += "dbsnp" -> cv.out
        }

        val gtfFile: Option[File] = genomeConfig.get("gtf_uri").map { gtfUri =>
          val outputFile = new File(annotationDir, new File(gtfUri.toString).getName.stripSuffix(".gz"))
          val curl = new Curl(this)
          curl.url = gtfUri.toString
          if (gtfUri.toString.endsWith(".gz")) add(curl | Zcat(this) > outputFile)
          else add(curl > outputFile)
          outputConfig += "annotation_gtf" -> outputFile
          outputFile
        }

        val refFlatFile: Option[File] = gtfFile.map { gtf =>
          val refFlat = new File(gtf + ".refFlat")
          val gtfToGenePred = new GtfToGenePred(this)
          gtfToGenePred.inputGtfs :+= gtf

          add(gtfToGenePred | Awk(this, """{ print $12"\t"$1"\t"$2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7"\t"$8"\t"$9"\t"$10 }""") > refFlat)

          outputConfig += "annotation_refflat" -> refFlat
          refFlat
        }

        // Bwa index
        val bwaIndex = new BwaIndex(this)
        bwaIndex.reference = createLinks(new File(genomeDir, "bwa"))
        add(bwaIndex)
        configDeps :+= bwaIndex.jobOutputFile
        outputConfig += "bwa" -> Map("reference_fasta" -> bwaIndex.reference.getAbsolutePath)

        // Gmap index
        val gmapDir = new File(genomeDir, "gmap")
        val gmapBuild = new GmapBuild(this)
        gmapBuild.dir = gmapDir
        gmapBuild.db = genomeName
        gmapBuild.fastaFiles ::= createLinks(gmapDir)
        add(gmapBuild)
        configDeps :+= gmapBuild.jobOutputFile
        outputConfig += "gsnap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)
        outputConfig += "gmap" -> Map("dir" -> gmapBuild.dir.getAbsolutePath, "db" -> genomeName)

        // STAR index
        val starDir = new File(genomeDir, "star")
        val starIndex = new Star(this)
        starIndex.outputDir = starDir
        starIndex.reference = createLinks(starDir)
        starIndex.runmode = "genomeGenerate"
        starIndex.sjdbGTFfile = gtfFile
        add(starIndex)
        configDeps :+= starIndex.jobOutputFile
        outputConfig += "star" -> Map(
          "reference_fasta" -> starIndex.reference.getAbsolutePath,
          "genomeDir" -> starDir.getAbsolutePath
        )

        // Bowtie index
        val bowtieIndex = new BowtieBuild(this)
        bowtieIndex.reference = createLinks(new File(genomeDir, "bowtie"))
        bowtieIndex.baseName = "reference"
        add(bowtieIndex)
        configDeps :+= bowtieIndex.jobOutputFile
        outputConfig += "bowtie" -> Map("reference_fasta" -> bowtieIndex.reference.getAbsolutePath)

        // Bowtie2 index
        val bowtie2Index = new Bowtie2Build(this)
        bowtie2Index.reference = createLinks(new File(genomeDir, "bowtie2"))
        bowtie2Index.baseName = "reference"
        add(bowtie2Index)
        configDeps :+= bowtie2Index.jobOutputFile
        outputConfig += "bowtie2" -> Map("reference_fasta" -> bowtie2Index.reference.getAbsolutePath)
        outputConfig += "tophat" -> Map(
          "bowtie_index" -> bowtie2Index.reference.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta")
        )

        // Hisat2 index
        val hisat2Index = new Hisat2Build(this)
        hisat2Index.inputFasta = createLinks(new File(genomeDir, "hisat2"))
        hisat2Index.hisat2IndexBase = new File(new File(genomeDir, "hisat2"), "reference").getAbsolutePath
        add(hisat2Index)
        configDeps :+= hisat2Index.jobOutputFile
        outputConfig += "hisat2" -> Map(
          "reference_fasta" -> hisat2Index.inputFasta.getAbsolutePath,
          "hisat_index" -> hisat2Index.inputFasta.getAbsolutePath.stripSuffix(".fa").stripSuffix(".fasta")
        )

        val writeConfig = new WriteConfig
        writeConfig.deps = configDeps
        writeConfig.out = new File(genomeDir, s"$speciesName-$genomeName.json")
        writeConfig.config = Map("references" -> Map(speciesName -> Map(genomeName -> outputConfig)))
        add(writeConfig)

        this.configDeps :::= configDeps
        outputConfig
      }
    }

    val writeConfig = new WriteConfig
    writeConfig.deps = configDeps
    writeConfig.out = new File(outputDir, "references.json")
    writeConfig.config = Map("references" -> outputConfig)
    add(writeConfig)
  }
}

object GenerateIndexes extends PipelineCommand
