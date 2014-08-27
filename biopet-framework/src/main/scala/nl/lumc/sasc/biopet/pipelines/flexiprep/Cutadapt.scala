package nl.lumc.sasc.biopet.pipelines.flexiprep

import scala.io.Source

import nl.lumc.sasc.biopet.extensions.Ln
import org.broadinstitute.gatk.utils.commandline.{ Input }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import scala.collection.mutable.Map

class Cutadapt(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) {
  @Input(doc = "Fastq contams file", required = false)
  var contams_file: File = _
  
  override def beforeCmd() {
    super.beforeCmd
    getContamsFromFile
  }
  
  override def cmdLine = {
    if (!opt_adapter.isEmpty || !opt_anywhere.isEmpty || !opt_front.isEmpty) {
      analysisName = getClass.getSimpleName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      Ln(this, fastq_input, fastq_output, relative = true).cmd
    }
  }
  
  def getContamsFromFile {
    if (contams_file != null) {
      if (contams_file.exists()) {
        for (line <- Source.fromFile(contams_file).getLines) {
          var s: String = line.substring(line.lastIndexOf("\t") + 1, line.size)
          if (default_clip_mode == "3") opt_adapter += s
          else if (default_clip_mode == "5") opt_front += s
          else if (default_clip_mode == "both") opt_anywhere += s
          else {
            opt_adapter += s
            logger.warn("Option default_clip_mode should be '3', '5' or 'both', falling back to default: '3'")
          }
          logger.info("Adapter: " + s + " found in: " + fastq_input)
        }
      } else logger.warn("File : " + contams_file + " does not exist")
    }
  }
  
  def getSummary: Json = {
    val trimR = """.*Trimmed reads: *(\d*) .*""".r
    val tooShortR = """.*Too short reads: *(\d*) .*""".r
    val tooLongR = """.*Too long reads: *(\d*) .*""".r
    val adapterR = """Adapter '([C|T|A|G]*)'.*trimmed (\d*) times.""".r
    
    var stats: Map[String, Int] = Map("trimmed" -> 0, "tooshort" -> 0, "toolong" -> 0)
    var adapter_stats: Map[String, Int] = Map()
    
    if (stats_output.exists) for (line <- Source.fromFile(stats_output).getLines) {
      line match {
        case trimR(m) => stats += ("trimmed" -> m.toInt)
        case tooShortR(m) => stats += ("tooshort" -> m.toInt)
        case tooLongR(m) => stats += ("toolong" -> m.toInt)
        case adapterR(adapter, count) =>  adapter_stats += (adapter -> count.toInt)
        case _ => 
      }
    }
    return ("num_reads_affected" := stats("trimmed")) ->:
      ("num_reads_discarded_too_short" := stats("tooshort")) ->:
      ("num_reads_discarded_too_long" := stats("toolong")) ->:
      ("adapters" := adapter_stats.toMap) ->:
      jEmptyObject
  }
}

object Cutadapt {
  def apply(root: Configurable, input:File, output:File): Cutadapt = {
    val cutadapt = new Cutadapt(root)
    cutadapt.fastq_input = input
    cutadapt.fastq_output = output
    cutadapt.stats_output = new File(output.getAbsolutePath.substring(0, output.getAbsolutePath.lastIndexOf(".")) + ".stats")
    return cutadapt
  }
  
  def mergeSummaries(jsons: List[Json]): Json = {
    var affected = 0
    var tooShort = 0
    var tooLong = 0
    var adapter_stats: Map[String, Int] = Map()
    
    for (json <- jsons) {
      affected += json.field("num_reads_affected").get.numberOrZero.toInt
      tooShort += json.field("num_reads_discarded_too_short").get.numberOrZero.toInt
      tooLong += json.field("num_reads_discarded_too_long").get.numberOrZero.toInt
      
      val adapters = json.fieldOrEmptyObject("adapters")
      for (key <- adapters.objectFieldsOrEmpty) {
        val number = adapters.field(key).get.numberOrZero.toInt
        if (adapter_stats.contains(key)) adapter_stats(key) += number
        else adapter_stats += (key -> number)
      }
    }
    
    return ("num_reads_affected" := affected) ->:
      ("num_reads_discarded_too_short" := tooShort) ->:
      ("num_reads_discarded_too_long" := tooLong) ->:
      ("adapters" := adapter_stats.toMap) ->:
      jEmptyObject
  }
}
