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
package nl.lumc.sasc.biopet.tools

import java.io.{ PrintWriter, InputStream, File }
import java.util

import htsjdk.samtools.util.Interval
import htsjdk.samtools.{ QueryInterval, SAMRecord, SamReader, SamReaderFactory }
import htsjdk.tribble.AbstractFeatureReader._
import htsjdk.tribble.{ AbstractFeatureReader, TabixFeatureReader }
import htsjdk.tribble.bed.{ BEDFeature, SimpleBEDFeature, BEDCodec, FullBEDFeature }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.variantcontext.{ VariantContext, VariantContextBuilder }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine }
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.extensions.rscript.ScatterPlot
import nl.lumc.sasc.biopet.utils.intervals.{ BedRecord, BedRecordList }

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.math._

object RegionAfCount extends ToolCommand {
  case class Args(bedFile: File = null,
                  outputFile: File = null,
                  scatterpPlot: Option[File] = None,
                  vcfFiles: List[File] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('b', "bedFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(bedFile = x)
    }
    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('s', "scatterPlot") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(scatterpPlot = Some(x))
    }
    opt[File]('V', "vcfFile") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(vcfFiles = c.vcfFiles ::: x :: Nil)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    logger.info("Start")
    logger.info("Reading bed file")

    val bedRecords = BedRecordList.fromFile(cmdArgs.bedFile).sort

    logger.info(s"Combine ${bedRecords.allRecords.size} bed records")

    val combinedBedRecords = bedRecords.combineOverlap

    logger.info(s"${combinedBedRecords.allRecords.size} left")

    logger.info("Reading vcf files")

    var c = 0
    val afCountsRaw = for (region <- combinedBedRecords.allRecords.par) yield {
      val sum = (for (vcfFile <- cmdArgs.vcfFiles.par) yield vcfFile -> {
        val afCounts = mutable.Map[String, Double]()
        val reader = new VCFFileReader(vcfFile, true)
        val it = reader.query(region.chr, region.start, region.end)
        for (variant <- it) {
          val sum = (variant.getAttribute("AF", 0) match {
            case a: util.ArrayList[_] => a.map(_.toString.toDouble).toArray
            case s                    => Array(s.toString.toDouble)
          }).sum
          region.originals()
            .map(x => x.name.getOrElse(s"${x.chr}:${x.start}-${x.end}"))
            .distinct
            .foreach(name => afCounts += name -> (afCounts.getOrElse(name, 0.0) + sum))
        }
        reader.close()
        afCounts.toMap
      }).toMap

      c += 1
      if (c % 100 == 0) logger.info(s"$c regions done")

      sum
    }

    logger.info(s"Done reading, $c regions")

    val afCounts: Map[String, Map[File, Double]] = {
      val combinedAfCounts: mutable.Map[String, mutable.Map[File, Double]] = mutable.Map()
      for (x <- afCountsRaw.toList; (file, counts) <- x.toList; (name, count) <- counts) {
        val map = combinedAfCounts.getOrElse(name, mutable.Map())
        map += file -> (map.getOrElse(file, 0.0) + count)
        combinedAfCounts += name -> map
      }
      combinedAfCounts.map(x => x._1 -> x._2.toMap).toMap
    }

    logger.info("Writing output file")

    val writer = new PrintWriter(cmdArgs.outputFile)
    writer.println("\t" + cmdArgs.vcfFiles.map(_.getName).mkString("\t"))
    for (r <- afCounts.keys) {
      writer.print(r + "\t")
      writer.println(cmdArgs.vcfFiles.map(afCounts(r).getOrElse(_, 0.0)).mkString("\t"))
    }
    writer.close()

    cmdArgs.scatterpPlot.foreach { scatterPlotFile =>
      logger.info("Generate plot")

      val scatterPlot = new ScatterPlot(null)
      scatterPlot.input = cmdArgs.outputFile
      scatterPlot.output = scatterPlotFile
      scatterPlot.ylabel = Some("Sum of AFs")
      scatterPlot.width = Some(1200)
      scatterPlot.height = Some(1000)
      scatterPlot.runLocal()
    }

    logger.info("Done")
  }
}