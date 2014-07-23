/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.lumc.sasc.biopet.core

import java.io.FileOutputStream
//import org.broadinstitute.sting.queue.QCommandLine
import org.broadinstitute.gatk.queue.util.Logging

trait PipelineCommand extends Logging {
  //  val src = ""
  //  val extension = ".scala"
  val pipeline = ""

  def main(args: Array[String]): Unit = {
    //    val tempFile = java.io.File.createTempFile(src + ".", extension)
    //    val is = getClass.getResourceAsStream(src + extension)
    //    val os = new FileOutputStream(tempFile)
    //    org.apache.commons.io.IOUtils.copy(is, os)
    //    os.close()

    var argv: Array[String] = Array()
    //argv ++= Array("-S", tempFile.getAbsolutePath)
    argv ++= Array("-S", pipeline)
    argv ++= args
    return BiopetQCommandLine.main(argv)
  }
}