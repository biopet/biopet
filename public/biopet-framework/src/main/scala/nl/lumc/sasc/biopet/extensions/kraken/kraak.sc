
import scala.util.matching.Regex

val versionString = "Kraken version 0.10.5-beta\nCopyright 2013-2015, Derrick Wood (dwood@cs.jhu.edu)"
val versionRegex: Regex = """Kraken version ([\d\w\-\.]+)\n.*""".r
versionString match {
  case versionRegex(m) => println(m)
  case _ => println("no match")
}

