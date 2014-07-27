package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.fastq.Fastqc
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File
import argonaut._, Argonaut._
import scalaz._, Scalaz._

class FlexiprepSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "Seq stat files R1", required=true)
  var seqstatR1: List[File] = Nil
  
  @Input(doc = "Seq stat files R2", required=false)
  var seqstatR2: List[File] = Nil
  
  @Input(doc = "Seq stat files R1 after", required=false)
  var seqstatR1after: List[File] = Nil
  
  @Input(doc = "Seq stat files R2 after", required=false)
  var seqstatR2after: List[File] = Nil
  
  @Input(doc = "sha1 files R1", required=true)
  var sha1R1: File = _
  
  @Input(doc = "sha1 files R2", required=false)
  var sha1R2: File = _
  
  @Input(doc = "sha1 files R1 after", required=false)
  var sha1R1after: File = _
  
  @Input(doc = "sha1 files R2 after", required=false)
  var sha1R2after: File = _
  
  @Input(doc = "Clip stat files R1", required=false)
  var clipstatR1: List[File] = Nil
  
  @Input(doc = "Clip stat files R2", required=false)
  var clipstatR2: List[File] = Nil
  
  @Input(doc = "Trim stat files", required=false)
  var trimstats: List[File] = Nil
  
  @Input(doc = "Sync stat files", required=false)
  var syncstats: List[File] = Nil
  
  @Output(doc = "Summary output", required=true)
  var out: File = _
  
  @Argument(doc = "fastqc files R1", required=true)
  var fastqcR1: Fastqc = _
  
  @Argument(doc = "fastqc files R2", required=false)
  var fastqcR2: Fastqc = _
  
  @Argument(doc = "fastqc files R1 after", required=false)
  var fastqcR1after: Fastqc = _
  
  @Argument(doc = "fastqc files R2 after", required=false)
  var fastqcR2after: Fastqc = _
  
  @Argument(doc = "Flexiprep Pipeline", required=true)
  var flexiprep: Flexiprep = _
  
  var clipping = true
  var trimming = true
  var paired = true
  
  override def run {
    clipping = !flexiprep.skipClip
    trimming = !flexiprep.skipTrim
    paired = flexiprep.paired
    
    fastqcSummary(fastqcR1)
    if (paired) fastqcSummary(fastqcR2)
    
    sha1Summary(sha1R1)
    if (paired) sha1Summary(sha1R2)
    
    seqstatSummary(seqstatR1)
    if (paired) seqstatSummary(seqstatR2)
    
    if (clipping) {
      clipstatSummary(clipstatR1)
      if (paired) {
        clipstatSummary(clipstatR2)
        syncstatSummary(syncstats)
      }
    }
    
    if (trimming) trimstatSummary(trimstats)
    
    if (clipping || trimming) {
      fastqcSummary(fastqcR1after)
      if (paired) fastqcSummary(fastqcR2after)
      
      sha1Summary(sha1R1after)
      if (paired) sha1Summary(sha1R2after)
      
      seqstatSummary(seqstatR1after)
      if (paired) seqstatSummary(seqstatR2after)
    }
  }
  
  def seqstatSummary(files: List[File]): Json = {
    //TODO
    return null
  }
  def sha1Summary(file:File): Json = {
    //TODO
    return null
  }
  def fastqcSummary(fastqc:Fastqc): Json  = {
    //TODO
    return null
  }
  def clipstatSummary(files: List[File]): Json = {
    //TODO
    return null
  }
  def syncstatSummary(files: List[File]): Json = {
    //TODO
    return null
  }
  def trimstatSummary(files: List[File]): Json = {
    //TODO
    return null
  }
}