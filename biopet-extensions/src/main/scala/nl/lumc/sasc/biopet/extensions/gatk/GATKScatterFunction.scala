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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import org.broadinstitute.gatk.queue.extensions.gatk.GATKIntervals
import org.broadinstitute.gatk.queue.function.scattergather.{ CloneFunction, ScatterFunction }
import org.broadinstitute.gatk.utils.commandline.Output
import org.broadinstitute.gatk.utils.interval.IntervalUtils
import org.broadinstitute.gatk.utils.io.IOUtils

trait GATKScatterFunction extends ScatterFunction {
  /* The runtime field to set for specifying intervals. */
  private final val intervalsField = "intervals"
  private final val intervalsStringField = "intervalsString"
  private final val excludeIntervalsField = "excludeIntervals"
  private final val excludeIntervalsStringField = "excludeIntervalsString"
  private final val intervalsSetRuleField = "interval_set_rule"
  private final val intervalMergingField = "interval_merging"
  private final val intervalPaddingField = "interval_padding"

  @Output(doc = "Scatter function outputs")
  var scatterOutputFiles: Seq[File] = Nil

  /** The original GATK function. */
  protected var originalGATK: CommandLineGATK = _

  /** Whether the last scatter job should also include any unmapped reads. */
  var includeUnmapped: Boolean = _

  override def init() {
    this.originalGATK = this.originalFunction.asInstanceOf[CommandLineGATK]
    // If intervals have been specified check if unmapped is included
    if (this.originalGATK.intervals.size + this.originalGATK.intervalsString.size > 0)
      this.includeUnmapped = this.originalGATK.intervalsString.exists(interval => IntervalUtils.isUnmapped(interval))
  }

  override def isScatterGatherable: Boolean = {
    this.originalGATK.reference_sequence != null
  }

  override def initCloneInputs(cloneFunction: CloneFunction, index: Int) {
    cloneFunction.setFieldValue(this.intervalsField, Seq(new File("scatter.intervals")))
    if (index == this.scatterCount && this.includeUnmapped)
      cloneFunction.setFieldValue(this.intervalsStringField, Seq("unmapped"))
    else
      cloneFunction.setFieldValue(this.intervalsStringField, Seq.empty[String])

    cloneFunction.setFieldValue(this.intervalsSetRuleField, null)
    cloneFunction.setFieldValue(this.intervalMergingField, null)
    cloneFunction.setFieldValue(this.intervalPaddingField, None)
    cloneFunction.setFieldValue(this.excludeIntervalsField, Seq.empty[File])
    cloneFunction.setFieldValue(this.excludeIntervalsStringField, Seq.empty[String])
  }

  override def bindCloneInputs(cloneFunction: CloneFunction, index: Int) {
    val scatterPart = cloneFunction.getFieldValue(this.intervalsField)
      .asInstanceOf[Seq[File]]
      .map(file => IOUtils.absolute(cloneFunction.commandDirectory, file))
    cloneFunction.setFieldValue(this.intervalsField, scatterPart)
    this.scatterOutputFiles ++= scatterPart
  }

  /**
   * @return true if all interval files exist.
   */
  protected def intervalFilesExist: Boolean = {
    !(this.originalGATK.intervals ++ this.originalGATK.excludeIntervals).exists(interval => !interval.exists())
  }

  /**
   * @return the maximum number of intervals or this.scatterCount if the maximum can't be determined ahead of time.
   */
  protected def maxIntervals: Int
}

object GATKScatterFunction {
  var gatkIntervalsCache = Seq.empty[GATKIntervals]

  def getGATKIntervals(originalFunction: CommandLineGATK): GATKIntervals = {
    val gatkIntervals = new GATKIntervals(
      originalFunction.reference_sequence,
      originalFunction.intervals,
      originalFunction.intervalsString,
      originalFunction.interval_set_rule.orNull,
      originalFunction.interval_merging.orNull,
      originalFunction.interval_padding,
      originalFunction.excludeIntervals,
      originalFunction.excludeIntervalsString)
    gatkIntervalsCache.find(_ == gatkIntervals) match {
      case Some(existingGatkIntervals) => existingGatkIntervals
      case None =>
        gatkIntervalsCache :+= gatkIntervals
        gatkIntervals
    }
  }
}
