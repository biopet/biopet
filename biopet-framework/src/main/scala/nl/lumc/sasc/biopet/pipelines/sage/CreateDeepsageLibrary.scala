package nl.lumc.sasc.biopet.pipelines.sage

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.biojava3.core.sequence.DNASequence
import org.biojava3.core.sequence.io.FastaReaderHelper
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.collection.SortedMap
import scala.collection.mutable.{Map, Set}
import scala.collection.JavaConversions._

class CreateDeepsageLibrary(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fasta", shortName = "input", required = true)
  var input: File = _
  
  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _
  
  @Output(doc = "Output no tags", shortName = "noTagsOutput", required = false)
  var noTagsOutput: File = _
  
  @Output(doc = "Output no anti tags library", shortName = "noAntiTagsOutput", required = false)
  var noAntiTagsOutput: File = _
  
  @Output(doc = "Output file all genes", shortName = "allGenes", required = false)
  var allGenesOutput: File = _
  
  var tag: String = config("tag", default = "CATG")
  var length: Option[Int] = config("length", default = 17)
  
  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)
    
  override def commandLine = super.commandLine + 
    required("-I", input) + 
    optional("-tag", tag) + 
    optional("-length", length) +
    optional("-notag", noTagsOutput) +
    optional("-noantitag", noAntiTagsOutput) +
    required("-o", output)
}

object CreateDeepsageLibrary {
  var tag = "CATG"
  var length = 17
  var input: File = _
  var noTagsOutput: File = _
  var noAntiTagsOutput: File = _
  var allGenesOutput: File = _
  var output: File = _
  lazy val tagRegex = (tag + "[CATG]{" + length + "}").r
  var geneRegex = """ENSG[0-9]{11}""".r
  
  val tagGenesMap: Map[String, TagGenes] = Map()
  
  val allGenes: Set[String] = Set()
  val tagGenes: Set[String] = Set()
  val antiTagGenes: Set[String] = Set()
  
  class TagGenes {
    val firstTag: Set[String] = Set()
    val allTags:Set[String] = Set()
    val firstAntiTag: Set[String] = Set()
    val allAntiTags:Set[String] = Set()
  }
  class TagResult( val firstTag: String, val allTags:List[String], val firstAntiTag: String, val allAntiTags:List[String] )
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    for (t <- 0 until args.size) {
      args(t) match {
        case "-I" => input = new File(args(t+1))
        case "-tag" => tag = args(t+1)
        case "-length" => length = args(t+1).toInt
        case "-o" => output = new File(args(t+1))
        case "-notag" => noTagsOutput = new File(args(t+1))
        case "-noantitag" => noAntiTagsOutput = new File(args(t+1))
        case "-allgenes" => allGenesOutput = new File(args(t+1))
        case _ =>
      }
    }
    if (input == null || !input.exists) throw new IllegalStateException("Input file not found, use -I")
    if (output == null) throw new IllegalStateException("Output file not found, use -o")
    
    var count = 0
    System.err.println("Reading fasta file")
    val reader = FastaReaderHelper.readFastaDNASequence(input)
    System.err.println("Finding tags")
    for ((name, seq) <- reader) {
      getTags(name, seq)
      count += 1
      if (count % 10000 == 0) System.err.println(count + " transcripts done")
    }
    System.err.println(count + " transcripts done")
    
    System.err.println("Start sorting tags")
    val tagGenesMapSorted:SortedMap[String, TagGenes] = SortedMap(tagGenesMap.toArray:_*)
    
    System.err.println("Writting output files")
    val writer = new PrintWriter(output)
    writer.println("#tag\tfirstTag\tAllTags\tFirstAntiTag\tAllAntiTags")
    for ((tag, genes) <- tagGenesMapSorted) {
      val line = tag + "\t" + genes.firstTag.mkString(",") +
                       "\t" + genes.allTags.mkString(",") +
                       "\t" + genes.firstAntiTag.mkString(",") +
                       "\t" + genes.allAntiTags.mkString(",")
      writer.println(line)
    }
    writer.close()
    
    if (noTagsOutput != null) {
      val writer = new PrintWriter(noTagsOutput)
      for (gene <- allGenes if !tagGenes.contains(gene)) {
        writer.println(gene)
      }
      writer.close
    }
    
    if (noAntiTagsOutput != null) {
      val writer = new PrintWriter(noAntiTagsOutput)
      for (gene <- allGenes if !antiTagGenes.contains(gene)) {
        writer.println(gene)
      }
      writer.close
    }
    
    if (allGenesOutput != null) {
      val writer = new PrintWriter(allGenesOutput)
      for (gene <- allGenes) {
        writer.println(gene)
      }
      writer.close
    }
  }
    
  def addTagresultToTaglib(name:String, tagResult:TagResult) {
    val id = name.split(" ").head //.stripPrefix("hg19_ensGene_")
    val geneID = geneRegex.findFirstIn(name).getOrElse("unknown_gene")
    allGenes.add(geneID)
    
    if (tagResult.firstTag != null) {
      if (!tagGenesMap.contains(tagResult.firstTag)) tagGenesMap += (tagResult.firstTag -> new TagGenes)
      tagGenesMap(tagResult.firstTag).firstTag.add(geneID)
      tagGenes.add(geneID)
    }
    
    for (tag <- tagResult.allTags) {
      if (!tagGenesMap.contains(tag)) tagGenesMap += (tag -> new TagGenes)
      tagGenesMap(tag).allTags.add(geneID)
    }
    
    if (tagResult.firstAntiTag != null) {
      if (!tagGenesMap.contains(tagResult.firstAntiTag)) tagGenesMap += (tagResult.firstAntiTag -> new TagGenes)
      tagGenesMap(tagResult.firstAntiTag).firstAntiTag.add(geneID)
      antiTagGenes.add(geneID)
    }
    
    for (tag <- tagResult.allAntiTags) {
      if (!tagGenesMap.contains(tag)) tagGenesMap += (tag -> new TagGenes)
      tagGenesMap(tag).allAntiTags.add(geneID)
    }
  }
  
  def getTags(name:String, seq: DNASequence): TagResult = {
    val allTags: List[String] = for (tag <- tagRegex.findAllMatchIn(seq.getSequenceAsString).toList) yield tag.toString
    val firstTag = if (allTags.isEmpty) null else allTags.last
    val allAntiTags: List[String] = for (tag <- tagRegex.findAllMatchIn(seq.getReverseComplement.getSequenceAsString).toList) yield tag.toString
    val firstAntiTag = if (allAntiTags.isEmpty) null else  allAntiTags.head
    val result = new TagResult(firstTag, allTags, firstAntiTag, allAntiTags)
    
    addTagresultToTaglib(name, result)
    
    return result
  }
}
