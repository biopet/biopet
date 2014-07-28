package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.Sha1sum
import nl.lumc.sasc.biopet.function.fastq.Cutadapt
import nl.lumc.sasc.biopet.function.fastq.Fastqc
import nl.lumc.sasc.biopet.function.fastq.Sickle
import nl.lumc.sasc.biopet.pipelines.flexiprep.scripts.FastqSync
import nl.lumc.sasc.biopet.pipelines.flexiprep.scripts.Seqstat
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File
import argonaut._, Argonaut._
import scalaz._, Scalaz._

class FlexiprepSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc="deps")
  var deps: List[File] = Nil
  
  @Output(doc = "Summary output", required=true)
  var out: File = _
  
  class Chunk {
    var seqstatR1: Seqstat = _
    var seqstatR2: Seqstat = _
    var seqstatR1after: Seqstat = _
    var seqstatR2after: Seqstat = _
    
    var cutadaptR1: Cutadapt = _
    var cutadaptR2: Cutadapt = _

    var fastqSync: FastqSync = _

    var sickle: Sickle = _
  }
  
  var chunks: Map[String, Chunk] = Map()
  
  def addSeqstat(seqstat:Seqstat, R2:Boolean = false, after:Boolean = false, chunk:String=""): Seqstat = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    if (!R2 && !after) chunks(chunk).seqstatR1 = seqstat
    else if (!R2 && after) chunks(chunk).seqstatR1after = seqstat
    else if (R2 && !after) chunks(chunk).seqstatR2 = seqstat
    else if (R2 && after) chunks(chunk).seqstatR2after = seqstat
    deps ::= seqstat.out
    return seqstat
  }
  
  def addCutadapt(cutadapt:Cutadapt, R2:Boolean = false, chunk:String=""): Cutadapt = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    if (!R2) chunks(chunk).cutadaptR1 = cutadapt
    else chunks(chunk).cutadaptR2 = cutadapt
    //TODO: stats file of cutadapt
    return cutadapt
  }
  
  
  
  var sha1R1: Sha1sum = _
  var sha1R2: Sha1sum = _
  var sha1R1after: Sha1sum = _
  var sha1R2after: Sha1sum = _
  
  var fastqcR1: Fastqc = _
  var fastqcR2: Fastqc = _
  var fastqcR1after: Fastqc = _
  var fastqcR2after: Fastqc = _
  
  var flexiprep: Flexiprep = root.asInstanceOf[Flexiprep]
  
  var clipping = true
  var trimming = true
  var paired = true
  
  override def run {
    clipping = !flexiprep.skipClip
    trimming = !flexiprep.skipTrim
    paired = flexiprep.paired
    
  }
  
  def seqstatSummary(): Json = {
    //TODO
    return null
  }
  def sha1Summary(): Json = {
    //TODO
    return null
  }
  def fastqcSummary(): Json  = {
    //TODO
    return null
  }
  def clipstatSummary(): Json = {
    //TODO
    return null
  }
  def syncstatSummary(): Json = {
    //TODO
    return null
  }
  def trimstatSummary(): Json = {
    //TODO
    return null
  }
}