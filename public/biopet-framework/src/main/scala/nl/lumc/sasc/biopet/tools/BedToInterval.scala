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

import java.io.{ File, PrintWriter }

import htsjdk.samtools.{ SAMSequenceRecord, SamReaderFactory }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ ToolCommand, ToolCommandFuntion }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.io.Source

/**
 * @deprecated Use picard.util.BedToIntervalList instead
 */
class BedToInterval(val root: Configurable) extends ToolCommandFuntion {
  javaMainClass = getClass.getName

  @Input(doc = "Input Bed file", required = true)
  var input: File = _

  @Input(doc = "Bam File", required = true)
  var bamFile: File = _

  @Output(doc = "Output interval list", required = true)
  var output: File = _

  override val defaultCoreMemory = 1.0

  override def commandLine = super.commandLine + required("-I", input) + required("-b", bamFile) + required("-o", output)
}

/**
 * @deprecated Use picard.util.BedToIntervalList instead
 */
object BedToInterval extends ToolCommand {
  def apply(root: Configurable, inputBed: File, inputBam: File, output: File): BedToInterval = {
    val bedToInterval = new BedToInterval(root)
    bedToInterval.input = inputBed
    bedToInterval.bamFile = inputBam
    bedToInterval.output = output
    bedToInterval
  }

  case class Args(inputFile: File = null, outputFile: File = null, bamFile: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[File]('o', "output") required () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val writer = new PrintWriter(commandArgs.outputFile)

    val inputSam = SamReaderFactory.makeDefault.open(commandArgs.bamFile)
    val refs = for (SQ <- inputSam.getFileHeader.getSequenceDictionary.getSequences.toArray) yield {
      val record = SQ.asInstanceOf[SAMSequenceRecord]
      writer.write("@SQ\tSN:" + record.getSequenceName + "\tLN:" + record.getSequenceLength + "\n")
      record.getSequenceName -> record.getSequenceLength
    }
    inputSam.close()
    val refsMap = Map(refs: _*)

    val bedFile = Source.fromFile(commandArgs.inputFile)
    for (
      line <- bedFile.getLines();
      split = line.split("\t") if split.size >= 3;
      chr = split(0);
      start = split(1);
      stop = split(2) if start forall Character.isDigit if stop forall Character.isDigit
    ) {
      if (!refsMap.contains(chr)) throw new IllegalStateException("Chr '" + chr + "' in bed file not found in bam file")
      writer.write(chr + "\t" + start + "\t" + stop + "\t")
      if (split.length >= 6 && (split(5) == "+" || split(5) == "-")) writer.write(split(5))
      else {
        var strand = "+"
        for (t <- 3 until split.length) {
          if (split(t) == "+" || split(t) == "-") strand = split(t)
        }
        writer.write(strand)
      }
      writer.write("\t" + chr + ":" + start + "-" + stop)
      for (t <- 3 until split.length) writer.write(":" + split(t))
      writer.write("\n")
    }

    writer.close()
  }
}