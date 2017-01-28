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

import java.io.File
import java.util

import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.picard.NormalizeFasta
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

  var referenceConfig: Map[String, Any] = _

  override def fixedValues = Map("gffread" -> Map("T" -> true))

  override def defaults = Map("normalizefasta" -> Map("line_length" -> 60))

  val downloadAnnotations: Boolean = config("download_annotations", default = false)

  /** This is executed before the script starts */
  def init(): Unit = {
    if (referenceConfig == null)
      referenceConfig = referenceConfigFiles.foldLeft(Map[String, Any]())((a, b) => ConfigUtils.mergeMaps(a, ConfigUtils.fileToConfigMap(b)))
  }

  /** Method where jobs must be added */
  def biopetScript(): Unit = {

    for ((speciesName, c) <- referenceConfig) yield speciesName -> {
      val speciesConfig = ConfigUtils.any2map(c)
      val speciesDir = new File(outputDir, speciesName)
      for ((genomeName, c) <- speciesConfig) yield genomeName -> {
        var configDeps: List[File] = Nil
        val genomeConfig = ConfigUtils.any2map(c)

        val genomeDir = new File(speciesDir, genomeName)
        val fastaFile = new File(genomeDir, "reference.fa")
        val downloadFastaFile = new File(genomeDir, "download.reference.fa")

        genomeConfig.get("ncbi_assembly_report") match {
          case Some(assemblyID: String) =>
            val downloadAssembly = new DownloadNcbiAssembly(this)
            downloadAssembly.assemblyId = assemblyID
            downloadAssembly.output = downloadFastaFile
            downloadAssembly.outputReport = new File(genomeDir, s"$speciesName-$genomeName.assembly.report")
            downloadAssembly.nameHeader = genomeConfig.get("ncbi_assembly_header_name").map(_.toString)
            downloadAssembly.mustHaveOne = genomeConfig.get("ncbi_assembly_must_have_one")
              .map(_.asInstanceOf[util.ArrayList[util.LinkedHashMap[String, String]]])
              .getOrElse(new util.ArrayList()).flatMap(x => x.map(y => y._1 + "=" + y._2))
              .toList
            downloadAssembly.mustNotHave = genomeConfig.get("ncbi_assembly_must_not_have")
              .map(_.asInstanceOf[util.ArrayList[util.LinkedHashMap[String, String]]])
              .getOrElse(new util.ArrayList()).flatMap(x => x.map(y => y._1 + "=" + y._2))
              .toList
            downloadAssembly.isIntermediate = true
            add(downloadAssembly)
          case _ =>
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
            fastaCat.output = downloadFastaFile
            fastaCat.isIntermediate = true
            if (fastaUris.length > 1 || fastaFiles.exists(_.getName.endsWith(".gz"))) {
              fastaFiles.foreach { file =>
                if (file.getName.endsWith(".gz")) {
                  val zcat = new Zcat(this)
                  zcat.appending = true
                  zcat.input :+= file
                  zcat.output = downloadFastaFile
                  fastaCat.cmds :+= zcat
                  fastaCat.input :+= file
                } else {
                  val cat = new Cat(this)
                  cat.appending = true
                  cat.input :+= file
                  cat.output = downloadFastaFile
                  fastaCat.cmds :+= cat
                  fastaCat.input :+= file
                }
              }
              add(fastaCat)
              configDeps :+= fastaCat.output
            }
        }

        val normalizeFasta = new NormalizeFasta(this)
        normalizeFasta.input = downloadFastaFile
        normalizeFasta.output = fastaFile
        add(normalizeFasta)

        val generateIndexes = new GenerateIndexes(this)
        generateIndexes.fastaFile = fastaFile
        generateIndexes.speciesName = speciesName
        generateIndexes.genomeName = genomeName
        generateIndexes.outputDir = genomeDir
        //generateIndexes.fastaUris = fastaUris
        //TODO: add gtf file
        add(generateIndexes)

        if (downloadAnnotations) {
          val annotationDir = new File(genomeDir, "annotation")

          def getAnnotation(tag: String): Map[String, Map[String, Any]] = (genomeConfig.get(tag) match {
            case Some(s: Map[_, _]) => s.map(x => x._2 match {
              case o: Map[_, _] => x._1.toString -> o.map(x => (x._1.toString, x._2))
              case _            => throw new IllegalStateException(s"values in the tag $tag should be json objects")
            })
            case None => Map()
            case x    => throw new IllegalStateException(s"tag $tag should be an object with objects, now $x")
          })

          // Download vep caches
          getAnnotation("vep").foreach {
            case (version, vep) =>
              val vepDir = new File(annotationDir, "vep" + File.separator + version)
              val curl = new Curl(this)
              curl.url = vep("cache_uri").toString
              curl.output = new File(vepDir, new File(curl.url).getName)
              curl.isIntermediate = true
              add(curl)

              val tar = new TarExtract(this)
              tar.inputTar = curl.output
              tar.outputDir = vepDir
              add(tar)
          }

          getAnnotation("dbsnp").foreach {
            case (version, dbsnp) =>
              val dbpsnpDir = new File(annotationDir, "dbsnp")
              val contigMap = dbsnp.get("dbsnp_contig_map").map(_.asInstanceOf[Map[String, Any]])
              val contigSed = contigMap.map { map =>
                val sed = new Sed(this)
                sed.expressions = map.map(x => s"""s/^${x._1}\t/${x._2}\t/""").toList
                sed
              }

              val cv = new CombineVariants(this)
              cv.reference_sequence = fastaFile
              def addDownload(uri: String): Unit = {
                val isZipped = uri.endsWith(".gz")
                val output = new File(dbpsnpDir, version + "." + new File(uri).getName + (if (isZipped) "" else ".gz"))
                val curl = new Curl(this)
                curl.url = uri

                val downloadCmd = (isZipped, contigSed) match {
                  case (true, Some(sed))  => curl | Zcat(this) | sed | new Bgzip(this) > output
                  case (false, Some(sed)) => curl | sed | new Bgzip(this) > output
                  case (true, None)       => curl > output
                  case (false, None)      => curl | new Bgzip(this) > output
                }
                downloadCmd.isIntermediate = true
                add(downloadCmd)

                val tabix = new Tabix(this)
                tabix.input = output
                tabix.p = Some("vcf")
                tabix.isIntermediate = true
                add(tabix)

                cv.variant :+= output
              }

              dbsnp.get("vcf_uri") match {
                case Some(l: Traversable[_])    => l.foreach(x => addDownload(x.toString))
                case Some(l: util.ArrayList[_]) => l.foreach(x => addDownload(x.toString))
                case Some(s)                    => addDownload(s.toString)
                case None                       => throw new IllegalStateException("Dbsnp should always have a 'vcf_uri' key")
              }

              cv.out = new File(dbpsnpDir, s"dbsnp.$version.vcf.gz")
              add(cv)
          }

          getAnnotation("gene_annotation").foreach {
            case (version, geneAnnotation) =>
              val dir = new File(annotationDir, version)
              val gffFile: Option[File] = geneAnnotation.get("gff_uri").map { gtfUri =>
                val outputFile = new File(dir, new File(gtfUri.toString).getName.stripSuffix(".gz"))
                val curl = new Curl(this)
                curl.url = gtfUri.toString
                if (gtfUri.toString.endsWith(".gz")) add(curl | Zcat(this) > outputFile)
                else add(curl > outputFile)
                outputFile
              }

              val gtfFile: Option[File] = geneAnnotation.get("gtf_uri") match {
                case Some(gtfUri) =>
                  val outputFile = new File(dir, new File(gtfUri.toString).getName.stripSuffix(".gz"))
                  val curl = new Curl(this)
                  curl.url = gtfUri.toString
                  if (gtfUri.toString.endsWith(".gz")) add(curl | Zcat(this) > outputFile)
                  else add(curl > outputFile)
                  Some(outputFile)
                case _ => gffFile.map { gff =>
                  val gffRead = new GffRead(this)
                  gffRead.input = gff
                  gffRead.output = swapExt(dir, gff, ".gff", ".gtf")
                  add(gffRead)
                  gffRead.output
                }
              }

              val refFlatFile: Option[File] = gtfFile.map { gtf =>
                val refFlat = new File(gtf + ".refFlat")
                val gtfToGenePred = new GtfToGenePred(this)
                gtfToGenePred.inputGtfs :+= gtf

                add(gtfToGenePred | Awk(this, """{ print $12"\t"$1"\t"$2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7"\t"$8"\t"$9"\t"$10 }""") > refFlat)

                refFlat
              }
          }
        }
      }
    }
  }
}

object DownloadGenomes extends PipelineCommand
