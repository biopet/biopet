package nl.lumc.sasc.biopet.core

import java.io.{File, PrintWriter}

import com.google.common.io.Files
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import PipelineStatusTest.Status
import nl.lumc.sasc.biopet.utils.IoUtils._

/**
  * Created by pjvan_thof on 10-1-17.
  */
class PipelineStatusTest extends TestNGSuite with Matchers {
  @Test
  def testDefault: Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    PipelineStatusTest.writeDeps(outputDir)

    PipelineStatus.main(Array("-o", outputDir.toString, "-d", outputDir.toString))
    checkOutput(outputDir)
  }

  @Test
  def testDepsFileArg: Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    val depsfile = PipelineStatusTest.writeDeps(outputDir)

    PipelineStatus.main(Array("-o", outputDir.toString, "-d", outputDir.toString, "--depsFile", depsfile.toString))
    checkOutput(outputDir)
  }

  def checkOutput(outputDir: File,
                  cat: Status.Value = Status.Pending,
                  gzip: Status.Value = Status.Pending,
                  zcat: Status.Value = Status.Pending): Unit = {
    val jobsGvFile = new File(outputDir, "jobs.gv")
    val mainJobsGvFile = new File(outputDir, "main_jobs.gv")
    val compressJobsGvFile = new File(outputDir, "compress.jobs.gv")
    val compressMainJobsGvFile = new File(outputDir, "compress.main_jobs.gv")
    jobsGvFile should exist
    mainJobsGvFile should exist
    compressJobsGvFile should exist
    compressMainJobsGvFile should exist

    val jobsGvLines = getLinesFromFile(jobsGvFile)
    require(jobsGvLines.exists(_.contains("cat_1 -> gzip_1")))
    require(jobsGvLines.exists(_.contains("gzip_1 -> zcat_1")))
    require(jobsGvLines.forall(!_.contains("cat_1 -> zcat_1")))
    require(jobsGvLines.exists(_.contains("cat_1 [style = dashed]")))
    require(jobsGvLines.forall(!_.contains("gzip_1 [style = dashed]")))
    require(jobsGvLines.forall(!_.contains("zcat_1 [style = dashed]")))

    val mainJobsGvLines = getLinesFromFile(mainJobsGvFile)
    require(mainJobsGvLines.exists(_.contains("cat_1 -> zcat_1")))
    require(mainJobsGvLines.forall(!_.contains("cat_1 -> gzip_1")))
    require(mainJobsGvLines.forall(!_.contains("gzip_1 -> zcat_1")))
    require(mainJobsGvLines.exists(_.contains("cat_1 [style = dashed]")))
    require(mainJobsGvLines.forall(!_.contains("gzip_1 [style = dashed]")))
    require(mainJobsGvLines.forall(!_.contains("zcat_1 [style = dashed]")))

    val compressJobsGvLines = getLinesFromFile(compressJobsGvFile)
    require(compressJobsGvLines.exists(_.contains("cat -> gzip")))
    require(compressJobsGvLines.exists(_.contains("gzip -> zcat")))
    require(compressJobsGvLines.forall(!_.contains("cat -> zcat")))
    require(compressJobsGvLines.exists(_.contains("cat [style = dashed]")))
    require(compressJobsGvLines.forall(!_.contains("gzip [style = dashed]")))
    require(compressJobsGvLines.forall(!_.contains("zcat [style = dashed]")))

    val compressMainJobsGvLines = getLinesFromFile(compressMainJobsGvFile)
    require(compressMainJobsGvLines.exists(_.contains("cat -> zcat")))
    require(compressMainJobsGvLines.forall(!_.contains("cat -> gzip")))
    require(compressMainJobsGvLines.forall(!_.contains("gzip -> zcat")))
    require(compressMainJobsGvLines.exists(_.contains("cat [style = dashed]")))
    require(compressMainJobsGvLines.forall(!_.contains("gzip [style = dashed]")))
    require(compressMainJobsGvLines.forall(!_.contains("zcat [style = dashed]")))

  }

}

object PipelineStatusTest {

  object Status extends Enumeration {
    val Failed, Done, Pending = Value
  }

  def writeDeps(outputDir: File): File = {
    require(outputDir.exists())
    val depsFile = new File(outputDir, ".log/test.1234567890/graph/deps.json")
    depsFile.getParentFile.mkdirs()
    val writer = new PrintWriter(depsFile)
    writer.println(defaultDeps(outputDir))
    writer.close()
    depsFile
  }

