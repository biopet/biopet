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
