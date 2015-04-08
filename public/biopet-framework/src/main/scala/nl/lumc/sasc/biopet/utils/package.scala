package nl.lumc.sasc.biopet

import scala.util.{ Failure, Success, Try }

/**
 * General utility functions.
 *
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package object utils {

  /** Regular expression for matching entire integer numbers (numbers without decimals / fractions) */
  val isInteger = """^([-+]?\d+)L?$""".r

  /** Regular expression for matching entire decimal numbers (compatible with the scientific notation) */
  val isDecimal = """^([-+]?\d*\.?\d+(?:[eE][-+]?[0-9]+)?)$""".r

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
  def tryToConvert(raw: String, funcs: (String => Any)*): Try[Any] = funcs match {

    case Seq(firstFunc, otherFuncs @ _*) =>
      Try(firstFunc(raw))
        .transform(s => Success(s), f => tryToConvert(raw, otherFuncs: _*))

    case Nil => Try(throw new Exception(s"Can not extract value from string $raw"))
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
   * @return a [[Try]] object encapsulating the conversion result.
   */
  def tryToParseNumber(raw: String) = raw match {

    case isInteger(i) =>
      tryToConvert(i, x => x.toInt, x => x.toLong, x => BigInt(x))

    case isDecimal(f) =>
      tryToConvert(f, x => x.toDouble, x => BigDecimal(x))

    case otherwise => Try(throw new Exception(s"Can not extract number from string $raw"))
  }
}
