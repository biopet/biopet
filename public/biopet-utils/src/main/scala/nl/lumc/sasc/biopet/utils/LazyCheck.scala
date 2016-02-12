package nl.lumc.sasc.biopet.utils

/**
 * Created by pjvan_thof on 1/25/16.
 */
class LazyCheck[T](function: => T) {
  private var _isSet = false
  def isSet = _isSet
  lazy val value = {
    val cache = function
    _isSet = true
    cache
  }
  def apply() = value
  def get = value
}
