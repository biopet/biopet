package nl.lumc.sasc.biopet.function.bedtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.sting.commandline.{Input, Output, Argument}
import java.io.File

class BedtoolsCoverage(val root:Configurable) extends Bedtools {
  @Input(doc="Input file (bed/gff/vcf/bam)")
  var input: File = _
  
  @Input(doc="Intersect file (bed/gff/vcf)")
  var intersectFile: File = _
  
  @Output(doc="output File")
  var output: File = _
  
  @Argument(doc="dept", required=false)
  var depth: Boolean = false
  
  var inputTag = "-a"
  
  override def beforeCmd {
    if (input.getName.endsWith(".bam")) inputTag = "-abam"
  }
  
  def cmdLine = required(executeble) + required("coverage") + 
    required(inputTag, input) + 
    required("-b", intersectFile) + 
    conditional(depth, "-d") + 
    " > " + required(output)
}

object BedtoolsCoverage {
  def apply(root:Configurable, input:File, intersect:File, output:File, 
            depth:Boolean = true) : BedtoolsCoverage = {
    val bedtoolsCoverage = new BedtoolsCoverage(root)
    bedtoolsCoverage.input = input
    bedtoolsCoverage.intersectFile = intersect
    bedtoolsCoverage.output = output
    bedtoolsCoverage.depth = depth
    return bedtoolsCoverage
  }
}