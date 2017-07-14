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
package nl.lumc.sasc.biopet

import scala.util.{Failure, Success, Try}
import scala.math._
import scala.util.matching.Regex

/**
  * General utility functions.
  *
  * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
  */
package object utils {

  /** Regular expression for matching entire integer numbers (numbers without decimals / fractions) */
  val isInteger: Regex = """^([-+]?\d+)L?$""".r

  /** Regular expression for matching entire decimal numbers (compatible with the scientific notation) */
  val isDecimal: Regex = """^([-+]?\d*\.?\d+(?:[eE][-+]?[0-9]+)?)$""".r

  def textToSize(text: String): Long = {
    text.last match {
      case 'g' | 'G' => ((1L << 30) * text.stripSuffix("g").stripSuffix("G").toDouble).toLong
      case 'm' | 'M' => ((1L << 20) * text.stripSuffix("g").stripSuffix("G").toDouble).toLong
      case 'k' | 'K' => ((1L << 10) * text.stripSuffix("g").stripSuffix("G").toDouble).toLong
      case _ => text.toLong
    }
  }

  /**
    * Tries to convert the given string with the given conversion functions recursively.
    *
    * If conversion is successful, the converted object is returned within as a [[Success]] type. Otherwise, a [[Failure]]
    * is returned. The order of conversion functions is the same as the order they are specified.
    *
    * @param raw the string to convert.
    * @param funcs one or more conversion functions to apply.
    * @return a [[Try]] object encapsulating the conversion result.
    */
  def tryToConvert(raw: String, funcs: (String => Any)*): Try[Any] = {
    if (funcs.isEmpty) Try(throw new Exception(s"Can not extract value from string $raw"))
    else
      Try(funcs.head(raw))
        .transform(s => Success(s), _ => tryToConvert(raw, funcs.tail: _*))
  }

  /**
    * Tries to convert the given string into the appropriate number representation.
    *
    * The given string must be whole numbers without any preceeding or succeeding whitespace. This function takes
    * into account the maximum values of the number object to use. For example, if the raw string represents a bigger
    * number than the maximum [[Int]] value, then a [[Long]] will be used. If the number is still bigger than a [[Long]],
    * the [[BigInt]] class will be used. The same is applied for decimal numbers, where the conversion order is first
    * a [[Double]], then a [[BigDecimal]].
    *
    * @param raw the string to convert.
    * @param fallBack Allows also to return the string itself when converting fails, default false.
    * @return a [[Try]] object encapsulating the conversion result.
    */
  def tryToParseNumber(raw: String, fallBack: Boolean = false): Try[Any] = raw match {
    case isInteger(i) => tryToConvert(i, x => x.toInt, x => x.toLong, x => BigInt(x))
    case isDecimal(f) => tryToConvert(f, x => x.toDouble, x => BigDecimal(x))
    case _ if fallBack => Try(raw)
    case _ => Try(throw new Exception(s"Can not extract number from string $raw"))
  }

  /** Converts string with underscores into camel-case strings */
  def camelize(ustring: String): String =
    ustring
      .split("_")
      .map(_.toLowerCase.capitalize)
      .mkString("")

  /** Split camelcase to separated words */
  def camelizeToWords(string: String, current: List[String] = Nil): List[String] = {
    if (string.nonEmpty) {
      val char = string.tail.find(!_.isLower)
      char match {
        case Some(c) =>
          val index = string.indexOf(c, 1)
          camelizeToWords(string.drop(index), current ::: List(string.take(index)))
        case _ => current ::: List(string)
      }
    } else current
  }

  /** Convert camelcase to underscores */
  def unCamelize(string: String): String = {
    camelizeToWords(string).map(_.toLowerCase).mkString("_")
  }

  /** Function to sort Any values */
  def sortAnyAny(a: Any, b: Any): Boolean = {
    a match {
      case ai: Int =>
        b match {
          case bi: Int => ai < bi
          case bi: Double => ai < bi
          case _ => a.toString < b.toString
        }
      case _ => a.toString < b.toString
    }
  }
}
