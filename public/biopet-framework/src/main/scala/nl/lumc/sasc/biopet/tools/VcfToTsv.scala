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

import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import java.io.PrintStream
import nl.lumc.sasc.biopet.core.ToolCommand
import scala.collection.JavaConversions._
import scala.collection.mutable.{ Map, ListBuffer }

class VcfToTsv {
  // TODO: Queue wrapper
}

object VcfToTsv extends ToolCommand {
  case class Args(inputFile: File = null, outputFile: File = null, fields: List[String] = Nil, infoFields: List[String] = Nil,
                  sampleFileds: List[String] = Nil, disableDefaults: Boolean = false,
                  allInfo: Boolean = false, allFormat: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[File]('o', "outputFile") maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(outputFile = x)
    } text ("output file, default to stdout")
    opt[String]('f', "field") unbounded () action { (x, c) =>
      c.copy(fields = x :: c.fields)
    }
    opt[String]('i', "info_field") unbounded () action { (x, c) =>
      c.copy(infoFields = x :: c.infoFields)
    }
    opt[Unit]("all_info") unbounded () action { (x, c) =>
      c.copy(allInfo = true)
    }
    opt[Unit]("all_format") unbounded () action { (x, c) =>
      c.copy(allFormat = true)
    }
    opt[String]('s', "sample_field") unbounded () action { (x, c) =>
      c.copy(sampleFileds = x :: c.sampleFileds)
    }
    opt[Unit]('d', "disable_defaults") unbounded () action { (x, c) =>
      c.copy(disableDefaults = true)
    }
  }

  val defaultFields = List("chr", "pos", "id", "ref", "alt", "qual")

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder

    val allInfoFields = header.getInfoHeaderLines.map(_.getID).toList
    val allFormatFields = header.getFormatHeaderLines.map(_.getID).toList

    val fields: Set[String] = (if (commandArgs.disableDefaults) Nil else defaultFields).toSet[String] ++
      commandArgs.fields.toSet[String] ++
      (if (commandArgs.allInfo) allInfoFields else commandArgs.infoFields).map("INFO-" + _) ++ {
        val buffer: ListBuffer[String] = ListBuffer()
        for (f <- (if (commandArgs.allFormat) allFormatFields else commandArgs.sampleFileds); sample <- samples) {
          buffer += sample + "-" + f
        }
        buffer.toSet[String]
      }

    val sortedFields = fields.toList.sortWith((a, b) => {
      val aT = if (a.startsWith("INFO-")) 'i' else if (samples.exists(x => a.startsWith(x + "-"))) 'f' else 'g'
      val bT = if (b.startsWith("INFO-")) 'i' else if (samples.exists(x => b.startsWith(x + "-"))) 'f' else 'g'
      if (aT == 'g' && bT == 'g') {
        val ai = defaultFields.indexOf(a)
        val bi = defaultFields.indexOf(b)
        if (bi < 0) true
        else ai <= bi
      } else if (aT == 'g') true
      else if (bT == 'g') false
      else if (aT == bT) (if (a.compareTo(b) > 0) false else true)
      else if (aT == 'i') true
      else false
    })

    val witter = if (commandArgs.outputFile != null) new PrintStream(commandArgs.outputFile)
    else sys.process.stdout

    witter.println(sortedFields.mkString("#", "\t", ""))
    for (vcfRecord <- reader) {
      val values: Map[String, Any] = Map()
      values += "chr" -> vcfRecord.getChr
      values += "pos" -> vcfRecord.getStart
      values += "id" -> vcfRecord.getID
      values += "ref" -> vcfRecord.getReference.getBaseString
      values += "alt" -> {
        val t = for (a <- vcfRecord.getAlternateAlleles) yield a.getBaseString
        t.mkString(",")
      }
      values += "qual" -> (if (vcfRecord.getPhredScaledQual == -10) "." else scala.math.round(vcfRecord.getPhredScaledQual * 100.0) / 100.0)
      values += "filter" -> vcfRecord.getFilters
      for ((field, content) <- vcfRecord.getAttributes) {
        values += "INFO-" + field -> {
          content match {
            case a: List[_]                => a.mkString(",")
            case a: Array[_]               => a.mkString(",")
            case a: java.util.ArrayList[_] => a.mkString(",")
            case _                         => content
          }
        }
      }

      for (sample <- samples) {
        val genotype = vcfRecord.getGenotype(sample)
        values += sample + "-GT" -> {
          val l = for (g <- genotype.getAlleles) yield vcfRecord.getAlleleIndex(g)
          l.map(x => if (x < 0) "." else x).mkString("/")
        }
        if (genotype.hasAD) values += sample + "-AD" -> List(genotype.getAD: _*).mkString(",")
        if (genotype.hasDP) values += sample + "-DP" -> genotype.getDP
        if (genotype.hasGQ) values += sample + "-GQ" -> genotype.getGQ
        if (genotype.hasPL) values += sample + "-PL" -> List(genotype.getPL: _*).mkString(",")
        for ((field, content) <- genotype.getExtendedAttributes) {
          values += sample + "-" + field -> content
        }
      }
      val line = for (f <- sortedFields) yield {
        if (values.contains(f)) {
          values(f)
        } else ""
      }
      witter.println(line.mkString("\t"))
    }
  }
}