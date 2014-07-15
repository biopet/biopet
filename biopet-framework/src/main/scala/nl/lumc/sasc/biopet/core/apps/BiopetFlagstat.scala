package nl.lumc.sasc.biopet.core.apps

import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMRecord
import java.io.{BufferedInputStream, File, FileInputStream, PrintWriter}
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import scala.io.Source
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}
import org.broadinstitute.gatk.queue.util.Logging
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

class BiopetFlagstat(val root:Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName
  
  @Input(doc="Input bam", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc="Output flagstat", shortName="output", required = true)
  var output: File = _
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
  
  override def commandLine = super.commandLine + required(input) + " > " + required(output)
}

object BiopetFlagstat extends Logging {
  def apply(root:Configurable, input:File, output:File) : BiopetFlagstat = {
    val flagstat = new BiopetFlagstat(root)
    flagstat.input = input
    flagstat.output = output
    return flagstat
  }
  def apply(root:Configurable, input:File, outputDir:String) : BiopetFlagstat = {
    val flagstat = new BiopetFlagstat(root)
    flagstat.input = input
    flagstat.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".biopetflagstat")
    return flagstat
  }
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val inputSam = new SAMFileReader(new File(args(0)))
    
    val flagstatCollector = new FlagstatCollector
    flagstatCollector.loadDefaultFunctions
    val m = 10
    val max = 60
    for (t <- 0 to (max / m))
      flagstatCollector.addFunction("MAPQ>"+(t * m), record => record.getMappingQuality > (t * m))
    flagstatCollector.addFunction("First normal, second read inverted (paired end orientation)", record => {
      if (record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
          ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))
      ) true
      else false
    })
    flagstatCollector.addFunction("First normal, second read normal", record => {
      if (record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
          ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))
      ) true
      else false
    })
    flagstatCollector.addFunction("First inverted, second read inverted", record => {
      if (record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
          ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))
      ) true
      else false
    })
    flagstatCollector.addFunction("First inverted, second read normal", record => {
      if (record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
          ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
           (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))
      ) true
      else false
    })
    flagstatCollector.addFunction("Mate in same strand", record => record.getReadNegativeStrandFlag && record.getMateNegativeStrandFlag &&
                                                                      record.getReferenceIndex == record.getMateReferenceIndex)
    flagstatCollector.addFunction("Mate on other chr", record => record.getReferenceIndex != record.getMateReferenceIndex)
    
    for (record <- inputSam.iterator) {
      if (flagstatCollector.readsCount % 1e6 == 0 && flagstatCollector.readsCount > 0)
        System.err.println("Reads prosessed: " + flagstatCollector.readsCount)
      flagstatCollector.loadRecord(record)
    }
    
    println(flagstatCollector.report)
  }
  
  class FlagstatCollector {    
    private var functionCount = 0
    var readsCount = 0
    private val names: Map[Int, String] = Map()
    private var functions: Array[SAMRecord => Boolean] = Array()
    private var totalCounts: Array[Long] = Array()
    private var crossCounts = Array.ofDim[Int](1,1)
        
    def loadDefaultFunctions {
      addFunction("All", record => true)
      addFunction("Mapped", record => !record.getReadUnmappedFlag)
      addFunction("Duplicates", record => record.getDuplicateReadFlag)
      addFunction("FirstOfPair", record => record.getFirstOfPairFlag)
      addFunction("SecondOfPair", record => record.getSecondOfPairFlag)

      addFunction("ReadNegativeStrand", record => record.getReadNegativeStrandFlag)
      
      addFunction("NotPrimaryAlignment", record => record.getNotPrimaryAlignmentFlag)

      addFunction("ReadPaired", record => record.getReadPairedFlag)
      addFunction("ProperPair", record => record.getProperPairFlag)

      addFunction("MateNegativeStrand", record => record.getMateNegativeStrandFlag)
      addFunction("MateUnmapped", record => record.getMateUnmappedFlag)

      addFunction("ReadFailsVendorQualityCheck", record => record.getReadFailsVendorQualityCheckFlag)
      addFunction("SupplementaryAlignment", record => record.getSupplementaryAlignmentFlag)
      addFunction("SecondaryOrSupplementary", record => record.isSecondaryOrSupplementary)
    }
    
    def loadRecord(record:SAMRecord) {
      readsCount += 1
      val values: Array[Boolean] = new Array(names.size)
      for (t <- 0 until names.size) {
        values(t) = functions(t)(record)
        if (values(t)) {
          totalCounts(t) += 1
        }
      }
      for (t <- 0 until names.size) {
        for (t2 <- 0 until names.size) {
          if (values(t) && values(t2)) {
            crossCounts(t)(t2) += 1
          }
        }
      }
    }
    
    def addFunction(name:String, function:SAMRecord => Boolean) {
      functionCount += 1
      crossCounts = Array.ofDim[Int](functionCount,functionCount)
      totalCounts = new Array[Long](functionCount)
      val temp = new Array[SAMRecord => Boolean](functionCount)
      for (t <- 0 until (temp.size - 1)) temp(t) = functions(t)
      functions = temp
      
      val index = functionCount - 1
      names += (index -> name)
      functions(index) = function
      totalCounts(index) = 0
    }
    
    def report: String = {
      val buffer = new StringBuilder
      buffer.append("Number\tTotal Flags\tFraction\tName\n")
      for (t <- 0 until names.size) {
        val precentage = (totalCounts(t).toFloat / readsCount) * 100
        buffer.append("#" + (t+1) + "\t" + totalCounts(t) + "\t" + f"$precentage%.4f" + "%\t" + names(t) + "\n")
      }
      buffer.append("\n")
      
      buffer.append(crossReport() + "\n")
      buffer.append(crossReport(fraction = true) + "\n")
      
      return buffer.toString
    }
    
    def crossReport(fraction:Boolean = false): String = {
      val buffer = new StringBuilder
      
      for (t <- 0 until names.size) // Header for table
        buffer.append("\t#" + (t+1))
      buffer.append("\n")
      
      for (t <- 0 until names.size) {
        buffer.append("#" + (t+1) + "\t")
        for (t2 <- 0 until names.size) {
          val reads = crossCounts(t)(t2)
          if (fraction) {
            val precentage = (reads.toFloat / totalCounts(t).toFloat) * 100
            buffer.append(f"$precentage%.4f" + "%")
          } else buffer.append(reads)
          if (t2 == names.size-1) buffer.append("\n")
          else buffer.append("\t")
        }
      }
      return buffer.toString
    }
  } // End of class
  
}
