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
package nl.lumc.sasc

import java.util.Properties

/**
  * Common values and functions for the biopet package
  *
  * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
  */
package object biopet {

  /** Latest git commit hash, with '-dirty' appended if any uncommited changes were present */
  val LastCommitHash: String = {
    val prop = new Properties()
    prop.load(getClass.getClassLoader.getResourceAsStream("git.properties"))

    val hash = prop.getProperty("git.commit.id.abbrev")
    if (prop.getProperty("git.commit.id.describe-short").endsWith("-dirty")) s"$hash-dirty"
    else hash
  }

  /** Package version */
  // needs the Option here since the value is `null` when we run from an unpackaged JAR
  val Version: String =
    Option(getClass.getPackage.getImplementationVersion).getOrElse("unpackaged")

  /** Full version (raw version and latest commit hash) */
  val FullVersion: String = s"$Version ($LastCommitHash)"
}
