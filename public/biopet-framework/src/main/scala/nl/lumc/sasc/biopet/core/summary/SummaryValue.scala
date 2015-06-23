package nl.lumc.sasc.biopet.core.summary

import java.math.BigDecimal

/**
 * Created by pjvan_thof on 4/23/15.
 */
case class SummaryValue(val value: Option[Any]) {

  def this(path: List[String],
           summary: Summary,
           sampleId: Option[String] = None,
           libId: Option[String] = None) = {
    this((sampleId, libId) match {
      case (Some(sample), Some(lib)) => summary.getLibraryValue(sample, lib, path: _*)
      case (Some(sample), _)         => summary.getSampleValue(sample, path: _*)
      case _                         => summary.getValue(path: _*)
    })
  }

  def +(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a + b.toString.toDouble))
      case (Some(a: Int), Some(b))    => SummaryValue(Some(a + b.toString.toInt))
      case (Some(a), Some(b))         => SummaryValue(Some(a.toString.toDouble + b.toString.toDouble))
      case _                          => SummaryValue(None)
    }
  }

  def -(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a - b.toString.toDouble))
      case (Some(a: Int), Some(b))    => SummaryValue(Some(a - b.toString.toInt))
      case (Some(a), Some(b))         => SummaryValue(Some(a.toString.toDouble - b.toString.toDouble))
      case _                          => SummaryValue(None)
    }
  }

  def /(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a / b.toString.toDouble))
      case (Some(a: Int), Some(b))    => SummaryValue(Some(a / b.toString.toInt))
      case (Some(a), Some(b))         => SummaryValue(Some(a.toString.toDouble / b.toString.toDouble))
      case _                          => SummaryValue(None)
    }
  }

  def *(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a * b.toString.toDouble))
      case (Some(a: Int), Some(b))    => SummaryValue(Some(a * b.toString.toInt))
      case (Some(a), Some(b))         => SummaryValue(Some(a.toString.toDouble * b.toString.toDouble))
      case _                          => SummaryValue(None)
    }
  }

  def %(that: SummaryValue): SummaryValue = {
    (this.value, that.value) match {
      case (Some(a: Double), Some(b)) => SummaryValue(Some(a % b.toString.toDouble))
      case (Some(a: Int), Some(b))    => SummaryValue(Some(a % b.toString.toInt))
      case (Some(a), Some(b))         => SummaryValue(Some(a.toString.toDouble % b.toString.toDouble))
      case _                          => SummaryValue(None)
    }
  }
}

