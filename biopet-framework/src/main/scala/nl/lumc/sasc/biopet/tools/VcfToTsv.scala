/*
 * Copyright 2014 pjvan_thof.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.lumc.sasc.biopet.tools

import htsjdk.variant.vcf.VCFFileReader
import java.io.File
import java.io.PrintStream
import nl.lumc.sasc.biopet.core.ToolCommand
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map, ListBuffer}

class VcfToTsv {
  // TODO: Queue wrapper
}

object VcfToTsv extends ToolCommand {
  case class Args (inputFile:File = null, outputFile:File = null, fields: List[String] = Nil, infoFields: List[String] = Nil,
                   sampleFileds: List[String] = Nil, disableDefaults: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required() maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(inputFile = x) }
    opt[File]('o', "outputFile") maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(outputFile = x) } text("output file, default to stdout")
    opt[String]('f', "field") unbounded() action { (x, c) =>
      c.copy(fields = x :: c.fields) }
    opt[String]('i', "info_field") unbounded() action { (x, c) =>
      c.copy(infoFields = x :: c.infoFields) }
    opt[String]('s', "sample_field") unbounded() action { (x, c) =>
      c.copy(sampleFileds = x :: c.sampleFileds) }
    opt[Boolean]('d', "disable_defaults") unbounded() action { (x, c) => 
      c.copy(disableDefaults = x) }
  }
  
  val defaultFields = List("chr", "pos", "id", "ref", "alt", "qual")
  
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)
    
    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val header = reader.getFileHeader
    val samples = header.getSampleNamesInOrder
    
    val fields = (if (commandArgs.disableDefaults) Nil else defaultFields) ::: commandArgs.fields ::: commandArgs.infoFields.map("INFO-"+_) ::: {
      val buffer: ListBuffer[String] = ListBuffer()
      for (f <- commandArgs.sampleFileds; sample <- samples) {
        buffer += sample+"-"+f
      }
      buffer.toList
    }
    
    val witter = if (commandArgs.outputFile != null) new PrintStream(commandArgs.outputFile)
    else scala.sys.process.stdout
    
    witter.println(fields.mkString("#", "\t", ""))
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
      values += "qual" -> (if (vcfRecord.getPhredScaledQual == -10) "." else scala.math.round(vcfRecord.getPhredScaledQual*100.0)/100.0)
      values += "filter" -> vcfRecord.getFilters
      for ((field, content) <- vcfRecord.getAttributes) {
        values += "INFO-"+field -> {
          content match {
            case a:List[_] => a.mkString(",")
            case a:Array[_] => a.mkString(",")
            case a:java.util.ArrayList[_] => a.mkString(",")
            case _ => content
          }
        }
      }
      for (sample <- samples) {
        val genotype = vcfRecord.getGenotype(sample)
        values += sample+"-GT" -> {
          val l = for (g <- genotype.getAlleles) yield vcfRecord.getAlleleIndex(g)
          l.map(x => if (x < 0) "." else x).mkString("/")
        }
        if (genotype.hasAD) values += sample+"-AD" -> List(genotype.getAD:_*).mkString(",")
        if (genotype.hasDP) values += sample+"-DP" -> genotype.getDP
        if (genotype.hasGQ )values += sample+"-GQ" -> genotype.getGQ
        if (genotype.hasPL) values += sample+"-PL" -> List(genotype.getPL:_*).mkString(",")
        for ((field, content) <- genotype.getExtendedAttributes) {
          values += sample+"-"+field -> content
        }
      }
      val line = for (f <- fields) yield {
        if (values.contains(f)) { 
          values(f) 
        } else ""
      }
      witter.println(line.mkString("\t"))
    }
  }
}