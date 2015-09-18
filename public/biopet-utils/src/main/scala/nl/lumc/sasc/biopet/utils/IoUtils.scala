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
package nl.lumc.sasc.biopet.utils

import java.io.{ File, FileInputStream, FileOutputStream, InputStream }

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
      if (srcFile.isDirectory) copyDir(new File(inputDir, srcFile.getName), new File(externalDir, srcFile.getName))
      else {
        val newFile = new File(externalDir, srcFile.getName)
        copyFile(srcFile, newFile)
      }
    }
  }
}
