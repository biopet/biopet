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
package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.{File, FileOutputStream, IOException, PrintWriter}

import nl.lumc.sasc.biopet.tools.vcfstats.Stats.plotHeatmap
import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats.logger
import nl.lumc.sasc.biopet.utils.{ConfigUtils, sortAnyAny}

import scala.collection.mutable
import scala.sys.process.{Process, ProcessLogger}

/**
  * General stats class to store vcf stats
  *
  * @param generalStats Stores are general stats
  * @param samplesStats Stores all sample/genotype specific stats
  */
case class Stats(generalStats: mutable.Map[String, mutable.Map[Any, Int]] = mutable.Map(),
                 samplesStats: mutable.Map[String, SampleStats] = mutable.Map()) {

  /** Add an other class */
  def +=(other: Stats): Stats = {
    for ((key, value) <- other.samplesStats) {
      if (this.samplesStats.contains(key)) this.samplesStats(key) += value
      else this.samplesStats(key) = value
    }
    for ((field, fieldMap) <- other.generalStats) {
      val thisField = this.generalStats.get(field)
      if (thisField.isDefined) Stats.mergeStatsMap(thisField.get, fieldMap)
      else this.generalStats += field -> fieldMap
    }
    this
  }

  /** Function to write 1 specific general field */
  def writeField(field: String, outputDir: File, prefix: String = ""): File = {
    val file = prefix match {
      case "" => new File(outputDir, field + ".tsv")
      case _ => new File(outputDir, prefix + "-" + field + ".tsv")
    }

    val data = this.generalStats
      .getOrElse(field, mutable.Map[Any, Int]())

    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    for (key <- data.keySet.toList.sortWith(sortAnyAny)) {
      writer.println(key + "\t" + data(key))
    }
    writer.close()
    file
  }

  /** Function to write 1 specific general field */
  def getField(field: String): Map[String, Array[Any]] = {

    val data = this.generalStats
      .getOrElse(field, mutable.Map[Any, Int]())
    val rows = for (key <- data.keySet.toArray.sortWith(sortAnyAny)) yield {
      (key, data(key))
    }
    Map("value" -> rows.map(_._1), "count" -> rows.map(_._2))
  }

  /** Function to write 1 specific genotype field */
  def writeGenotypeField(samples: List[String],
                         field: String,
                         outputDir: File,
                         prefix: String = ""): Unit = {
    val file = prefix match {
      case "" => new File(outputDir, field + ".tsv")
      case _ => new File(outputDir, prefix + "-" + field + ".tsv")
    }

    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println(samples.mkString(field + "\t", "\t", ""))
    val keySet = (for (sample <- samples)
      yield
        this
          .samplesStats(sample)
          .genotypeStats
          .getOrElse(field, Map[Any, Int]())
          .keySet).fold(Set[Any]())(_ ++ _)
    for (key <- keySet.toList.sortWith(sortAnyAny)) {
      val values = for (sample <- samples)
        yield
          this
            .samplesStats(sample)
            .genotypeStats
            .getOrElse(field, Map[Any, Int]())
            .getOrElse(key, 0)
      writer.println(values.mkString(key + "\t", "\t", ""))
    }
    writer.close()
  }

  /** Function to write 1 specific genotype field */
  def getGenotypeField(samples: List[String], field: String): Map[String, Map[String, Any]] = {
    val keySet = (for (sample <- samples)
      yield
        this
          .samplesStats(sample)
          .genotypeStats
          .getOrElse(field, Map[Any, Int]())
          .keySet).fold(Set[Any]())(_ ++ _)

    (for (sample <- samples)
      yield
        sample -> {
          keySet
            .map(
              key =>
                key.toString -> this
                  .samplesStats(sample)
                  .genotypeStats
                  .getOrElse(field, Map[Any, Int]())
                  .get(key))
            .filter(_._2.isDefined)
            .toMap
        }).toMap
  }

  /** This will generate stats for one contig */
  def getStatsAsMap(samples: List[String],
                    genotypeFields: List[String] = Nil,
                    infoFields: List[String] = Nil,
                    sampleDistributions: List[String] = Nil): Map[String, Any] = {
    Map(
      "genotype" -> genotypeFields.map(f => f -> getGenotypeField(samples, f)).toMap,
      "info" -> infoFields.map(f => f -> getField(f)).toMap,
      "sample_distributions" -> sampleDistributions
        .map(f => f -> getField("SampleDistribution-" + f))
        .toMap
    ) ++ Map(
      "sample_compare" -> Map(
        "samples" -> samples,
        "genotype_overlap" -> samples.map(sample1 =>
          samples.map(sample2 => samplesStats(sample1).sampleToSample(sample2).genotypeOverlap)),
        "allele_overlap" -> samples.map(sample1 =>
          samples.map(sample2 => samplesStats(sample1).sampleToSample(sample2).alleleOverlap))
      )
    )
  }

  def writeAllOutput(outputDir: File,
                     samples: List[String],
                     genotypeFields: List[String],
                     infoFields: List[String],
                     sampleDistributions: List[String],
                     contig: Option[String]): Unit = {
    outputDir.mkdirs()
    this.writeToFile(new File(outputDir, "stats.json"),
                     samples,
                     genotypeFields,
                     infoFields,
                     sampleDistributions,
                     contig)
    writeOverlap(outputDir, samples)
  }

  def writeToFile(outputFile: File,
                  samples: List[String],
                  genotypeFields: List[String],
                  infoFields: List[String],
                  sampleDistributions: List[String],
                  contig: Option[String]): Unit = {
    val allWriter = new PrintWriter(outputFile)
    val map = this.getStatsAsMap(samples, genotypeFields, infoFields, sampleDistributions)
    val json = contig match {
      case Some(c) => ConfigUtils.mapToJson(Map("contigs" -> Map(c -> map)))
      case _ => ConfigUtils.mapToJson(Map("total" -> map))
    }
    allWriter.println(json.nospaces)
    allWriter.close()
  }

  def writeOverlap(outputDir: File, samples: List[String]): Unit = {
    this.writeOverlap(_.genotypeOverlap, outputDir + "/sample_compare/genotype_overlap", samples)
    this.writeOverlap(_.alleleOverlap, outputDir + "/sample_compare/allele_overlap", samples)
  }

  /** Function to write sample to sample compare tsv's / heatmaps */
  private def writeOverlap(function: SampleToSampleStats => Int,
                           prefix: String,
                           samples: List[String],
                           plots: Boolean = true): Unit = {
    val absFile = new File(prefix + ".abs.tsv")
    val relFile = new File(prefix + ".rel.tsv")

    absFile.getParentFile.mkdirs()

    val absWriter = new PrintWriter(absFile)
    val relWriter = new PrintWriter(relFile)

    absWriter.println(samples.mkString("\t", "\t", ""))
    relWriter.println(samples.mkString("\t", "\t", ""))
    for (sample1 <- samples) {
      val values = for (sample2 <- samples)
        yield function(this.samplesStats(sample1).sampleToSample(sample2))

      absWriter.println(values.mkString(sample1 + "\t", "\t", ""))

      val total = function(this.samplesStats(sample1).sampleToSample(sample1))
      relWriter.println(values.map(_.toFloat / total).mkString(sample1 + "\t", "\t", ""))
    }
    absWriter.close()
    relWriter.close()

    if (plots) plotHeatmap(relFile)
  }

}

