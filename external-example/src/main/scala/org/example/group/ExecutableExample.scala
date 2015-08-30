package org.example.group

import nl.lumc.sasc.biopet.core.{ MainCommand, BiopetExecutable }

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
