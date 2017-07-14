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

import java.io.{File, PrintWriter}

import scala.collection.mutable

import nl.lumc.sasc.biopet.utils.sortAnyAny

/**
  * General stats class to store vcf stats
  *
  * @param generalStats Stores are general stats
  * @param samplesStats Stores all sample/genotype specific stats
  */
case class Stats(generalStats: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] =
                   mutable.Map(),
                 samplesStats: mutable.Map[String, SampleStats] = mutable.Map()) {

  /** Add an other class */
  def +=(other: Stats): Stats = {
    for ((key, value) <- other.samplesStats) {
      if (this.samplesStats.contains(key)) this.samplesStats(key) += value
      else this.samplesStats(key) = value
    }
    for ((chr, chrMap) <- other.generalStats; (field, fieldMap) <- chrMap) {
      if (!this.generalStats.contains(chr))
        generalStats += (chr -> mutable.Map[String, mutable.Map[Any, Int]]())
      val thisField = this.generalStats(chr).get(field)
      if (thisField.isDefined) Stats.mergeStatsMap(thisField.get, fieldMap)
      else this.generalStats(chr) += field -> fieldMap
    }
    this
  }

  /** Function to write 1 specific general field */
  def writeField(field: String,
                 outputDir: File,
                 prefix: String = "",
                 chr: String = "total"): File = {
    val file = (prefix, chr) match {
      case ("", "total") => new File(outputDir, field + ".tsv")
      case (_, "total") => new File(outputDir, prefix + "-" + field + ".tsv")
      case ("", _) => new File(outputDir, chr + "-" + field + ".tsv")
      case _ => new File(outputDir, prefix + "-" + chr + "-" + field + ".tsv")
    }

    val data = this.generalStats
      .getOrElse(chr, mutable.Map[String, mutable.Map[Any, Int]]())
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
  def getField(field: String, chr: String = "total"): Map[String, Array[Any]] = {

    val data = this.generalStats
      .getOrElse(chr, mutable.Map[String, mutable.Map[Any, Int]]())
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
                         prefix: String = "",
                         chr: String = "total"): Unit = {
    val file = (prefix, chr) match {
      case ("", "total") => new File(outputDir, field + ".tsv")
      case (_, "total") => new File(outputDir, prefix + "-" + field + ".tsv")
      case ("", _) => new File(outputDir, chr + "-" + field + ".tsv")
      case _ => new File(outputDir, prefix + "-" + chr + "-" + field + ".tsv")
    }

    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println(samples.mkString(field + "\t", "\t", ""))
    val keySet = (for (sample <- samples)
      yield
        this
          .samplesStats(sample)
          .genotypeStats
          .getOrElse(chr, Map[String, Map[Any, Int]]())
          .getOrElse(field, Map[Any, Int]())
          .keySet).fold(Set[Any]())(_ ++ _)
    for (key <- keySet.toList.sortWith(sortAnyAny)) {
      val values = for (sample <- samples)
        yield
          this
            .samplesStats(sample)
            .genotypeStats
            .getOrElse(chr, Map[String, Map[Any, Int]]())
            .getOrElse(field, Map[Any, Int]())
            .getOrElse(key, 0)
      writer.println(values.mkString(key + "\t", "\t", ""))
    }
    writer.close()
  }

  /** Function to write 1 specific genotype field */
  def getGenotypeField(samples: List[String],
                       field: String,
                       chr: String = "total"): Map[String, Map[String, Any]] = {
    val keySet = (for (sample <- samples)
      yield
        this
          .samplesStats(sample)
          .genotypeStats
          .getOrElse(chr, Map[String, Map[Any, Int]]())
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
                  .getOrElse(chr, Map[String, Map[Any, Int]]())
                  .getOrElse(field, Map[Any, Int]())
                  .get(key))
            .filter(_._2.isDefined)
            .toMap
        }).toMap
  }

  /** This will generate stats for one contig */
  def getContigStats(contig: String,
                     samples: List[String],
                     genotypeFields: List[String] = Nil,
                     infoFields: List[String] = Nil,
                     sampleDistributions: List[String] = Nil): Map[String, Any] = {
    Map(
      "genotype" -> genotypeFields.map(f => f -> getGenotypeField(samples, f, contig)).toMap,
      "info" -> infoFields.map(f => f -> getField(f, contig)).toMap,
      "sample_distributions" -> sampleDistributions
        .map(f => f -> getField("SampleDistribution-" + f, contig))
        .toMap
    )
  }

  /** This will generate stats for total */
  def getTotalStats(samples: List[String],
                    genotypeFields: List[String] = Nil,
                    infoFields: List[String] = Nil,
                    sampleDistributions: List[String] = Nil): Map[String, Any] =
    getContigStats("total", samples, genotypeFields, infoFields, sampleDistributions)

  /** This will generate stats for total and contigs separated */
  def getAllStats(contigs: List[String],
                  samples: List[String],
                  genotypeFields: List[String] = Nil,
                  infoFields: List[String] = Nil,
                  sampleDistributions: List[String] = Nil): Map[String, Any] = {
    Map(
      "contigs" -> contigs
        .map(c => c -> getContigStats(c, samples, genotypeFields, infoFields, sampleDistributions))
        .toMap,
      "total" -> getTotalStats(samples, genotypeFields, infoFields, sampleDistributions)
    )
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
  def mergeNestedStatsMap(m1: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]],
                          m2: Map[String, Map[String, Map[Any, Int]]]): Unit = {
    for ((chr, chrMap) <- m2; (field, fieldMap) <- chrMap) {
      if (m1.contains(chr)) {
        if (m1(chr).contains(field)) {
          for ((key, value) <- fieldMap) {
            if (m1(chr)(field).contains(key)) m1(chr)(field)(key) += value
            else m1(chr)(field)(key) = value
          }
        } else m1(chr)(field) = mutable.Map(fieldMap.toList: _*)
      } else m1(chr) = mutable.Map(field -> mutable.Map(fieldMap.toList: _*))
    }
  }
}
