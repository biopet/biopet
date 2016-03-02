import java.io.File

import nl.lumc.sasc.biopet.extensions.tools.VcfFilter
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test


/**
  * Created by ahbbollen on 2-3-16.
  */
class VcfFilterTest extends TestNGSuite with Matchers {

  def cmd(s: String) = {
    s.replace("'", "").replace("  ", " ").trim
  }

  @Test
  def testVcfFilterCmdLine = {
    val filterer = new VcfFilter(null)

    val iVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    val oVcf = File.createTempFile("vcfFilter", ".vcf.gz")
    iVcf.deleteOnExit()
    oVcf.deleteOnExit()
    filterer.inputVcf = iVcf
    filterer.outputVcf = oVcf

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath}")


    filterer.minSampleDepth = Some(50)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50")

    filterer.minTotalDepth = Some(50)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50")

    filterer.minAlternateDepth = Some(50)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50")

    filterer.minSamplesPass = Some(5)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5")

    filterer.minGenomeQuality = Some(100)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100")

    filterer.filterRefCalls = true

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls")

    val invVcf = File.createTempFile("VcfFilter", ".vcf.gz")
    invVcf.deleteOnExit()
    filterer.invertedOutputVcf = Some(invVcf)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath}")

    filterer.resToDom = Some("dummy")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy")

    filterer.trioCompound = Some("dummy")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy")

    filterer.deNovoInSample = Some("dummy")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy")

    filterer.deNovoTrio = Some("dummy")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy")

    filterer.trioLossOfHet = Some("dummy")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy")

    filterer.mustHaveVariant = List("sample1", "sample2")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2")

    filterer.calledIn  = List("sample1", "sample2")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2")

    filterer.mustHaveGenotype = List("sample1:HET", "sample2:HET")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET")

    filterer.diffGenotype = List("sample1:sample2", "sample2:sample3")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET " +
      s"--diffGenotype sample1:sample2 --diffGenotype sample2:sample3")

    filterer.filterHetVarToHomVar = List("sample1:sample2", "sample2:sample3")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET " +
      s"--diffGenotype sample1:sample2 --diffGenotype sample2:sample3 " +
      s"--filterHetVarToHomVar sample1:sample2 --filterHetVarToHomVar sample2:sample3")

    filterer.minQualScore = Some(50)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET " +
      s"--diffGenotype sample1:sample2 --diffGenotype sample2:sample3 " +
      s"--filterHetVarToHomVar sample1:sample2 --filterHetVarToHomVar sample2:sample3 " +
      s"--minQualScore 50.0")

    filterer.id = List("rs01", "rs02")

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET " +
      s"--diffGenotype sample1:sample2 --diffGenotype sample2:sample3 " +
      s"--filterHetVarToHomVar sample1:sample2 --filterHetVarToHomVar sample2:sample3 " +
      s"--minQualScore 50.0 " +
      s"--id rs01 --id rs02")

    val idFile = File.createTempFile("vcfFilter", ".txt")
    idFile.deleteOnExit()
    filterer.idFile = Some(idFile)

    cmd(filterer.cmdLine) should equal(s"/usr/bin/java -XX:+UseParallelOldGC " +
      s"-XX:ParallelGCThreads=4 -XX:GCTimeLimit=50 -XX:GCHeapFreeLimit=10 " +
      s" -Dscala.concurrent.context.numThreads=1 -cp nl.lumc.sasc.biopet.tools.VcfFilter " +
      s"-I ${iVcf.getAbsolutePath} " +
      s"-o ${oVcf.getAbsolutePath} " +
      s"--minSampleDepth 50 " +
      s"--minTotalDepth 50 " +
      s"--minAlternateDepth 50 " +
      s"--minSamplesPass 5 " +
      s"--minGenomeQuality 100 " +
      s"--filterRefCalls " +
      s"--invertedOutputVcf ${invVcf.getAbsolutePath} " +
      s"--resToDom dummy " +
      s"--trioCompound dummy " +
      s"--deNovoInSample dummy " +
      s"--deNovoTrio dummy " +
      s"--trioLossOfHet dummy " +
      s"--mustHaveVariant sample1 --mustHaveVariant sample2 " +
      s"--calledIn sample1 --calledIn sample2 " +
      s"--mustHaveGenotype sample1:HET --mustHaveGenotype sample2:HET " +
      s"--diffGenotype sample1:sample2 --diffGenotype sample2:sample3 " +
      s"--filterHetVarToHomVar sample1:sample2 --filterHetVarToHomVar sample2:sample3 " +
      s"--minQualScore 50.0 " +
      s"--id rs01 --id rs02 " +
      s"--idFile ${idFile.getAbsolutePath}")
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
