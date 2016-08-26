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
package org.example.group

import nl.lumc.sasc.biopet.utils.{ BiopetExecutable, MainCommand }

/**
 * Created by pjvanthof on 30/08/15.
 */
object ExecutableExample extends BiopetExecutable {

  /** This list defines the pipeline that are usable from the executable */
  def pipelines: List[MainCommand] = List(
    org.example.group.pipelines.MultisamplePipeline,
    org.example.group.pipelines.BiopetPipeline,
    org.example.group.pipelines.SimplePipeline
  )

  /** This list defines the (biopet)tools that are usable from the executable */
  def tools: List[MainCommand] = Nil
}
