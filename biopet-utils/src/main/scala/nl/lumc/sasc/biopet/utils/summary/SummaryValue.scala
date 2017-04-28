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
package nl.lumc.sasc.biopet.utils.summary

/**
  * This case class is used for easy access and calculations on those values
  *
  * Created by pjvan_thof on 4/23/15.
  *
  * @deprecated
  */
case class SummaryValue(value: Option[Any]) {

  def this(path: List[String],
           summary: Summary,
           sampleId: Option[String] = None,
           libId: Option[String] = None) = {
    this((sampleId, libId) match {
      case (Some(sample), Some(lib)) => summary.getLibraryValue(sample, lib, path: _*)
      case (Some(sample), _) => summary.getSampleValue(sample, path: _*)
      case _ => summary.getValue(path: _*)
    })
  }

  def +(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a + b.toString.toDouble))
      case (Some(a: Int), Some(b)) => SummaryValue(Some(a + b.toString.toInt))
      case (Some(a), Some(b)) => SummaryValue(Some(a.toString.toDouble + b.toString.toDouble))
      case _ => SummaryValue(None)
    }
  }

  def -(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a - b.toString.toDouble))
      case (Some(a: Int), Some(b)) => SummaryValue(Some(a - b.toString.toInt))
      case (Some(a), Some(b)) => SummaryValue(Some(a.toString.toDouble - b.toString.toDouble))
      case _ => SummaryValue(None)
    }
  }

  def /(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a / b.toString.toDouble))
      case (Some(a: Int), Some(b)) => SummaryValue(Some(a / b.toString.toInt))
      case (Some(a), Some(b)) => SummaryValue(Some(a.toString.toDouble / b.toString.toDouble))
      case _ => SummaryValue(None)
    }
  }

  def *(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a * b.toString.toDouble))
      case (Some(a: Int), Some(b)) => SummaryValue(Some(a * b.toString.toInt))
      case (Some(a), Some(b)) => SummaryValue(Some(a.toString.toDouble * b.toString.toDouble))
      case _ => SummaryValue(None)
    }
  }

  def %(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a % b.toString.toDouble))
      case (Some(a: Int), Some(b)) => SummaryValue(Some(a % b.toString.toInt))
      case (Some(a), Some(b)) => SummaryValue(Some(a.toString.toDouble % b.toString.toDouble))
      case _ => SummaryValue(None)
    }
  }
}
