package nl.lumc.sasc.biopet.core.apps

import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMSequenceRecord
import java.io.File
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.PrintWriter
import scala.io.Source

class BedToInterval(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input Bed file", required = true)
  var input: File = _

  @Input(doc = "Bam File", required = true)
  var bamFile: File = _

  @Output(doc = "Output interval list", required = true)
  var output: File = _

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)

  override def commandLine = super.commandLine + required(input) + required(bamFile) + required(output)
}

object BedToInterval {
  def apply(root: Configurable, inputBed: File, inputBam: File, outputDir: String): BedToInterval = {
    val bedToInterval = new BedToInterval(root)
    bedToInterval.input = inputBed
    bedToInterval.bamFile = inputBam
    bedToInterval.output = new File(outputDir, inputBed.getName.stripSuffix(".bed") + ".interval")
    return bedToInterval
  }

  def apply(root: Configurable, inputBed: File, inputBam: File, output: File): BedToInterval = {
    val bedToInterval = new BedToInterval(root)
    bedToInterval.input = inputBed
    bedToInterval.bamFile = inputBam
    bedToInterval.output = output
    return bedToInterval
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val writer = new PrintWriter(args(2))

    val inputSam = new SAMFileReader(new File(args(1)))
    for (bla <- inputSam.getFileHeader.getSequenceDictionary.getSequences.toArray) {
      val record = bla.asInstanceOf[SAMSequenceRecord]
      writer.write("@SQ\tSN:" + record.getSequenceName + "\tLN:" + record.getSequenceLength + "\n")
    }
    inputSam.close

    val bedFile = Source.fromFile(args(0))
    for (line <- bedFile.getLines) {
      val split = line.split("\t")
      val chr = split(0)
      val start = split(1)
      val stop = split(2)
      writer.write(chr + "\t" + start + "\t" + stop + "\t")
      if (split.length >= 6 && (split(5) == "+" || split(5) == "-")) writer.write(split(5))
      else {
        var strand = "+"
        for (t <- 3 until split.length) {
          if ((split(t) == "+" || split(t) == "-")) strand = split(t)
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