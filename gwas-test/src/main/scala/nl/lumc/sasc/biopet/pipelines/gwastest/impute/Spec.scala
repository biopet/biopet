package nl.lumc.sasc.biopet.pipelines.gwastest.impute

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * .spec file parser combinator.
 *
 * TODO: clean up.
 *
 * @author Matthijs Moed <M.H.Moed@lumc.nl>
 */
object Spec extends JavaTokenParsers {
  def obj: Parser[Map[String, Any]] =
    "{" ~> rep(member) <~ "}" ^^ (Map() ++ _)

  def arr: Parser[List[Any]] = "(" ~> repsep(value, ",") <~ ")"

  def key: Parser[String] = """([\w+])*+""".r

  def member: Parser[(String, Any)] = key ~ "=" ~ value <~ literal(";") ^^ {
    case name ~ "=" ~ value => (name, value)
  }

  // This is a hack to get around the fact that JavaTokenParsers'
  // stringLiterals do not have their double quotes removed. They're stripped
  // here, without any error checking.
  // TODO: implement as token parser

  def string: Parser[String] = stringLiteral ^^ {
    case s => s.substring(1, s.length - 1)
  }

  def value: Parser[Any] = (
    obj
    | arr
    | string
    | wholeNumber ^^ (_.toInt)
    | floatingPointNumber ^^ (_.toDouble)
  )

  //

  trait SpecDecoder[T] {
    def decode(obj: Map[String, Any]): T
  }

  case class ImputeOutput(chromosome: String, name: String, orig: String)

  class ImputeOutputMappingDecoder extends SpecDecoder[List[ImputeOutput]] {
    override def decode(obj: Map[String, Any]): List[ImputeOutput] = {
      val dicts = obj("files").asInstanceOf[List[Map[String, String]]]
      dicts.map(d => ImputeOutput(d("chromosome"), d("name"), d("orig")))
    }
  }

  implicit val imputeOutputMappingDecoder = new ImputeOutputMappingDecoder

  implicit class SpecDecoderOps(string: String) {
    implicit def decode[T](implicit decoder: SpecDecoder[T]): T = {
      decoder.decode(Spec.parseAll(Spec.obj, string).get)
    }
  }
}

