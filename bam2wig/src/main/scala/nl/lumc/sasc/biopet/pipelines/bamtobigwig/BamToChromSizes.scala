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
package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import java.io.{File, PrintWriter}

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.collection.JavaConversions._

/**
  * Class to extract chrom.sizes files from a bam file
  *
  * Created by pjvan_thof on 1/29/15.
  */
class BamToChromSizes(val parent: Configurable) extends InProcessFunction with Configurable {
  @Input
  var bamFile: File = _

  @Output
  var chromSizesFile: File = _

  @Input(required = false)
  var deps: List[File] = Nil

  def run(): Unit = {
    val bamReader = SamReaderFactory.makeDefault().open(bamFile)
    val writer = new PrintWriter(chromSizesFile)
    for (ref <- bamReader.getFileHeader.getSequenceDictionary.getSequences) {
      writer.println(ref.getSequenceName + "\t" + ref.getSequenceLength)
    }
    bamReader.close()
    writer.close()
  }
}
