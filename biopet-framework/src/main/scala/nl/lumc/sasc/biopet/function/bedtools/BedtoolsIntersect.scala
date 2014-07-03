package nl.lumc.sasc.biopet.function.bedtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.sting.commandline.{Input, Output, Argument}
import java.io.File

class BedtoolsIntersect(val root:Configurable) extends Bedtools {
  @Input(doc="Input file (bed/gff/vcf/bam)")
  var input: File = _
  
  @Input(doc="Intersect file (bed/gff/vcf)")
  var intersectFile: File = _
  
  @Output(doc="output File")
  var output: File = _
  
  @Argument(doc="Min overlap", required=false)
  var minOverlap: Double = config("minoverlap", 1E-9)
  
  @Argument(doc="Only count", required=false)
  var count: Boolean = false
  
  var inputTag = "-a"
  
  override def beforeCmd {
    if (input.getName.endsWith(".bam")) inputTag = "-abam"
  }
  
  def cmdLine = required(executeble) + required("intersect") + 
    required(inputTag, input) + 
    required("-b", intersectFile) + 
    required("-f", minOverlap) + 
    conditional(count, "-c") + 
    " > " + required(output)
}

object BedtoolsIntersect {
  def apply(root:Configurable, input:File, intersect:File, output:File, 
            minOverlap:Double = 0, count:Boolean = false) : BedtoolsIntersect = {
    val bedtoolsIntersect = new BedtoolsIntersect(root)
    bedtoolsIntersect.input = input
    bedtoolsIntersect.intersectFile = intersect
    bedtoolsIntersect.output = output
    if (minOverlap > 0) bedtoolsIntersect.minOverlap = minOverlap
    bedtoolsIntersect.count = count
    return bedtoolsIntersect
  }
}