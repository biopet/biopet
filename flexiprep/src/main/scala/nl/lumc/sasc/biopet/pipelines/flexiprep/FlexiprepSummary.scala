package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Sha1sum
import nl.lumc.sasc.biopet.pipelines.flexiprep.scripts.{ FastqSync, Seqstat }
import nl.lumc.sasc.biopet.pipelines.flexiprep.scripts.Seqstat
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File
import argonaut._, Argonaut._
import scalaz._, Scalaz._

class FlexiprepSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "deps")
  var deps: List[File] = Nil

  @Output(doc = "Summary output", required = true)
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

  var sha1R1: Sha1sum = _
  var sha1R2: Sha1sum = _
  var sha1R1after: Sha1sum = _
  var sha1R2after: Sha1sum = _

  var fastqcR1: Fastqc = _
  var fastqcR2: Fastqc = _
  var fastqcR1after: Fastqc = _
  var fastqcR2after: Fastqc = _

  var flexiprep: Flexiprep = root.asInstanceOf[Flexiprep]

  def addFastqc(fastqc: Fastqc, R2: Boolean = false, after: Boolean = false): Fastqc = {
    if (!R2 && !after) this.fastqcR1 = fastqc
    else if (!R2 && after) this.fastqcR1after = fastqc
    else if (R2 && !after) this.fastqcR2 = fastqc
    else if (R2 && after) this.fastqcR2after = fastqc
    deps ::= fastqc.output
    return fastqc
  }

  def addSha1sum(sha1sum: Sha1sum, R2: Boolean = false, after: Boolean = false): Sha1sum = {
    if (!R2 && !after) this.sha1R1 = sha1sum
    else if (!R2 && after) this.sha1R1after = sha1sum
    else if (R2 && !after) this.sha1R2 = sha1sum
    else if (R2 && after) this.sha1R2after = sha1sum
    deps ::= sha1sum.output
    return sha1sum
  }

  def addSeqstat(seqstat: Seqstat, R2: Boolean = false, after: Boolean = false, chunk: String = ""): Seqstat = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    if (!R2 && !after) chunks(chunk).seqstatR1 = seqstat
    else if (!R2 && after) chunks(chunk).seqstatR1after = seqstat
    else if (R2 && !after) chunks(chunk).seqstatR2 = seqstat
    else if (R2 && after) chunks(chunk).seqstatR2after = seqstat
    deps ::= seqstat.out
    return seqstat
  }

  def addCutadapt(cutadapt: Cutadapt, R2: Boolean = false, chunk: String = ""): Cutadapt = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    if (!R2) chunks(chunk).cutadaptR1 = cutadapt
    else chunks(chunk).cutadaptR2 = cutadapt
    deps ::= cutadapt.stats_output
    return cutadapt
  }

  def addSickle(sickle: Sickle, chunk: String = ""): Sickle = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    chunks(chunk).sickle = sickle
    deps ::= sickle.output_stats
    return sickle
  }

  def addFastqcSync(fastqSync: FastqSync, chunk: String = ""): FastqSync = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    chunks(chunk).fastqSync = fastqSync
    deps ::= fastqSync.output_stats
    return fastqSync
  }
  // format: OFF
  override def run {
    logger.debug("Start")
    val summary = 
      ("flexiprep" := (
        ("clipping" := !flexiprep.skipClip) ->:
        ("trimming" := !flexiprep.skipTrim) ->:
        ("paired" := flexiprep.paired) ->:
        jEmptyObject)) ->:
      ("seqstat" := seqstatSummary) ->:
      ("sha1" := sha1Summary) ->:
      ("fastqc" := fastqcSummary) ->:
      ("clipping" :=? clipstatSummary) ->?:
      ("trimming" :=? trimstatSummary) ->?:
      jEmptyObject
    // format: ON
    logger.debug(summary.spaces2) // TODO: need output writter
    logger.debug("Stop")
  }

  def seqstatSummary(): Option[Json] = {
    val R1: Json = if (chunks.size == 1) chunks.head._2.seqstatR1.getSummary
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR1.getSummary
      Seqstat.mergeSummarys(s.toList)
    }
    val R2: Option[Json] = if (!flexiprep.paired) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR2.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR2.getSummary
      Option(Seqstat.mergeSummarys(s.toList))
    }
    val R1_proc: Option[Json] = if (flexiprep.skipClip && flexiprep.skipTrim) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR1after.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR1after.getSummary
      Option(Seqstat.mergeSummarys(s.toList))
    }
    val R2_proc: Option[Json] = if (!flexiprep.paired && flexiprep.skipClip && flexiprep.skipTrim) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR2after.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR2after.getSummary
      Option(Seqstat.mergeSummarys(s.toList))
    }
    return Option(("R1_raw" := R1) ->:
      ("R2_raw" :=? R2) ->?:
      ("R1_proc" :=? R1_proc) ->?:
      ("R2_proc" :=? R2_proc) ->?:
      jEmptyObject)
  }

  def sha1Summary: Json = {
    return ("R1_raw" := sha1Summary(sha1R1)) ->:
      ("R2_raw" :=? sha1Summary(sha1R2)) ->?:
      ("R1_proc" :=? sha1Summary(sha1R1after)) ->?:
      ("R2_proc" :=? sha1Summary(sha1R2after)) ->?:
      jEmptyObject
  }

  def sha1Summary(sha1sum: Sha1sum): Option[Json] = {
    if (sha1sum == null) return None
    else return Option(sha1sum.getSummary)
  }

  def fastqcSummary: Json = {
    return ("R1_raw" := fastqcSummary(fastqcR1)) ->:
      ("R2_raw" :=? fastqcSummary(fastqcR2)) ->?:
      ("R1_proc" :=? fastqcSummary(fastqcR1after)) ->?:
      ("R2_proc" :=? fastqcSummary(fastqcR2after)) ->?:
      jEmptyObject
  }

  def fastqcSummary(fastqc: Fastqc): Option[Json] = {
    if (fastqc == null) return None
    else return Option(fastqc.getSummary)
  }

  def clipstatSummary(): Option[Json] = {
    if (flexiprep.skipClip) return None
    val R1: Json = if (chunks.size == 1) chunks.head._2.cutadaptR1.getSummary
    else {
      val s = for ((key, value) <- chunks) yield value.cutadaptR1.getSummary
      Cutadapt.mergeSummarys(s.toList)
    }
    val R2: Option[Json] = if (!flexiprep.paired) None
    else if (chunks.size == 1) Option(chunks.head._2.cutadaptR2.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.cutadaptR2.getSummary
      Option(Cutadapt.mergeSummarys(s.toList))
    }
    return Option(("R1" := R1) ->:
      ("R2" :=? R2) ->?:
      ("fastqSync" :=? syncstatSummary) ->?:
      jEmptyObject)
  }

  def syncstatSummary(): Option[Json] = {
    if (flexiprep.skipClip || !flexiprep.paired) return None
    if (chunks.size == 1) return Option(chunks.head._2.sickle.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.fastqSync.getSummary
      return Option(FastqSync.mergeSummarys(s.toList))
    }
  }

  def trimstatSummary(): Option[Json] = {
    if (flexiprep.skipTrim) return None
    if (chunks.size == 1) return Option(chunks.head._2.sickle.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.sickle.getSummary
      return Option(Sickle.mergeSummarys(s.toList))
    }
  }
}