  def defaultDeps(outputDir: File) =
    s"""
       |{
       |  "jobs" : {
       |    "zcat_1" : {
       |      "fail_files" : [
       |        "$outputDir/.file.out.zcat.fail",
       |        "$outputDir/..file.out.zcat.Zcat.out.fail"
       |      ],
       |      "done_at_start" : false,
       |      "output_used_by_jobs" : [
       |
       |      ],
       |      "outputs" : [
       |        "$outputDir/file.out.zcat",
       |        "$outputDir/.file.out.zcat.Zcat.out"
       |      ],
       |      "command" : "'/bin/zcat'  '$outputDir/file.out.gz'  >  '$outputDir/file.out.zcat' ",
       |      "stdout_file" : "$outputDir/.file.out.zcat.Zcat.out",
       |      "depends_on_intermediate" : false,
       |      "fail_at_start" : false,
       |      "inputs" : [
       |        "$outputDir/file.out.gz"
       |      ],
       |      "depends_on_jobs" : [
       |        "gzip_1"
       |      ],
       |      "intermediate" : false,
       |      "done_files" : [
       |        "$outputDir/.file.out.zcat.done",
       |        "$outputDir/..file.out.zcat.Zcat.out.done"
       |      ],
       |      "main_job" : true
       |    },
       |    "gzip_1" : {
       |      "fail_files" : [
       |        "$outputDir/.file.out.gz.fail",
       |        "$outputDir/..file.out.gz.Gzip.out.fail"
       |      ],
       |      "done_at_start" : false,
       |      "output_used_by_jobs" : [
       |        "zcat_1"
       |      ],
       |      "outputs" : [
       |        "$outputDir/file.out.gz",
       |        "$outputDir/.file.out.gz.Gzip.out"
       |      ],
       |      "command" : "'/bin/gzip'  -c  '$outputDir/file.out'  >  '$outputDir/file.out.gz' ",
       |      "stdout_file" : "$outputDir/.file.out.gz.Gzip.out",
       |      "depends_on_intermediate" : false,
       |      "fail_at_start" : false,
       |      "inputs" : [
       |        "$outputDir/file.out"
       |      ],
       |      "depends_on_jobs" : [
       |        "cat_1"
       |      ],
       |      "intermediate" : false,
       |      "done_files" : [
       |        "$outputDir/.file.out.gz.done",
       |        "$outputDir/..file.out.gz.Gzip.out.done"
       |      ],
       |      "main_job" : false
       |    },
       |    "cat_1" : {
       |      "fail_files" : [
       |        "$outputDir/.file.out.fail",
       |        "$outputDir/..file.out.Cat.out.fail"
       |      ],
       |      "done_at_start" : false,
       |      "output_used_by_jobs" : [
       |        "gzip_1"
       |      ],
       |      "outputs" : [
       |        "$outputDir/file.out",
       |        "$outputDir/.file.out.Cat.out"
       |      ],
       |      "command" : "'/bin/cat'  'test.deps'  >  '$outputDir/file.out' ",
       |      "stdout_file" : "$outputDir/.file.out.Cat.out",
       |      "depends_on_intermediate" : false,
       |      "fail_at_start" : false,
       |      "inputs" : [
       |        "test.deps"
       |      ],
       |      "depends_on_jobs" : [
       |        
       |      ],
       |      "intermediate" : true,
       |      "done_files" : [
       |        "$outputDir/.file.out.done",
       |        "$outputDir/..file.out.Cat.out.done"
       |      ],
       |      "main_job" : true
       |    }
       |  },
       |  "files" : [
       |    {
       |      "output_jobs" : [
       |        "cat_1"
       |      ],
       |      "path" : "$outputDir/file.out",
       |      "input_jobs" : [
       |        "gzip_1"
       |      ],
       |      "exists_at_start" : false,
       |      "pipeline_input" : false,
       |      "intermediate" : true
       |    },
       |    {
       |      "output_jobs" : [
       |        "gzip_1"
       |      ],
       |      "path" : "$outputDir/.file.out.gz.Gzip.out",
       |      "input_jobs" : [
       |        
       |      ],
       |      "exists_at_start" : false,
       |      "pipeline_input" : false,
       |      "intermediate" : false
       |    },
       |    {
       |      "output_jobs" : [
       |        "gzip_1"
       |      ],
       |      "path" : "$outputDir/file.out.gz",
       |      "input_jobs" : [
       |        
       |      ],
       |      "exists_at_start" : false,
       |      "pipeline_input" : false,
       |      "intermediate" : false
       |    },
       |    {
       |      "output_jobs" : [
       |        
       |      ],
       |      "path" : "$outputDir/test.deps",
       |      "input_jobs" : [
       |        "cat_1"
       |      ],
       |      "exists_at_start" : false,
       |      "pipeline_input" : true,
       |      "intermediate" : false
       |    },
       |    {
       |      "output_jobs" : [
       |        "cat_1"
       |      ],
       |      "path" : "$outputDir/.file.out.Cat.out",
       |      "input_jobs" : [
       |        
       |      ],
       |      "exists_at_start" : false,
       |      "pipeline_input" : false,
       |      "intermediate" : false
       |    }
       |  ]
       |}
     """.stripMargin
}
