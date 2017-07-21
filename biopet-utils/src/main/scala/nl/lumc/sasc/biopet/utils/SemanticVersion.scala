package nl.lumc.sasc.biopet.utils
import scala.util.matching.Regex

/**
  * Created by pjvanthof on 29/04/2017.
  */
case class SemanticVersion(major: Int, minor: Int, patch: Int, build: Option[String] = None) {
  def >(that: SemanticVersion): Boolean = {
    if (this.major != that.major) this.major > that.major
    else if (this.minor != that.minor) this.minor > that.minor
    else if (this.patch != that.patch) this.patch > that.patch
    else false
  }

  def <(that: SemanticVersion): Boolean = {
    if (this.major != that.major) this.major < that.major
    else if (this.minor != that.minor) this.minor < that.minor
    else if (this.patch != that.patch) this.patch < that.patch
    else false
  }

  def >=(that: SemanticVersion): Boolean = {
    if (this.major != that.major) this.major > that.major
    else if (this.minor != that.minor) this.minor > that.minor
    else if (this.patch != that.patch) this.patch > that.patch
    else true
  }

  def <=(that: SemanticVersion): Boolean = {
    if (this.major != that.major) this.major < that.major
    else if (this.minor != that.minor) this.minor < that.minor
    else if (this.patch != that.patch) this.patch < that.patch
    else true
  }
}

object SemanticVersion {
  val semanticVersionRegex: Regex = "[vV]?(\\d+)\\.(\\d+)\\.(\\d+)(-.*)?".r

  /**
    * Check whether a version string is a semantic version.
    *
    * @param version version string
    * @return boolean
    */
  def isSemanticVersion(version: String): Boolean = getSemanticVersion(version).isDefined

  /**
    * Check whether a version string is a semantic version.
    * Note: the toInt calls here are only safe because the regex only matches numbers
    *
    * @param version version string
    * @return SemanticVersion case class
    */
  def getSemanticVersion(version: String): Option[SemanticVersion] = {
    version match {
      case semanticVersionRegex(major, minor, patch, build) =>
        Some(
          SemanticVersion(major.toInt,
                          minor.toInt,
                          patch.toInt,
                          Option(build).map(x => x.stripPrefix("-"))))
      case _ => None
    }
  }

}