object Stats {

  def emptyStats(samples: List[String]): Stats = {
    val stats = new Stats
    //init stats
    for (sample1 <- samples) {
      stats.samplesStats += sample1 -> new SampleStats
      for (sample2 <- samples) {
        stats.samplesStats(sample1).sampleToSample += sample2 -> new SampleToSampleStats
      }
    }
    stats
  }

  /** Merge m2 into m1 */
  def mergeStatsMap(m1: mutable.Map[Any, Int], m2: mutable.Map[Any, Int]): Unit = {
    for (key <- m2.keySet)
      m1(key) = m1.getOrElse(key, 0) + m2(key)
  }

  /** Merge m2 into m1 */
  def mergeNestedStatsMap(m1: mutable.Map[String, mutable.Map[Any, Int]],
                          m2: Map[String, Map[Any, Int]]): Unit = {
    for ((field, fieldMap) <- m2) {
      if (m1.contains(field)) {
        for ((key, value) <- fieldMap) {
          if (m1(field).contains(key)) m1(field)(key) += value
          else m1(field)(key) = value
        }
      } else m1(field) = mutable.Map(fieldMap.toList: _*)
    }
  }

  /** Plots heatmaps on target tsv file */
  def plotHeatmap(file: File) {
    executeRscript(
      "plotHeatmap.R",
      Array(
        file.getAbsolutePath,
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.png",
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.clustering.png",
        file.getAbsolutePath.stripSuffix(".tsv") + ".heatmap.dendrogram.png"
      )
    )
  }

  /** Function to execute Rscript as subproces */
  def executeRscript(resource: String, args: Array[String]): Unit = {
    val is = getClass.getResourceAsStream(resource)
    val file = File.createTempFile("script.", "." + resource)
    file.deleteOnExit()
    val os = new FileOutputStream(file)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()

    val command: String = "Rscript " + file + " " + args.mkString(" ")

    logger.info("Starting: " + command)
    try {
      val process = Process(command).run(ProcessLogger(x => logger.debug(x), x => logger.debug(x)))
      if (process.exitValue() == 0) logger.info("Done: " + command)
      else {
        logger.warn("Failed: " + command)
        if (!logger.isDebugEnabled) logger.warn("Use -l debug for more info")
      }
    } catch {
      case e: IOException =>
        logger.warn("Failed: " + command)
        logger.debug(e)
    }
  }

}
