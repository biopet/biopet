package nl.lumc.sasc.biopet.core

import java.io.{File, PrintWriter}

import com.google.common.io.Files
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

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
  }

  @Test
  def testDepsFileArg: Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    val depsfile = PipelineStatusTest.writeDeps(outputDir)

    PipelineStatus.main(Array("-o", outputDir.toString, "-d", outputDir.toString, "--depsFile", depsfile.toString))
  }

}

object PipelineStatusTest {

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
       |    "gzip_1" : {
       |      "fail_files" : [
       |        "$outputDir/.file.out.gz.fail",
       |        "$outputDir/..file.out.gz.Gzip.out.fail"
       |      ],
       |      "done_at_start" : false,
       |      "output_used_by_jobs" : [
       |        
       |      ],
       |      "outputs" : [
       |        "$outputDir/file.out.gz",
       |        "$outputDir/.file.out.gz.Gzip.out"
       |      ],
       |      "command" : "\n\n '/bin/gzip'  -c  '$outputDir/file.out'  >  '$outputDir/file.out.gz' ",
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
       |      "main_job" : true
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
       |      "command" : "\n\n '/bin/cat'  'test.deps'  >  '$outputDir/file.out' ",
       |      "stdout_file" : "$outputDir/.file.out.Cat.out",
       |      "depends_on_intermediate" : false,
       |      "fail_at_start" : false,
       |      "inputs" : [
       |        "test.deps"
       |      ],
       |      "depends_on_jobs" : [
       |        
       |      ],
       |      "intermediate" : false,
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
       |      "intermediate" : false
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
