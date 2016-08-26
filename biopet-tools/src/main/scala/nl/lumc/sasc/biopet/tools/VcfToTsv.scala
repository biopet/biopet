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

import java.io.{ File, PrintStream }
import java.text.DecimalFormat

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.{ ListBuffer, Map }

// TODO: Queue wrapper
object VcfToTsv extends ToolCommand {
  case class Args(inputFile: File = null, outputFile: File = null, fields: List[String] = Nil, infoFields: List[String] = Nil,
                  sampleFields: List[String] = Nil, disableDefaults: Boolean = false,
                  allInfo: Boolean = false, allFormat: Boolean = false,
                  separator: String = "\t", listSeparator: String = ",", maxDecimals: Int = 2) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "Input vcf file"
    opt[File]('o', "outputFile") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output file, default to stdout"
    opt[String]('f', "field") unbounded () action { (x, c) =>
      c.copy(fields = x :: c.fields)
    } text "Genotype field to use" valueName "Genotype field name"
    opt[String]('i', "info_field") unbounded () action { (x, c) =>
      c.copy(infoFields = x :: c.infoFields)
    } text "Info field to use" valueName "Info field name"
    opt[Unit]("all_info") unbounded () action { (x, c) =>
      c.copy(allInfo = true)
    } text "Use all info fields in the vcf header"
    opt[Unit]("all_format") unbounded () action { (x, c) =>
      c.copy(allFormat = true)
    } text "Use all genotype fields in the vcf header"
    opt[String]('s', "sample_field") unbounded () action { (x, c) =>
      c.copy(sampleFields = x :: c.sampleFields)
    } text "Genotype fields to use in the tsv file"
    opt[Unit]('d', "disable_defaults") unbounded () action { (x, c) =>
      c.copy(disableDefaults = true)
    } text "Don't output the default columns from the vcf file"
    opt[String]("separator") maxOccurs 1 action { (x, c) =>
      c.copy(separator = x)
    } text "Optional separator. Default is tab-delimited"
    opt[String]("list_separator") maxOccurs 1 action { (x, c) =>
      c.copy(listSeparator = x)
    } text "Optional list separator. By default, lists are separated by a comma"
    opt[Int]("max_decimals") maxOccurs 1 action { (x, c) =>
      c.copy(maxDecimals = x)
    } text "Number of decimal places for numbers. Default is 2"
  }

  val defaultFields = List("CHROM", "POS", "ID", "REF", "ALT", "QUAL")

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    // Throw exception if separator and listSeparator are identical
    if (commandArgs.separator == commandArgs.listSeparator) throw new IllegalArgumentException(
      "Separator and list_separator should not be identical"
    )

    val formatter = createFormatter(commandArgs.maxDecimals)

    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder

    val allInfoFields = header.getInfoHeaderLines.map(_.getID).toList
    val allFormatFields = header.getFormatHeaderLines.map(_.getID).toList

    val fields: Set[String] = (if (commandArgs.disableDefaults) Nil else defaultFields).toSet[String] ++
      commandArgs.fields.toSet[String] ++
      (if (commandArgs.allInfo) allInfoFields else commandArgs.infoFields).map("INFO-" + _) ++ {
        val buffer: ListBuffer[String] = ListBuffer()
        for (f <- if (commandArgs.allFormat) allFormatFields else commandArgs.sampleFields; sample <- samples) {
          buffer += sample + "-" + f
        }
        buffer.toSet[String]
      }

    val sortedFields = sortFields(fields, samples.toList)

    val writer = if (commandArgs.outputFile != null) new PrintStream(commandArgs.outputFile)
    else sys.process.stdout

    writer.println(sortedFields.mkString("#", commandArgs.separator, ""))
    for (vcfRecord <- reader) {
      val values: mutable.Map[String, Any] = mutable.Map()
      values += "CHROM" -> vcfRecord.getContig
      values += "POS" -> vcfRecord.getStart
      values += "ID" -> vcfRecord.getID
      values += "REF" -> vcfRecord.getReference.getBaseString
      values += "ALT" -> {
        val t = for (a <- vcfRecord.getAlternateAlleles) yield a.getBaseString
        t.mkString(commandArgs.listSeparator)
      }
      values += "QUAL" -> (if (vcfRecord.getPhredScaledQual == -10) "." else formatter.format(vcfRecord.getPhredScaledQual))
      values += "INFO" -> vcfRecord.getFilters
      for ((field, content) <- vcfRecord.getAttributes) {
        values += "INFO-" + field -> {
          content match {
            case a: List[_]                => a.mkString(commandArgs.listSeparator)
            case a: Array[_]               => a.mkString(commandArgs.listSeparator)
            case a: java.util.ArrayList[_] => a.mkString(commandArgs.listSeparator)
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
        if (genotype.hasAD) values += sample + "-AD" -> List(genotype.getAD: _*).mkString(commandArgs.listSeparator)
        if (genotype.hasDP) values += sample + "-DP" -> genotype.getDP
        if (genotype.hasGQ) values += sample + "-GQ" -> genotype.getGQ
        if (genotype.hasPL) values += sample + "-PL" -> List(genotype.getPL: _*).mkString(commandArgs.listSeparator)
        for ((field, content) <- genotype.getExtendedAttributes) {
          values += sample + "-" + field -> content
        }
      }
      val line = for (f <- sortedFields) yield {
        if (values.contains(f)) {
          values(f)
        } else ""
      }
      writer.println(line.mkString(commandArgs.separator))
    }
  }

  /**
   *  This function creates a correct DecimalFormat for a specific length of decimals
   * @param len number of decimal places
   * @return DecimalFormat formatter
   */
  def createFormatter(len: Int): DecimalFormat = {
    val patternString = "###." + (for (x <- 1 to len) yield "#").mkString("")
    new DecimalFormat(patternString)
  }

  /**
   * This fields sorts fields, such that non-info and non-sample specific fields (e.g. general ones) are on front
   * followed by info fields
   * followed by sample-specific fields
   * @param fields fields
   * @param samples samples
   * @return sorted samples
   */
  def sortFields(fields: Set[String], samples: List[String]): List[String] = {
    def fieldType(x: String) = x match {
      case _ if x.startsWith("INFO-") => 'i'
      case _ if samples.exists(y => x.startsWith(y + "-")) => 'f'
      case _ => 'g'
    }

    fields.toList.sortWith((a, b) => {
      (fieldType(a), fieldType(b)) match {
        case ('g', 'g') =>
          val ai = defaultFields.indexOf(a)
          val bi = defaultFields.indexOf(b)
          if (bi < 0) true else ai <= bi
        case ('f', 'f') =>
          val sampleA = a.split("-").head
          val sampleB = b.split("-").head
          sampleA.compareTo(sampleB) match {
            case 0          => !(a.compareTo(b) > 0)
            case i if i > 0 => false
            case _          => true
          }
        case ('g', _)             => true
        case (_, 'g')             => false
        case (a2, b2) if a2 == b2 => !(a2.compareTo(b2) > 0)
        case ('i', _)             => true
        case _                    => false
      }
    })
  }
}