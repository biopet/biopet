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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ Md5sum, Seqstat }
import nl.lumc.sasc.biopet.tools.FastqSync
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

  var md5R1: Md5sum = _
  var md5R2: Md5sum = _
  var md5R1after: Md5sum = _
  var md5R2after: Md5sum = _

  var fastqcR1: Fastqc = _
  var fastqcR2: Fastqc = _
  var fastqcR1after: Fastqc = _
  var fastqcR2after: Fastqc = _

  var flexiprep: Flexiprep = if (root.isInstanceOf[Flexiprep]) root.asInstanceOf[Flexiprep] else {
    throw new IllegalStateException("Root is no instance of Flexiprep")
  }

  var resources: Map[String, Json] = Map()

  def addFastqc(fastqc: Fastqc, R2: Boolean = false, after: Boolean = false): Fastqc = {
    if (!R2 && !after) this.fastqcR1 = fastqc
    else if (!R2 && after) this.fastqcR1after = fastqc
    else if (R2 && !after) this.fastqcR2 = fastqc
    else if (R2 && after) this.fastqcR2after = fastqc
    deps ::= fastqc.output
    return fastqc
  }

  def addMd5sum(md5sum: Md5sum, R2: Boolean = false, after: Boolean = false): Md5sum = {
    if (!R2 && !after) this.md5R1 = md5sum
    else if (!R2 && after) this.md5R1after = md5sum
    else if (R2 && !after) this.md5R2 = md5sum
    else if (R2 && after) this.md5R2after = md5sum
    deps ::= md5sum.output
    return md5sum
  }

  def addSeqstat(seqstat: Seqstat, R2: Boolean = false, after: Boolean = false, chunk: String = ""): Seqstat = {
    if (!chunks.contains(chunk)) chunks += (chunk -> new Chunk)
    if (!R2 && !after) chunks(chunk).seqstatR1 = seqstat
    else if (!R2 && after) chunks(chunk).seqstatR1after = seqstat
    else if (R2 && !after) chunks(chunk).seqstatR2 = seqstat
    else if (R2 && after) chunks(chunk).seqstatR2after = seqstat
    deps ::= seqstat.output
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
    deps ::= fastqSync.outputStats
    fastqSync
  }
  // format: OFF
  override def run {
    logger.debug("Start")
    md5Summary()
    val summary = 
      ("samples" := ( flexiprep.sampleId :=
        ("libraries" := ( flexiprep.libId := (
          ("flexiprep" := (
            ("clipping" := !flexiprep.skipClip) ->:
            ("trimming" := !flexiprep.skipTrim) ->:
            ("paired" := flexiprep.paired) ->:
            jEmptyObject)) ->:
          ("stats" := (
            ("fastq" := seqstatSummary) ->:
            ("clipping" :=? clipstatSummary) ->?:
            ("trimming" :=? trimstatSummary) ->?:
            jEmptyObject)) ->:
          ("resources" := (("raw_R1" := getResources(fastqcR1, md5R1)) ->:
            ("raw_R2" :?= getResources(fastqcR2, md5R2)) ->?:
            ("proc_R1" :?= getResources(fastqcR1after, md5R1after)) ->?:
            ("proc_R2" :?= getResources(fastqcR2after, md5R2after)) ->?:
            jEmptyObject)) ->:
          jEmptyObject ))->: jEmptyObject)->: jEmptyObject)->: jEmptyObject) ->: jEmptyObject
    // format: ON
    val summeryText = summary.spaces2
    logger.debug("\n" + summeryText)
    val writer = new PrintWriter(out)
    writer.write(summeryText)
    writer.close()
    logger.debug("Stop")
  }

  def seqstatSummary(): Option[Json] = {
    val R1_chunks = for ((key, value) <- chunks) yield value.seqstatR1.getSummary
    val R1: Json = Seqstat.mergeSummaries(R1_chunks.toList)

    val R2: Option[Json] = if (!flexiprep.paired) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR2.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR2.getSummary
      Option(Seqstat.mergeSummaries(s.toList))
    }
    val R1_proc: Option[Json] = if (flexiprep.skipClip && flexiprep.skipTrim) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR1after.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR1after.getSummary
      Option(Seqstat.mergeSummaries(s.toList))
    }
    val R2_proc: Option[Json] = if (!flexiprep.paired || (flexiprep.skipClip && flexiprep.skipTrim)) None
    else if (chunks.size == 1) Option(chunks.head._2.seqstatR2after.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.seqstatR2after.getSummary
      Option(Seqstat.mergeSummaries(s.toList))
    }
    return Option(("R1_raw" := R1) ->:
      ("R2_raw" :=? R2) ->?:
      ("R1_proc" :=? R1_proc) ->?:
      ("R2_proc" :=? R2_proc) ->?:
      jEmptyObject)
  }

  def md5Summary() {
    val R1_raw = md5Summary(md5R1)
    val R2_raw = md5Summary(md5R2)
    val R1_proc = md5Summary(md5R1after)
    val R2_proc = md5Summary(md5R2after)

    if (!R1_raw.isEmpty) resources += ("fastq_R1_raw" -> R1_raw.get)
    if (!R2_raw.isEmpty) resources += ("fastq_R2_raw" -> R2_raw.get)
    if (!R1_proc.isEmpty) resources += ("fastq_R1_proc" -> R1_proc.get)
    if (!R2_proc.isEmpty) resources += ("fastq_R2_proc" -> R2_proc.get)
  }

  def md5Summary(md5sum: Md5sum): Option[Json] = {
    if (md5sum == null) return None
    else return Option(md5sum.getSummary)
  }

  def getResources(fastqc: Fastqc, md5sum: Md5sum): Option[Json] = {
    if (fastqc == null || md5sum == null) return None
    val fastqcSum = fastqcSummary(fastqc).get
    return Option(("fastq" := md5Summary(md5sum)) ->: fastqcSum)
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
      Cutadapt.mergeSummaries(s.toList)
    }
    val R2: Option[Json] = if (!flexiprep.paired) None
    else if (chunks.size == 1) Option(chunks.head._2.cutadaptR2.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.cutadaptR2.getSummary
      Option(Cutadapt.mergeSummaries(s.toList))
    }
    return Option(("R1" := R1) ->:
      ("R2" :=? R2) ->?:
      ("fastq_sync" :=? syncstatSummary) ->?:
      jEmptyObject)
  }

  def syncstatSummary(): Option[Json] =
    if (flexiprep.skipClip || !flexiprep.paired)
      None
    else {
      val s = for ((key, value) <- chunks) yield value.fastqSync.summary
      Option(FastqSync.mergeSummaries(s.toList))
    }

  def trimstatSummary(): Option[Json] = {
    if (flexiprep.skipTrim) return None
    if (chunks.size == 1) return Option(chunks.head._2.sickle.getSummary)
    else {
      val s = for ((key, value) <- chunks) yield value.sickle.getSummary
      return Option(Sickle.mergeSummaries(s.toList))
    }
  }
}
