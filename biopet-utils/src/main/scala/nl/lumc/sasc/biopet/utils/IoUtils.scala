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
package nl.lumc.sasc.biopet.utils

import java.io._

import scala.io.Source
import scala.sys.process.Process

/**
  * This object contains generic io methods
  *
  * Created by pjvan_thof on 6/4/15.
  */
object IoUtils {
  def copyFile(in: File, out: File, createDirs: Boolean = false): Unit = {
    copyStreamToFile(new FileInputStream(in), out, createDirs)
  }

  def copyStreamToFile(in: InputStream, out: File, createDirs: Boolean = false): Unit = {
    if (createDirs) out.getParentFile.mkdirs()
    val os = new FileOutputStream(out)

    org.apache.commons.io.IOUtils.copy(in, os)
    os.close()
    in.close()
  }

  def copyDir(inputDir: File, externalDir: File): Unit = {
    require(inputDir.isDirectory)
    externalDir.mkdirs()
    for (srcFile <- inputDir.listFiles) {
      if (srcFile.isDirectory)
        copyDir(new File(inputDir, srcFile.getName), new File(externalDir, srcFile.getName))
      else {
        val newFile = new File(externalDir, srcFile.getName)
        copyFile(srcFile, newFile)
      }
    }
  }

  /** Possible compression extensions to trim from input files. */
  val zipExtensions = Set(".gz", ".gzip", ".bzip2", ".bz", ".xz", ".zip")

  /**
    * Given a file object and a set of compression extensions, return the filename without any of the compression
    * extensions.
    *
    * Examples:
    *  - my_file.fq.gz returns "my_file.fq"
    *  - my_other_file.fastq returns "my_file.fastq"
    *
    * @param f Input file object.
    * @param exts Possible compression extensions to trim.
    * @return Filename without compression extension.
    */
  def getUncompressedFileName(f: File, exts: Set[String] = zipExtensions): String =
    exts.foldLeft(f.getName) { (fname, ext) =>
      if (fname.toLowerCase.endsWith(ext)) fname.dropRight(ext.length)
      else fname
    }

  /**
    * This return the contends of a file as a List[String]
    *
    * @param file
    * @return
    */
  def getLinesFromFile(file: File): List[String] = {
    val reader = Source.fromFile(file)
    val lines = reader.getLines().toList
    reader.close()
    lines
  }

  def writeLinesToFile(lines: List[String]): File = {
    val file = File.createTempFile("", "")
    val writer = new PrintWriter(file)
    lines.foreach(writer.println(_))
    writer.close()
    file.deleteOnExit()
    file
  }

  def executableExist(exe: String): Boolean = {
    try {
      val process = Process(Seq(exe)).run()
      true
    } catch {
      case e: IOException => false
    }
  }
}
