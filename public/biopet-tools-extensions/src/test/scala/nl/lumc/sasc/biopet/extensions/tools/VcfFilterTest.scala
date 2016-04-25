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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

/**
 * Created by ahbbollen on 2-3-16.
 */
class VcfFilterTest extends TestNGSuite with Matchers {

  def cmd(s: String) = {
    s.replace("'", "").replace("  ", " ").trim
  }

  @Test
  def testBeforeGraph() = {
    val filterer = new VcfFilter(null)
    val iVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    val oVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    iVcf.deleteOnExit()
    oVcf.deleteOnExit()
    filterer.inputVcf = iVcf
    filterer.outputVcf = oVcf

    filterer.beforeGraph()
    filterer.outputVcfIndex.getAbsolutePath shouldBe oVcf.getAbsolutePath + ".tbi"
  }

  @DataProvider(name = "functions")
  def functions = {
    Array(
      () => testCommand(minSampleDepth = Some(2)),
      () => testCommand(minTotalDepth = Some(2)),
      () => testCommand(minAlternateDepth = Some(2)),
      () => testCommand(minSamplesPass = Some(2)),
      () => testCommand(minGenomeQuality = Some(50)),
      () => testCommand(filterRefCalls = true),
      () => testCommand(invertedOutputVcf = Some(File.createTempFile("vcfFilter", ".vcf"))),
      () => testCommand(resToDom = Some("dummy")),
      () => testCommand(trioCompound = Some("dummy")),
      () => testCommand(deNovoInSample = Some("dummy")),
      () => testCommand(deNovoTrio = Some("dummy")),
      () => testCommand(trioLossOfHet = Some("dummy")),
      () => testCommand(mustHaveVariant = List("sample1", "sample2")),
      () => testCommand(calledIn = List("sample1", "sample2")),
      () => testCommand(mustHaveGenotype = List("sample1:HET", "sample2:HET")),
      () => testCommand(diffGenotype = List("sample1:sample2", "sample2:sample3")),
      () => testCommand(minQualScore = Some(50.0)),
      () => testCommand(filterHetVarToHomVar = List("dummy")),
      () => testCommand(id = List("rs01", "rs02")),
      () => testCommand(idFile = Some(File.createTempFile("vcfFilter", ".txt")))
    ).map(Array(_))
  }

  @Test(dataProvider = "functions")
  def executer(function0: Function0[Unit]): Unit = function0()

  protected def testCommand(minSampleDepth: Option[Int] = None,
                            minTotalDepth: Option[Int] = None,
                            minAlternateDepth: Option[Int] = None,
                            minSamplesPass: Option[Int] = None,
                            minGenomeQuality: Option[Int] = None,
                            filterRefCalls: Boolean = false,
                            invertedOutputVcf: Option[File] = None,
                            resToDom: Option[String] = None,
                            trioCompound: Option[String] = None,
                            deNovoInSample: Option[String] = None,
                            deNovoTrio: Option[String] = None,
                            trioLossOfHet: Option[String] = None,
                            mustHaveVariant: List[String] = Nil,
                            calledIn: List[String] = Nil,
                            mustHaveGenotype: List[String] = Nil,
                            diffGenotype: List[String] = Nil,
                            filterHetVarToHomVar: List[String] = Nil,
                            minQualScore: Option[Double] = None,
                            id: List[String] = Nil,
                            idFile: Option[File] = None): Unit = {

    val vcfFilter = new VcfFilter(null)
    vcfFilter.minSampleDepth = minSampleDepth
    vcfFilter.minTotalDepth = minTotalDepth
    vcfFilter.minAlternateDepth = minAlternateDepth
    vcfFilter.minSamplesPass = minSamplesPass
    vcfFilter.minGenomeQuality = minGenomeQuality
    vcfFilter.filterRefCalls = filterRefCalls
    vcfFilter.invertedOutputVcf = invertedOutputVcf
    vcfFilter.resToDom = resToDom
    vcfFilter.trioCompound = trioCompound
    vcfFilter.deNovoInSample = deNovoInSample
    vcfFilter.deNovoTrio = deNovoTrio
    vcfFilter.trioLossOfHet = trioLossOfHet
    vcfFilter.mustHaveVariant = mustHaveVariant
    vcfFilter.calledIn = calledIn
    vcfFilter.mustHaveGenotype = mustHaveGenotype
    vcfFilter.diffGenotype = diffGenotype
    vcfFilter.filterHetVarToHomVar = filterHetVarToHomVar
    vcfFilter.minQualScore = minQualScore
    vcfFilter.id = id
    vcfFilter.idFile = idFile
    val command = cmd(vcfFilter.cmdLine)

    var cmdString: List[String] = Nil
    if (minSampleDepth.isDefined) cmdString = "--minSampleDepth " + minSampleDepth.getOrElse("") :: cmdString

    if (minTotalDepth.isDefined) cmdString = "--minTotalDepth " + minTotalDepth.getOrElse("") :: cmdString

    if (minAlternateDepth.isDefined) cmdString = "--minAlternateDepth " + minAlternateDepth.getOrElse("") :: cmdString

    if (minSamplesPass.isDefined) cmdString = "--minSamplesPass " + minSamplesPass.getOrElse("") :: cmdString

    if (minGenomeQuality.isDefined) cmdString = "--minGenomeQuality " + minGenomeQuality.getOrElse("") :: cmdString

    if (filterRefCalls) cmdString = "--filterRefCalls" :: cmdString

    if (invertedOutputVcf.isDefined) cmdString = "--invertedOutputVcf " + invertedOutputVcf.getOrElse(new File("")).getAbsolutePath :: cmdString

    if (resToDom.isDefined) cmdString = "--resToDom " + resToDom.getOrElse("") :: cmdString

    if (trioCompound.isDefined) cmdString = "--trioCompound " + trioCompound.getOrElse("") :: cmdString

    if (deNovoInSample.isDefined) cmdString = "--deNovoInSample " + deNovoInSample.getOrElse("") :: cmdString

    if (deNovoTrio.isDefined) cmdString = "--deNovoTrio " + deNovoTrio.getOrElse("") :: cmdString

    if (trioLossOfHet.isDefined) cmdString = "--trioLossOfHet " + trioLossOfHet.getOrElse("") :: cmdString

    if (mustHaveVariant.nonEmpty) cmdString = mustHaveVariant.map(x => "--mustHaveVariant " + x) ::: cmdString

    if (calledIn.nonEmpty) cmdString = calledIn.map(x => "--calledIn " + x) ::: cmdString

    if (mustHaveGenotype.nonEmpty) cmdString = mustHaveGenotype.map(x => "--mustHaveGenotype " + x) ::: cmdString

    if (diffGenotype.nonEmpty) cmdString = diffGenotype.map(x => "--diffGenotype " + x) ::: cmdString

    if (filterHetVarToHomVar.nonEmpty) cmdString = filterHetVarToHomVar.map(x => "--filterHetVarToHomVar " + x) ::: cmdString

    if (id.nonEmpty) cmdString = id.map(x => "--id " + x) ::: cmdString

    if (idFile.isDefined) cmdString = "--idFile " + idFile.getOrElse(new File("")).getAbsolutePath :: cmdString

    if (minQualScore.isDefined) cmdString = "--minQualScore " + minQualScore.getOrElse("") :: cmdString

    cmdString.foreach(x => command.contains(x) shouldBe true)
  }

}
