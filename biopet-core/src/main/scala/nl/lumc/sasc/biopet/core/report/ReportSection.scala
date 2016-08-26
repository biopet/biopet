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
package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 4/8/15.
 *
 * @param location Location inside the classpath / jar
 * @param args arguments only for current section, this is not passed to other sub pages
 */
case class ReportSection(location: String,
                         args: Map[String, Any] = Map()) {
  /**
   * This method will render this section
   * @param args Possible to give more arguments
   * @return Rendered result for this section
   */
  def render(args: Map[String, Any] = Map()): String = {
    ReportBuilder.renderTemplate(location, args ++ this.args)
  }
}
