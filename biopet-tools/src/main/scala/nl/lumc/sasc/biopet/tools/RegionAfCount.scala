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
package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}
import java.util

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.rscript.ScatterPlot
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object RegionAfCount extends ToolCommand {
  case class Args(bedFile: File = null,
                  outputPrefix: String = null,
                  scatterpPlot: Boolean = false,
                  vcfFiles: List[File] = Nil)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('b', "bedFile") unbounded () required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(bedFile = x)
    }
    opt[String]('o', "outputPrefix") unbounded () required () maxOccurs 1 valueName "<file prefix>" action {
      (x, c) =>
        c.copy(outputPrefix = x)
    }
    opt[Unit]('s', "scatterPlot") unbounded () action { (x, c) =>
      c.copy(scatterpPlot = true)
    }
    opt[File]('V', "vcfFile") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(vcfFiles = c.vcfFiles ::: x :: Nil)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")
    logger.info("Reading bed file")

    val bedRecords = BedRecordList.fromFile(cmdArgs.bedFile).sorted

    logger.info(s"Combine ${bedRecords.allRecords.size} bed records")

    val combinedBedRecords = bedRecords.combineOverlap

    logger.info(s"${combinedBedRecords.allRecords.size} left")
    logger.info(s"${combinedBedRecords.allRecords.size * cmdArgs.vcfFiles.size} query's to do")
    logger.info("Reading vcf files")

    case class AfCounts(var names: Double = 0,
                        var namesExons: Double = 0,
                        var namesIntrons: Double = 0,
                        var namesCoding: Double = 0,
                        var utr: Double = 0,
                        var utr5: Double = 0,
                        var utr3: Double = 0)

    var c = 0
    val afCounts = (for (vcfFile <- cmdArgs.vcfFiles.par)
      yield
        vcfFile -> {
          val reader = new VCFFileReader(vcfFile, true)
          val afCounts: mutable.Map[String, AfCounts] = mutable.Map()
          for (region <- combinedBedRecords.allRecords) yield {
            val originals = region.originals()
            for (variant <- reader.query(region.chr, region.start, region.end)) {
              val sum = (variant.getAttribute("AF", 0) match {
                case a: util.ArrayList[_] => a.map(_.toString.toDouble).toArray
                case s => Array(s.toString.toDouble)
              }).sum
              val interval = BedRecord(variant.getContig, variant.getStart, variant.getEnd)
              originals.foreach { x =>
                val name = x.name.getOrElse(s"${x.chr}:${x.start}-${x.end}")
                if (!afCounts.contains(name)) afCounts += name -> AfCounts()
                afCounts(name).names += sum
                val exons = x.exons.getOrElse(Seq()).filter(_.overlapWith(interval))
                val introns = x.introns.getOrElse(Seq()).filter(_.overlapWith(interval))
                val utr5 = x.utr5.map(_.overlapWith(interval))
                val utr3 = x.utr3.map(_.overlapWith(interval))
                if (exons.nonEmpty) {
                  afCounts(name).namesExons += sum
                  if (!utr5.getOrElse(false) && !utr3.getOrElse(false))
                    afCounts(name).namesCoding += sum
                }
                if (introns.nonEmpty) afCounts(name).namesIntrons += sum
                if (utr5.getOrElse(false) || utr3.getOrElse(false)) afCounts(name).utr += sum
                if (utr5.getOrElse(false)) afCounts(name).utr5 += sum
                if (utr3.getOrElse(false)) afCounts(name).utr3 += sum
              }
            }
            c += 1
            if (c % 100 == 0) logger.info(s"$c regions done")
          }
          afCounts.toMap
        }).toMap

    logger.info(s"Done reading, ${c} regions")

    logger.info("Writing output files")

    def writeOutput(tsvFile: File, function: AfCounts => Double): Unit = {
      val writer = new PrintWriter(tsvFile)
      writer.println("\t" + cmdArgs.vcfFiles.map(_.getName).mkString("\t"))
      for (r <- cmdArgs.vcfFiles.foldLeft(Set[String]())((a, b) => a ++ afCounts(b).keySet)) {
        writer.print(r + "\t")
        writer.println(
          cmdArgs.vcfFiles.map(x => function(afCounts(x).getOrElse(r, AfCounts()))).mkString("\t"))
      }
      writer.close()

      if (cmdArgs.scatterpPlot) generatePlot(tsvFile)
    }

    def generatePlot(tsvFile: File)(implicit ec: ExecutionContext): Unit = {
      logger.info(s"Generate plot for $tsvFile")

      val scatterPlot = new ScatterPlot(null)
      scatterPlot.input = tsvFile
      scatterPlot.output = new File(tsvFile.getAbsolutePath.stripSuffix(".tsv") + ".png")
      scatterPlot.ylabel = Some("Sum of AFs")
      scatterPlot.width = Some(1200)
      scatterPlot.height = Some(1000)
      scatterPlot.runLocal()
    }
    for (arg <- List[(File, AfCounts => Double)](
           (new File(cmdArgs.outputPrefix + ".names.tsv"), _.names),
           (new File(cmdArgs.outputPrefix + ".names.exons_only.tsv"), _.namesExons),
           (new File(cmdArgs.outputPrefix + ".names.introns_only.tsv"), _.namesIntrons),
           (new File(cmdArgs.outputPrefix + ".names.coding.tsv"), _.namesCoding),
           (new File(cmdArgs.outputPrefix + ".names.utr.tsv"), _.utr),
           (new File(cmdArgs.outputPrefix + ".names.utr5.tsv"), _.utr5),
           (new File(cmdArgs.outputPrefix + ".names.utr3.tsv"), _.utr3)
         ).par) writeOutput(arg._1, arg._2)

    logger.info("Done")
  }
}
