package nl.lumc.sasc.biopet.tools

import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMRecord
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.AsyncVariantContextWriter
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.variant.vcf.VCFInfoHeaderLine
import java.io.File
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.collection.JavaConversions._
import scala.collection.mutable
import htsjdk.variant.vcf.VCFHeaderLineType
import htsjdk.variant.vcf.VCFHeaderLineCount
import scala.math._

class CheckAllelesVcfInBam(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName
  
  @Input(doc = "Input vcf file", shortName = "V", required = true)
  var inputFile: File = _

  @Input(doc = "Input bam file", shortName = "bam", required = true)
  var bamFiles: List[File] = Nil
  
  @Output(doc = "Output vcf file", shortName = "o", required = true)
  var output: File = _

  override def commandLine = super.commandLine + required("-V", inputFile) + repeat("-bam", bamFiles) + required(output)
}

object CheckAllelesVcfInBam extends ToolCommand {
  case class Args (inputFile:File = null, outputFile:File = null, samples:List[String] = Nil, bamFiles:List[File] = Nil, minMapQual:Int = 1) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required() maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(inputFile = x) }
    opt[File]('o', "outputFile") required() maxOccurs(1) valueName("<file>") action { (x, c) =>
      c.copy(outputFile = x) }
    opt[String]('s', "sample") unbounded() minOccurs(1) action { (x, c) =>
      c.copy(samples = x :: c.samples) }
    opt[File]('b', "bam") unbounded() minOccurs(1) action { (x, c) =>
      c.copy(bamFiles = x :: c.bamFiles) }
    opt[Int]('m', "min_mapping_quality") maxOccurs(1) action { (x, c) =>
      c.copy(minMapQual = c.minMapQual) }
  }
  
  private class CountReport (
    var notFound: Int = 0,
    var aCounts: mutable.Map[String, Int] = mutable.Map(),
    var duplicateReads: Int = 0,
    var lowMapQualReads: Int = 0
  )
  
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)
    
    if (commandArgs.bamFiles.size != commandArgs.samples.size)
      logger.warn("Number of samples is diffrent then number of bam files, left over will be removed")
    val bamReaders:Map[String,SAMFileReader] = Map(commandArgs.samples zip commandArgs.bamFiles.map(x => new SAMFileReader(x)):_*)
    val bamHeaders = bamReaders.map(x => (x._1, x._2.getFileHeader))
    
    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().setOutputFile(commandArgs.outputFile).build)
    
    val header = reader.getFileHeader
    for ((sample,_) <- bamReaders) {
      header.addMetaDataLine(new VCFInfoHeaderLine("BAM-AD-" + sample, 
                              VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Allele depth, ref and alt on order of vcf file"))
      header.addMetaDataLine(new VCFInfoHeaderLine("BAM-DP-" + sample, 
                              1, VCFHeaderLineType.Integer, "Total reads on this location"))
    }
    
    writer.writeHeader(header)
    
    for (vcfRecord <- reader) {
      val countReports: Map[String,CountReport] = bamReaders.map(x => (x._1, new CountReport))
      val refAllele = vcfRecord.getReference.getBaseString
      for ((sample, bamReader) <- bamReaders) {
        val queryInterval = new SAMFileReader.QueryInterval(bamHeaders(sample).getSequenceIndex(vcfRecord.getChr), 
                                                            vcfRecord.getStart, vcfRecord.getStart + refAllele.size - 1)
        val bamIter = bamReader.query(Array(queryInterval), false)
        
        def filterRead(samRecord:SAMRecord): Boolean = {
          if (samRecord.getDuplicateReadFlag) {
            countReports(sample).duplicateReads += 1
            return true
          }
          if (samRecord.getSupplementaryAlignmentFlag) return true
          if (samRecord.getNotPrimaryAlignmentFlag) return true
          if (samRecord.getMappingQuality < commandArgs.minMapQual) {
            countReports(sample).lowMapQualReads += 1
            return true
          }
          return false
        }
        
        val counts = for (samRecord <- bamIter if !filterRead(samRecord)) {
          checkAlles(samRecord, vcfRecord) match {
            case Some(a) => if (countReports(sample).aCounts.contains(a)) countReports(sample).aCounts(a) += 1 
                            else countReports(sample).aCounts += (a -> 1)
            case _ => countReports(sample).notFound += 1
          }
        }        
        bamIter.close
      }
      
      val builder = new VariantContextBuilder(vcfRecord)
      for ((k,v) <- countReports) {
        val s = for (allele <- vcfRecord.getAlleles) yield {
          val s = allele.getBaseString
          if (v.aCounts.contains(s)) v.aCounts(s)
          else 0
        }
        builder.attribute("BAM-AD-" + k, s.mkString(","))
        builder.attribute("BAM-DP-" + k, (0 /: s) (_ + _) + v.notFound)
      }
      writer.add(builder.make)
    }
    for ((_, r) <- bamReaders) r.close
    reader.close
    writer.close
  }
  
  def checkAlles(samRecord:SAMRecord, vcfRecord:VariantContext): Option[String] = {
    val readStartPos = List.range(0, samRecord.getReadBases.length)
        .find(x => samRecord.getReferencePositionAtReadPosition(x+1) == vcfRecord.getStart) getOrElse { return None }
    val readBases = samRecord.getReadBases()
    val alleles = vcfRecord.getAlleles.map(x => x.getBaseString)
    val refAllele = alleles.head
    var maxSize = 1
    for (allele <- alleles if allele.size > maxSize) maxSize = allele.size
    val readC = for (t <- readStartPos until readStartPos+maxSize if t < readBases.length) yield readBases(t).toChar
    val allelesInRead = mutable.Set(alleles.filter(readC.mkString.startsWith(_)):_*)
    
    // Removal of insertions that are not really in the cigarstring
    for (allele <- allelesInRead if allele.size > refAllele.size) {
      val refPos = for (t <- refAllele.size until allele.size) yield samRecord.getReferencePositionAtReadPosition(readStartPos + t + 1)
      if (refPos.exists(_ > 0)) allelesInRead -= allele
    }
    
    // Removal of deletions that are not really in the cigarstring
    for (allele <- allelesInRead) {
      val posAfterAllele = samRecord.getReferencePositionAtReadPosition(readStartPos + allele.size + 1)
      if (!(posAfterAllele == vcfRecord.getStart + refAllele.size)) allelesInRead -= allele
    }
    
    //if (allelesInRead.size > 1) allelesInRead -= refAllele
    if (allelesInRead.isEmpty) return None
    else if (allelesInRead.size == 1) return Some(allelesInRead.head)
    else {
      logger.warn("vcfRecord: " + vcfRecord)
      logger.warn("samRecord: " + samRecord)
      logger.warn("Found multiple options: " + allelesInRead.toString)
      logger.warn("Read skipped, please report this")
      return None
    }
  }
}