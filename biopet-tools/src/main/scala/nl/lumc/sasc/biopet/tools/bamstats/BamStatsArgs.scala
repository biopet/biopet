package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File

import nl.lumc.sasc.biopet.tools.bamstats.BamStats.commandName
import nl.lumc.sasc.biopet.utils.AbstractOptParser

case class BamStatsArgs(outputDir: File = null,
                bamFile: File = null,
                referenceFasta: Option[File] = None,
                binSize: Int = 10000,
                threadBinSize: Int = 1000000,
                tsvOutputs: Boolean = false)

class BamStatsOptParser extends AbstractOptParser[BamStatsArgs](commandName) {
  opt[File]('R', "reference") valueName "<file>" action { (x, c) =>
    c.copy(referenceFasta = Some(x))
  } text "Fasta file of reference"
  opt[File]('o', "outputDir") required () valueName "<directory>" action { (x, c) =>
    c.copy(outputDir = x)
  } text "Output directory"
  opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
    c.copy(bamFile = x)
  } text "Input bam file"
  opt[Int]("binSize") valueName "<int>" action { (x, c) =>
    c.copy(binSize = x)
  } text "Bin size of stats (beta)"
  opt[Int]("threadBinSize") valueName "<int>" action { (x, c) =>
    c.copy(threadBinSize = x)
  } text "Size of region per thread"
  opt[Unit]("tsvOutputs") action { (_, c) =>
    c.copy(tsvOutputs = true)
  } text "Also output tsv files, default there is only a json"
}
