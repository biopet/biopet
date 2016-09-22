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

import java.util

import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.{ Cat, Curl, Zcat }
import nl.lumc.sasc.biopet.extensions.tools.DownloadNcbiAssembly
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls

class DownloadGenomes(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Argument(required = true)
  var referenceConfigFiles: List[File] = Nil

  var referenceConfig: Map[String, Any] = null

  override def fixedValues = Map("gffread" -> Map("T" -> true))

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

        val genomeDir = new File(speciesDir, genomeName)
        val fastaFile = new File(genomeDir, "reference.fa")

        genomeConfig.get("ncbi_assembly_id") match {
          case Some(assemblyID: String) => {
            val downloadAssembly = new DownloadNcbiAssembly(this)
            downloadAssembly.assemblyId = assemblyID
            downloadAssembly.output = fastaFile
            downloadAssembly.outputReport = new File(genomeDir, s"$speciesName-$genomeName.assamble.report")
            downloadAssembly.nameHeader = referenceConfig.get("ncbi_assembly_header_name").map(_.toString)
            downloadAssembly.mustHaveOne = referenceConfig.get("ncbi_assembly_must_have_one")
              .map(_.asInstanceOf[Map[String, String]])
              .getOrElse(Map())
            downloadAssembly.mustNotHave = referenceConfig.get("ncbi_assembly_must_not_have")
              .map(_.asInstanceOf[Map[String, String]])
              .getOrElse(Map())
            add(downloadAssembly)
          }
          case _ => {
            val fastaUris = genomeConfig.getOrElse("fasta_uri",
              throw new IllegalArgumentException(s"No fasta_uri found for $speciesName - $genomeName")) match {
                case a: Traversable[_]    => a.map(_.toString).toArray
                case a: util.ArrayList[_] => a.map(_.toString).toArray
                case a                    => Array(a.toString)
              }

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
          }
        }

        val generateIndexes = new GenerateIndexes(this)
        generateIndexes.fastaFile = fastaFile
        generateIndexes.speciesName = speciesName
        generateIndexes.genomeName = genomeName
        //generateIndexes.fastaUris = fastaUris
        //TODO: add gtf file
        add(generateIndexes)

        //        val annotationDir = new File(genomeDir, "annotation")
        //
        //        genomeConfig.get("vep_cache_uri").foreach { vepCacheUri =>
        //          val vepDir = new File(annotationDir, "vep")
        //          val curl = new Curl(this)
        //          curl.url = vepCacheUri.toString
        //          curl.output = new File(vepDir, new File(curl.url).getName)
        //          curl.isIntermediate = true
        //          add(curl)
        //
        //          val tar = new TarExtract(this)
        //          tar.inputTar = curl.output
        //          tar.outputDir = vepDir
        //          add(tar)
        //
        //          val regex = """.*\/(.*)_vep_(\d*)_(.*)\.tar\.gz""".r
        //          vepCacheUri.toString match {
        //            case regex(species, version, assembly) if version.forall(_.isDigit) =>
        //              outputConfig ++= Map("varianteffectpredictor" -> Map(
        //                "species" -> species,
        //                "assembly" -> assembly,
        //                "cache_version" -> version.toInt,
        //                "cache" -> vepDir,
        //                "fasta" -> createLinks(vepDir)))
        //            case _ => throw new IllegalArgumentException("Cache found but no version was found")
        //          }
        //        }
        //
        //        genomeConfig.get("dbsnp_vcf_uri").foreach { dbsnpUri =>
        //          val contigMap = genomeConfig.get("dbsnp_contig_map").map(_.asInstanceOf[Map[String, Any]])
        //          val contigSed = contigMap.map { map =>
        //            val sed = new Sed(this)
        //            sed.expressions = map.map(x => s"""s/^${x._1}\t/${x._2}\t/""").toList
        //            sed
        //          }
        //
        //          val cv = new CombineVariants(this)
        //          cv.reference_sequence = fastaFile
        //          def addDownload(uri: String): Unit = {
        //            val isZipped = uri.endsWith(".gz")
        //            val output = new File(annotationDir, new File(uri).getName + (if (isZipped) "" else ".gz"))
        //            val curl = new Curl(this)
        //            curl.url = uri
        //
        //            val downloadCmd = (isZipped, contigSed) match {
        //              case (true, Some(sed)) => curl | Zcat(this) | sed | new Bgzip(this) > output
        //              case (false, Some(sed)) => curl | sed | new Bgzip(this) > output
        //              case (true, None) => curl > output
        //              case (false, None) => curl | new Bgzip(this) > output
        //            }
        //            downloadCmd.isIntermediate = true
        //            add(downloadCmd)
        //
        //            val tabix = new Tabix(this)
        //            tabix.input = output
        //            tabix.p = Some("vcf")
        //            tabix.isIntermediate = true
        //            add(tabix)
        //
        //            cv.variant :+= output
        //          }
        //
        //          dbsnpUri match {
        //            case l: Traversable[_]    => l.foreach(x => addDownload(x.toString))
        //            case l: util.ArrayList[_] => l.foreach(x => addDownload(x.toString))
        //            case _                    => addDownload(dbsnpUri.toString)
        //          }
        //
        //          cv.out = new File(annotationDir, "dbsnp.vcf.gz")
        //          add(cv)
        //          outputConfig += "dbsnp" -> cv.out
        //        }
        //
        //        val gffFile: Option[File] = genomeConfig.get("gff_uri").map { gtfUri =>
        //          val outputFile = new File(annotationDir, new File(gtfUri.toString).getName.stripSuffix(".gz"))
        //          val curl = new Curl(this)
        //          curl.url = gtfUri.toString
        //          if (gtfUri.toString.endsWith(".gz")) add(curl | Zcat(this) > outputFile)
        //          else add(curl > outputFile)
        //          outputConfig += "annotation_gff" -> outputFile
        //          outputFile
        //        }
        //
        //        val gtfFile: Option[File] = if (gffFile.isDefined) gffFile.map { gff =>
        //          val gffRead = new GffRead(this)
        //          gffRead.input = gff
        //          gffRead.output = swapExt(annotationDir, gff, ".gff", ".gtf")
        //          add(gffRead)
        //          gffRead.output
        //        } else genomeConfig.get("gtf_uri").map { gtfUri =>
        //          val outputFile = new File(annotationDir, new File(gtfUri.toString).getName.stripSuffix(".gz"))
        //          val curl = new Curl(this)
        //          curl.url = gtfUri.toString
        //          if (gtfUri.toString.endsWith(".gz")) add(curl | Zcat(this) > outputFile)
        //          else add(curl > outputFile)
        //          outputConfig += "annotation_gtf" -> outputFile
        //          outputFile
        //        }
        //
        //        val refFlatFile: Option[File] = gtfFile.map { gtf =>
        //          val refFlat = new File(gtf + ".refFlat")
        //          val gtfToGenePred = new GtfToGenePred(this)
        //          gtfToGenePred.inputGtfs :+= gtf
        //
        //          add(gtfToGenePred | Awk(this, """{ print $12"\t"$1"\t"$2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7"\t"$8"\t"$9"\t"$10 }""") > refFlat)
        //
        //          outputConfig += "annotation_refflat" -> refFlat
        //          refFlat
        //        }
      }
    }
  }
}

object DownloadGenomes extends PipelineCommand
