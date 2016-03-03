import java.io.File

import nl.lumc.sasc.biopet.extensions.tools.VcfFilter
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}


/**
  * Created by ahbbollen on 2-3-16.
  */
class VcfFilterTest extends TestNGSuite with Matchers {

  def cmd(s: String) = {
    s.replace("'", "").replace("  ", " ").trim
  }

  def createFilterer = {
    val filterer = new VcfFilter(null)

    val iVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    val oVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    iVcf.deleteOnExit()
    oVcf.deleteOnExit()
    filterer.inputVcf = iVcf
    filterer.outputVcf = oVcf

    filterer
  }

  @DataProvider(name = "intArguments")
  def intArguments = {
    Array(
      Array("minSampleDepth", Some(50)),
      Array("minTotalDepth", Some(50)),
      Array("minAlternateDepth", Some(50)),
      Array("minSamplesPass", Some(5)),
      Array("minGenomeQuality", Some(100))
    )
  }

  @DataProvider(name = "stringArguments")
  def stringArguments  = {
    Array(
      Array("resToDom", Some("dummy")),
      Array("trioCompound", Some("dummy")),
      Array("deNovoInSample", Some("dummy")),
      Array("deNovoTrio", Some("dummy")),
      Array("trioLossOfHet", Some("dummy"))
    )
  }

  @DataProvider(name = "listArguments")
  def listArguments = {
    Array(
      Array("mustHaveVariant", List("sample1", "sample2")),
      Array("calledIn", List("sample1", "sample2")),
      Array("mustHaveGenotype", List("sample1:HET", "sample2:HET")),
      Array("diffGenotype", List("sample1:sample2", "sample2:sample3")),
      Array("filterHetVarToHomVar", List("sample1:sample2", "sample2:sample3")),
      Array("id", List("rs01", "rs02"))
    )
  }

  @DataProvider(name = "fileArguments")
  def fileArguments = {
    val invFile = File.createTempFile("vcfFilter", ".vcf")
    val idFile = File.createTempFile("vcfFilter", ".txt")
    invFile.deleteOnExit()
    idFile.deleteOnExit()
    Array(
      Array("invertedOutputVcf", Some(invFile)),
      Array("idFile", Some(idFile))
    )
  }

  @Test(dataProvider = "intArguments")
  def testIntArguments(attr: String, value: Option[Int]) = {
    val filterer = createFilterer

    attr match {
      case "minSampleDepth" => filterer.minSampleDepth = value
      case "minTotalDepth" => filterer.minTotalDepth = value
      case "minAlternateDepth" => filterer.minAlternateDepth = value
      case "minSamplesPass" => filterer.minSamplesPass = value
      case "minGenomeQuality" => filterer.minGenomeQuality = value
      case _ => throw new IllegalArgumentException
    }

    val cmdString = "--" + attr + " " + (value match {
      case Some(v) => v.toString
      case _ => throw new IllegalArgumentException
    })

    cmd(filterer.cmdLine).endsWith(cmdString) shouldBe true
  }

  @Test(dataProvider = "stringArguments")
  def testStringArguments(attr: String, value: Option[String]) = {
    val filterer = createFilterer

    attr match {
      case "resToDom" => filterer.resToDom = value
      case "trioCompound" => filterer.trioCompound = value
      case "deNovoInSample" => filterer.deNovoInSample = value
      case "deNovoTrio" => filterer.deNovoTrio = value
      case "trioLossOfHet" =>  filterer.trioLossOfHet = value
      case _ => throw new IllegalArgumentException
    }

    val cmdString = "--" + attr + " " + (value match {
      case Some(v) => v
      case _ => throw new IllegalArgumentException
    })

    cmd(filterer.cmdLine).endsWith(cmdString) shouldBe true
  }

  @Test(dataProvider = "listArguments")
  def testListArguments(attr: String, value: List[String]) = {
    val filterer = createFilterer

    attr match {
      case "mustHaveVariant" => filterer.mustHaveVariant = value
      case "mustHaveGenotype" => filterer.mustHaveGenotype = value
      case "calledIn" => filterer.calledIn = value
      case "diffGenotype" => filterer.diffGenotype = value
      case "filterHetVarToHomVar" => filterer.filterHetVarToHomVar = value
      case "id" => filterer.id = value
      case _ => throw new IllegalArgumentException
    }

    val cmdString = value.foldLeft("")((acc, x) => acc + "--" + attr + " " + x + " ").stripSuffix(" ")
    cmd(filterer.cmdLine).endsWith(cmdString) shouldBe true
  }

  @Test(dataProvider = "fileArguments")
  def testFileArguments(attr: String, value: Option[File])  = {
    val filterer = createFilterer

    attr match {
      case "invertedOutputVcf" => filterer.invertedOutputVcf = value
      case "idFile" => filterer.idFile = value
      case _ => throw new IllegalArgumentException
    }

    val cmdString = "--" + attr + " " + (value match {
      case Some(v) => v.getAbsolutePath
      case _ => throw new IllegalArgumentException
    })

    cmd(filterer.cmdLine).endsWith(cmdString) shouldBe true
  }

  /**
    * The following two tests are for arguments with a so-far unique type
    */
  @Test
  def testMinQual = {
    val filterer = createFilterer
    filterer.minQualScore = Option(50)

    cmd(filterer.cmdLine).endsWith("--minQualScore 50.0") shouldBe true
  }

  @Test
  def testFilterRefCalls = {
    val filterer = createFilterer
    filterer.filterRefCalls = true

    cmd(filterer.cmdLine).endsWith("--filterRefCalls") shouldBe true
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

}
