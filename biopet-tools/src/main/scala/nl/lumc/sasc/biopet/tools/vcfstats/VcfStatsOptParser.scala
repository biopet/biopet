package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.File

import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats.{
  defaultGenotypeFields,
  defaultInfoFields,
  generalWiggleOptions,
  genotypeWiggleOptions
}
import nl.lumc.sasc.biopet.utils.AbstractOptParser

/**
  * Created by pjvanthof on 18/07/2017.
  */
class VcfStatsOptParser(cmdName: String) extends AbstractOptParser[VcfStatsArgs](cmdName) {
  opt[File]('I', "inputFile") required () unbounded () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(inputFile = x.getAbsoluteFile)
  } validate { x =>
    if (x.exists) success else failure("Input VCF required")
  } text "Input VCF file (required)"
  opt[File]('R', "referenceFile") required () unbounded () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(referenceFile = x)
  } validate { x =>
    if (x.exists) success else failure("Reference file required")
  } text "Fasta reference which was used to call input VCF (required)"
  opt[File]('o', "outputDir") required () unbounded () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(outputDir = x)
  } validate { x =>
    if (x == null) failure("Valid output directory required")
    else if (x.exists) success
    else failure(s"Output directory does not exist: $x")
  } text "Path to directory for output (required)"
  opt[File]('i', "intervals") unbounded () valueName "<file>" action { (x, c) =>
    c.copy(intervals = Some(x))
  } text "Path to interval (BED) file (optional)"
  opt[String]("infoTag") unbounded () valueName "<tag>" action { (x, c) =>
    c.copy(infoTags = x :: c.infoTags)
  } text s"Summarize these info tags. Default is (${defaultInfoFields.mkString(", ")})"
  opt[String]("genotypeTag") unbounded () valueName "<tag>" action { (x, c) =>
    c.copy(genotypeTags = x :: c.genotypeTags)
  } text s"Summarize these genotype tags. Default is (${defaultGenotypeFields.mkString(", ")})"
  opt[Unit]("allInfoTags") unbounded () action { (_, c) =>
    c.copy(allInfoTags = true)
  } text "Summarize all info tags. Default false"
  opt[Unit]("allGenotypeTags") unbounded () action { (_, c) =>
    c.copy(allGenotypeTags = true)
  } text "Summarize all genotype tags. Default false"
  opt[Int]("binSize") unbounded () action { (x, c) =>
    c.copy(binSize = x)
  } text "Binsize in estimated base pairs"
  opt[Unit]("writeBinStats") unbounded () action { (_, c) =>
    c.copy(writeBinStats = true)
  } text "Write bin statistics. Default False"
  opt[String]("generalWiggle") unbounded () action { (x, c) =>
    c.copy(generalWiggle = x :: c.generalWiggle, writeBinStats = true)
  } validate { x =>
    if (generalWiggleOptions.contains(x)) success else failure(s"""Nonexistent field $x""")
  } text s"""Create a wiggle track with bin size <binSize> for any of the following statistics:
            |${generalWiggleOptions.mkString(", ")}""".stripMargin
  opt[String]("genotypeWiggle") unbounded () action { (x, c) =>
    c.copy(genotypeWiggle = x :: c.genotypeWiggle, writeBinStats = true)
  } validate { x =>
    if (genotypeWiggleOptions.contains(x)) success else failure(s"""Non-existent field $x""")
  } text s"""Create a wiggle track with bin size <binSize> for any of the following genotype fields:
            |${genotypeWiggleOptions.mkString(", ")}""".stripMargin
  opt[Int]('t', "localThreads") unbounded () action { (x, c) =>
    c.copy(localThreads = x)
  } text s"Number of local threads to use"
  opt[String]("sparkMaster") unbounded () action { (x, c) =>
    c.copy(sparkMaster = Some(x))
  } text s"Spark master to use"
}
