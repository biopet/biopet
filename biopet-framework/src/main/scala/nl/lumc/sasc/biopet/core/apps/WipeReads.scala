/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.core.apps

import java.io.{ File, IOException }

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable


class WipeReads(val root: Configurable) extends BiopetJavaCommandLineFunction {

  javaMainClass = getClass.getName

  @Input(doc = "Input BAM file (must be indexed)", shortName = "I", required = true)
  var inputBAM: File = _

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBAM: File = _

}


object WipeReads {

  type OptionMap = Map[String, Any]

  object Strand extends Enumeration {
    type Strand = Value
    val Plus, Minus, Ignore = Value
  }

  def checkInputFile(inFile: File): File =
    if (inFile.exists)
      inFile
    else
      throw new IOException("Input file " + inFile.getPath + " not found")

  def checkInputBAM(inBAM: File): File = {
    // input BAM must have a .bam.bai index
    if (new File(inBAM.getPath + ".bai").exists)
      checkInputFile(inBAM)
    else
      throw new IOException("Index for input BAM file " + inBAM.getPath + " not found")
  }

  def parseOption(opts: OptionMap, list: List[String]): OptionMap =
    list match {
      case Nil
          => opts
      case ("--inputBAM" | "-I") :: value :: tail if !opts.contains("inputBAM")
          => parseOption(opts ++ Map("inputBAM" -> checkInputBAM(new File(value))), tail)
      case ("--targetRegions" | "-l") :: value :: tail if !opts.contains("targetRegions")
          => parseOption(opts ++ Map("targetRegions" -> checkInputFile(new File(value))), tail)
      case ("--outputBAM" | "-o") :: value :: tail if !opts.contains("outputBAM")
          => parseOption(opts ++ Map("outputBAM" -> new File(value)), tail)
      case ("--minOverlapFraction" | "-f") :: value :: tail if !opts.contains("minOverlapFraction")
          => parseOption(opts ++ Map("minOverlapFraction" -> value.toDouble), tail)
      case ("--minMapQ" | "-Q") :: value :: tail if !opts.contains("minMapQ")
          => parseOption(opts ++ Map("minMapQ" -> value.toInt), tail)
      case ("--strand" | "-s") :: (value @ ("plus" | "minus" | "ignore")) :: tail if !opts.contains("strand")
          => parseOption(opts ++ Map("strand" -> Strand.withName(value.capitalize)), tail)
      case ("--makeIndex") :: tail
          => parseOption(opts ++ Map("makeIndex" -> true), tail)
      case ("--limitToRegion" | "-limit") :: tail
          => parseOption(opts ++ Map("limitToRegion" -> true), tail)
      // TODO: better way to parse multiple flag values?
      case ("--readGroup" | "-RG") :: value :: tail if !opts.contains("readGroup")
          => parseOption(opts ++ Map("readGroup" -> value.split(",").toSeq), tail)
      case option :: tail
          => throw new IllegalArgumentException("Unexpected or duplicate option flag: " + option)
    }

  def validateOption(opts: OptionMap): Unit = {
    // TODO: better way to check for required arguments ~ use scalaz.Validation?
    if (opts.get("inputBAM") == None)
      throw new IllegalArgumentException("Input BAM not supplied")
    if (opts.get("targetRegions") == None)
      throw new IllegalArgumentException("Target regions file not supplied")
  }

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println(usage)
      System.exit(1)
    }
    val options = parseOption(Map(), args.toList)
    validateOption(options)
  }

  val usage: String =
    """
      |usage: java -cp BiopetFramework.jar nl.lumc.sasc.biopet-core.apps.WipeReads [options] -I input -l regions -o output
      |
      |WipeReads - Tool for reads removal from an indexed BAM file.
      |
      |positional arguments:
      |  -I,--inputBAM              Input BAM file, must be indexed with '.bam.bai' extension
      |  -l,--targetRegions         Input BED file
      |  -o,--outputBAM             Output BAM file
      |
      |optional arguments:
      |  -f,--minOverlapFraction    Minimum overlap of reads and target regions
      |
      |This tool will remove BAM records that overlaps a set of given regions.
      |By default, if the removed reads are also mapped to other regions outside
      |the given ones, they will also be removed.
    """.stripMargin


}
