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
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.variant.variantcontext.{GenotypeType, VariantContext}
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand, VcfUtils}

import scala.collection.JavaConversions._
import scala.io.Source

object VcfFilter extends ToolCommand {

  /** Container class for a trio */
  protected[tools] case class Trio(child: String, father: String, mother: String) {
    def this(arg: String) = {
      this(arg.split(":")(0), arg.split(":")(1), arg.split(":")(2))
    }
  }

  case class BooleanArgs(uniqueOnly: Boolean = false,
                         sharedOnly: Boolean = false,
                         filterRefCalls: Boolean = false,
                         filterNoCalls: Boolean = false)

  case class Args(inputVcf: File = null,
                  outputVcf: File = null,
                  invertedOutputVcf: Option[File] = None,
                  minQualScore: Option[Double] = None,
                  minSampleDepth: Int = -1,
                  minTotalDepth: Int = -1,
                  minAlternateDepth: Int = -1,
                  minSamplesPass: Int = 1,
                  mustHaveVariant: List[String] = Nil,
                  mustNotHaveVariant: List[String] = Nil,
                  calledIn: List[String] = Nil,
                  mustHaveGenotype: List[(String, GenotypeType)] = Nil,
                  uniqueVariantInSample: String = null,
                  resToDom: List[Trio] = Nil,
                  trioCompound: List[Trio] = Nil,
                  deNovoTrio: List[Trio] = Nil,
                  trioLossOfHet: List[Trio] = Nil,
                  booleanArgs: BooleanArgs = BooleanArgs(),
                  diffGenotype: List[(String, String)] = Nil,
                  filterHetVarToHomVar: List[(String, String)] = Nil,
                  iDset: Set[String] = Set(),
                  minGenomeQuality: Int = 0,
                  advancedGroups: List[List[String]] = Nil)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputVcf = x)
    } text "Input vcf file"
    opt[File]('o', "outputVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputVcf = x)
    } text "Output vcf file"
    opt[File]("invertedOutputVcf") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(invertedOutputVcf = Some(x))
    } text "inverted output vcf file"
    opt[Int]("minSampleDepth") unbounded () valueName "<int>" action { (x, c) =>
      c.copy(minSampleDepth = x)
    } text "Min value for DP in genotype fields"
    opt[Int]("minTotalDepth") unbounded () valueName "<int>" action { (x, c) =>
      c.copy(minTotalDepth = x)
    } text "Min value of DP field in INFO fields"
    opt[Int]("minAlternateDepth") unbounded () valueName "<int>" action { (x, c) =>
      c.copy(minAlternateDepth = x)
    } text "Min value of AD field in genotype fields"
    opt[Int]("minSamplesPass") unbounded () valueName "<int>" action { (x, c) =>
      c.copy(minSamplesPass = x)
    } text "Min number off samples to pass --minAlternateDepth, --minBamAlternateDepth and --minSampleDepth"
    opt[String]("resToDom") unbounded () valueName "<child:father:mother>" action { (x, c) =>
      c.copy(resToDom = new Trio(x) :: c.resToDom)
    } text "Only shows variants where child is homozygous and both parants hetrozygous"
    opt[String]("trioCompound") unbounded () valueName "<child:father:mother>" action { (x, c) =>
      c.copy(trioCompound = new Trio(x) :: c.trioCompound)
    } text "Only shows variants where child is a compound variant combined from both parants"
    opt[String]("deNovoInSample") maxOccurs 1 unbounded () valueName "<sample>" action { (x, c) =>
      c.copy(uniqueVariantInSample = x)
    } text "Only show variants that contain unique alleles in complete set for given sample"
    opt[String]("deNovoTrio") unbounded () valueName "<child:father:mother>" action { (x, c) =>
      c.copy(deNovoTrio = new Trio(x) :: c.deNovoTrio)
    } text "Only show variants that are denovo in the trio"
    opt[String]("trioLossOfHet") unbounded () valueName "<child:father:mother>" action { (x, c) =>
      c.copy(trioLossOfHet = new Trio(x) :: c.trioLossOfHet)
    } text "Only show variants where a loss of hetrozygosity is detected"
    opt[String]("mustHaveVariant") unbounded () valueName "<sample>" action { (x, c) =>
      c.copy(mustHaveVariant = x :: c.mustHaveVariant)
    } text "Given sample must have 1 alternative allele"
    opt[String]("mustNotHaveVariant") unbounded () valueName "<sample>" action { (x, c) =>
      c.copy(mustNotHaveVariant = x :: c.mustNotHaveVariant)
    } text "Given sample may not have alternative alleles"
    opt[String]("calledIn") unbounded () valueName "<sample>" action { (x, c) =>
      c.copy(calledIn = x :: c.calledIn)
    } text "Must be called in this sample"
    opt[String]("mustHaveGenotype") unbounded () valueName "<sample:genotype>" action { (x, c) =>
      c.copy(
        mustHaveGenotype = (x.split(":")(0), GenotypeType.valueOf(x.split(":")(1))) :: c.mustHaveGenotype)
    } validate { x =>
      if (x.split(":")
            .length == 2 && GenotypeType.values().map(_.toString).contains(x.split(":")(1)))
        success
      else failure("--mustHaveGenotype should be in this format: sample:genotype")
    } text "Must have genotoype <genotype> for this sample. Genotype can be " + GenotypeType
      .values()
      .mkString(", ")
    opt[String]("diffGenotype") unbounded () valueName "<sample:sample>" action { (x, c) =>
      c.copy(diffGenotype = (x.split(":")(0), x.split(":")(1)) :: c.diffGenotype)
    } validate { x =>
      if (x.split(":").length == 2) success
      else failure("--notSameGenotype should be in this format: sample:sample")
    } text "Given samples must have a different genotype"
    opt[String]("filterHetVarToHomVar") unbounded () valueName "<sample:sample>" action { (x, c) =>
      c.copy(filterHetVarToHomVar = (x.split(":")(0), x.split(":")(1)) :: c.filterHetVarToHomVar)
    } validate { x =>
      if (x.split(":").length == 2) success
      else failure("--filterHetVarToHomVar should be in this format: sample:sample")
    } text "If variants in sample 1 are heterogeneous and alternative alleles are homogeneous in sample 2 variants are filtered"
    opt[Unit]("filterRefCalls") unbounded () action { (_, c) =>
      c.copy(booleanArgs = c.booleanArgs.copy(filterRefCalls = true))
    } text "Filter when there are only ref calls"
    opt[Unit]("filterNoCalls") unbounded () action { (_, c) =>
      c.copy(booleanArgs = c.booleanArgs.copy(filterNoCalls = true))
    } text "Filter when there are only no calls"
    opt[Unit]("uniqueOnly") unbounded () action { (_, c) =>
      c.copy(booleanArgs = c.booleanArgs.copy(uniqueOnly = true))
    } text "Filter when there more then 1 sample have this variant"
    opt[Unit]("sharedOnly") unbounded () action { (_, c) =>
      c.copy(booleanArgs = c.booleanArgs.copy(sharedOnly = true))
    } text "Filter when not all samples have this variant"
    opt[Double]("minQualScore") unbounded () action { (x, c) =>
      c.copy(minQualScore = Some(x))
    } text "Min qual score"
    opt[String]("id") unbounded () action { (x, c) =>
      c.copy(iDset = c.iDset + x)
    } text "Id that may pass the filter"
    opt[File]("idFile") unbounded () action { (x, c) =>
      c.copy(iDset = c.iDset ++ Source.fromFile(x).getLines())
    } text "File that contain list of IDs to get from vcf file"
    opt[Int]("minGenomeQuality") unbounded () action { (x, c) =>
      c.copy(minGenomeQuality = x)
    }
    opt[String]("advancedGroups") unbounded () action { (x, c) =>
      c.copy(advancedGroups = x.split(",").toList :: c.advancedGroups)
    } text "All members of groups sprated with a ','"
  }

  /** @param args the command line arguments */
  def main(args: Array[String]): Unit = {
    logger.info("Start")
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()).getOrElse { throw new IllegalArgumentException }

    val reader = new VCFFileReader(cmdArgs.inputVcf, false)
    val header = reader.getFileHeader
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(cmdArgs.outputVcf)
        .setReferenceDictionary(header.getSequenceDictionary)
        .build)
    writer.writeHeader(header)

    val invertedWriter = cmdArgs.invertedOutputVcf.collect {
      case x =>
        new VariantContextWriterBuilder()
          .setOutputFile(x)
          .setReferenceDictionary(header.getSequenceDictionary)
          .build
    }
    invertedWriter.foreach(_.writeHeader(header))

    var counterTotal = 0
    var counterLeft = 0
    for (record <- reader) {
      if (cmdArgs.minQualScore.forall(minQualscore(record, _)) &&
          (!cmdArgs.booleanArgs.filterRefCalls || hasNonRefCalls(record)) &&
          (!cmdArgs.booleanArgs.filterNoCalls || hasCalls(record)) &&
          (!cmdArgs.booleanArgs.uniqueOnly || hasUniqeSample(record)) &&
          (!cmdArgs.booleanArgs.sharedOnly || allSamplesVariant(record)) &&
          hasMinTotalDepth(record, cmdArgs.minTotalDepth) &&
          hasMinSampleDepth(record, cmdArgs.minSampleDepth, cmdArgs.minSamplesPass) &&
          minAlternateDepth(record, cmdArgs.minAlternateDepth, cmdArgs.minSamplesPass) &&
          minGenomeQuality(record, cmdArgs.minGenomeQuality, cmdArgs.minSamplesPass) &&
          (cmdArgs.mustHaveVariant.isEmpty || mustHaveVariant(record, cmdArgs.mustHaveVariant)) &&
          (cmdArgs.mustNotHaveVariant.isEmpty || mustNotHaveVariant(record,
                                                                    cmdArgs.mustNotHaveVariant)) &&
          calledIn(record, cmdArgs.calledIn) &&
          hasGenotype(record, cmdArgs.mustHaveGenotype) &&
          (cmdArgs.diffGenotype.isEmpty || cmdArgs.diffGenotype.forall(x =>
            notSameGenotype(record, x._1, x._2))) &&
          (
            cmdArgs.filterHetVarToHomVar.isEmpty ||
            cmdArgs.filterHetVarToHomVar.forall(x => filterHetVarToHomVar(record, x._1, x._2))
          ) &&
          uniqueVariantInSample(record, cmdArgs.uniqueVariantInSample) &&
          denovoTrio(record, cmdArgs.deNovoTrio) &&
          denovoTrio(record, cmdArgs.trioLossOfHet, onlyLossHet = true) &&
          resToDom(record, cmdArgs.resToDom) &&
          trioCompound(record, cmdArgs.trioCompound) &&
          advancedGroupFilter(record, cmdArgs.advancedGroups) &&
          (cmdArgs.iDset.isEmpty || inIdSet(record, cmdArgs.iDset))) {
        writer.add(record)
        counterLeft += 1
      } else
        invertedWriter.foreach(_.add(record))
      counterTotal += 1
      if (counterTotal % 100000 == 0)
        logger.info(s"$counterTotal variants processed, $counterLeft passed filter")
    }
    logger.info(s"$counterTotal variants processed, $counterLeft passed filter")
    reader.close()
    writer.close()
    invertedWriter.foreach(_.close())
    logger.info("Done")
  }

  /**
    * Checks if given samples are called
    *
    * @param record VCF record
    * @param samples Samples that need this sample to be called
    * @return false when filters fail
    */
  def calledIn(record: VariantContext, samples: List[String]): Boolean = {
    if (!samples.forall(record.getGenotype(_).isCalled)) false
    else true
  }

  /**
    * Checks if given genotypes for given samples are there
    *
    * @param record VCF record
    * @param samplesGenotypes samples and their associated genotypes to be checked (of format sample:genotype)
    * @return false when filter fails
    */
  def hasGenotype(record: VariantContext,
                  samplesGenotypes: List[(String, GenotypeType)]): Boolean = {
    samplesGenotypes.forall { x =>
      record.getGenotype(x._1).getType == x._2
    }
  }

  /**
    * Checks if record has atleast minQualScore
    *
    * @param record VCF record
    * @param minQualScore Minimal quality score
    * @return false when filters fail
    */
  def minQualscore(record: VariantContext, minQualScore: Double): Boolean = {
    record.getPhredScaledQual >= minQualScore
  }

  /** returns true record contains Non reference genotypes */
  def hasNonRefCalls(record: VariantContext): Boolean = {
    record.getGenotypes.exists(g => !g.isHomRef)
  }

  /** returns true when record has calls */
  def hasCalls(record: VariantContext): Boolean = {
    record.getGenotypes.exists(g => !g.isNoCall)
  }

  /** Checks if there is a variant in only 1 sample */
  def hasUniqeSample(record: VariantContext): Boolean = {
    record.getSampleNames.exists(uniqueVariantInSample(record, _))
  }

  /** Checks if all samples are a variant */
  def allSamplesVariant(record: VariantContext): Boolean = {
    record.getGenotypes.forall(g =>
      !g.isNonInformative && g.getAlleles.exists(a => a.isNonReference && !a.isNoCall))
  }

  /** returns true when DP INFO field is atleast the given value */
  def hasMinTotalDepth(record: VariantContext, minTotalDepth: Int): Boolean = {
    record.getAttributeAsInt("DP", -1) >= minTotalDepth
  }

  /**
    * Checks if DP genotype field have a minimal value
    *
    * @param record VCF record
    * @param minSampleDepth minimal depth
    * @param minSamplesPass Minimal number of samples to pass filter
    * @return true if filter passed
    */
  def hasMinSampleDepth(record: VariantContext,
                        minSampleDepth: Int,
                        minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(genotype => {
      val DP = if (genotype.hasDP) genotype.getDP else -1
      DP >= minSampleDepth
    }) >= minSamplesPass
  }

  /**
    * Checks if non-ref AD genotype field have a minimal value
    *
    * @param record VCF record
    * @param minAlternateDepth minimal depth
    * @param minSamplesPass Minimal number of samples to pass filter
    * @return true if filter passed
    */
  def minAlternateDepth(record: VariantContext,
                        minAlternateDepth: Int,
                        minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(genotype => {
      val AD = if (genotype.hasAD) List(genotype.getAD: _*) else Nil
      if (AD.nonEmpty && minAlternateDepth >= 0) AD.tail.count(_ >= minAlternateDepth) > 0
      else true
    }) >= minSamplesPass
  }

  /**
    * Checks if genome quality field has minimum value
    *
    * @param record VCF record
    * @param minGQ smallest GQ allowed
    * @param minSamplesPass number of samples to consider
    * @return
    */
  def minGenomeQuality(record: VariantContext, minGQ: Int, minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(
      x =>
        if (minGQ == 0) true
        else if (!x.hasGQ) false
        else if (x.getGQ >= minGQ) true
        else false) >= minSamplesPass
  }

  /**
    * Checks if given samples does have a variant hin this record
    *
    * @param record VCF record
    * @param samples List of samples that should have this variant
    * @return true if filter passed
    */
  def mustHaveVariant(record: VariantContext, samples: List[String]): Boolean = {
    samples.foreach { s =>
      if (!record.getSampleNames.toList.contains(s)) {
        throw new IllegalArgumentException(s"Sample name $s does not exist in VCF file")
      }
    }
    !samples
      .map(record.getGenotype)
      .exists(a => a.isHomRef || a.isNoCall || VcfUtils.isCompoundNoCall(a))
  }

  /**
    * Checks if given samples does have a variant hin this record
    *
    * @param record VCF record
    * @param samples List of samples that should have this variant
    * @return true if filter passed
    */
  def mustNotHaveVariant(record: VariantContext, samples: List[String]): Boolean = {
    samples.foreach { s =>
      if (!record.getSampleNames.toList.contains(s)) {
        throw new IllegalArgumentException(s"Sample name $s does not exist in VCF file")
      }
    }
    samples
      .map(record.getGenotype)
      .forall(a => a.isHomRef || a.isNoCall || VcfUtils.isCompoundNoCall(a))
  }

  /** Checks if given samples have the same genotype */
  def notSameGenotype(record: VariantContext, sample1: String, sample2: String): Boolean = {
    val genotype1 = record.getGenotype(sample1)
    val genotype2 = record.getGenotype(sample2)
    if (genotype1.sameGenotype(genotype2)) false
    else true
  }

  /** Checks if sample1 is hetrozygous and if sample2 is homozygous for a alternative allele in sample1 */
  def filterHetVarToHomVar(record: VariantContext, sample1: String, sample2: String): Boolean = {
    val genotype1 = record.getGenotype(sample1)
    val genotype2 = record.getGenotype(sample2)
    if (genotype1.isHet && !genotype1.getAlleles.forall(_.isNonReference)) {
      for (allele <- genotype1.getAlleles if allele.isNonReference) {
        if (genotype2.getAlleles.forall(_.basesMatch(allele))) return false
      }
    }
    true
  }

  /** Checks if given sample have alternative alleles that are unique in the VCF record */
  def uniqueVariantInSample(record: VariantContext, sample: String): Boolean = {
    if (sample == null) return true
    val genotype = record.getGenotype(sample)
    if (genotype.isNoCall) return false
    if (genotype.getAlleles.forall(_.isReference)) return false
    for (allele <- genotype.getAlleles if allele.isNonReference) {
      for (g <- record.getGenotypes if g.getSampleName != sample) {
        if (g.getAlleles.exists(_.basesMatch(allele))) return false
      }
    }
    true
  }

  /** Return true when variant is homozygous in the child and hetrozygous in parants */
  def resToDom(record: VariantContext, trios: List[Trio]): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)

      if (child.isHomVar && child.getAlleles.forall(allele => {
            record.getGenotype(trio.father).countAllele(allele) == 1 &&
            record.getGenotype(trio.mother).countAllele(allele) == 1
          })) return true
    }
    trios.isEmpty
  }

  /** Returns true when variant a compound variant in the child and hetrozygous in parants */
  def trioCompound(record: VariantContext, trios: List[Trio]): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)

      if (child.isHetNonRef && child.getAlleles.forall(allele => {
            record.getGenotype(trio.father).countAllele(allele) >= 1 &&
            record.getGenotype(trio.mother).countAllele(allele) >= 1
          })) return true
    }
    trios.isEmpty
  }

  /** Returns true when child got a deNovo variant */
  def denovoTrio(record: VariantContext,
                 trios: List[Trio],
                 onlyLossHet: Boolean = false): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)
      val father = record.getGenotype(trio.father)
      val mother = record.getGenotype(trio.mother)

      for (allele <- child.getAlleles) {
        val childCount = child.countAllele(allele)
        val fatherCount = father.countAllele(allele)
        val motherCount = mother.countAllele(allele)

        if (onlyLossHet) {
          if (childCount == 2 && ((fatherCount == 2 && motherCount == 0) ||
              (fatherCount == 0 && motherCount == 2))) return true
        } else {
          if (childCount == 1 && fatherCount == 0 && motherCount == 0) return true
          else if (childCount == 2 && (fatherCount == 0 || motherCount == 0)) return true
        }
      }
    }
    trios.isEmpty
  }

  /** Returns true when VCF record contains a ID from the given list */
  def inIdSet(record: VariantContext, idSet: Set[String]): Boolean = {
    record.getID.split(",").exists(idSet.contains)
  }

  /**
    * returns true when for all groups all or none members have a variants,
    * records with partial groups are discarded
    */
  def advancedGroupFilter(record: VariantContext, groups: List[List[String]]): Boolean = {
    val samples = record.getGenotypes
      .map(a => a.getSampleName -> (a.isHomRef || a.isNoCall || VcfUtils.isCompoundNoCall(a)))
      .toMap

    val g: List[Option[Boolean]] = groups.map { group =>
      val total = group.size
      val count = group.count(samples(_))
      if (count == 0) Some(false)
      else if (total == count) Some(true)
      else None
    }

    !g.contains(None)
  }
}
