package org.sigurdthor.ghrank

object model {

  case class Repository(name: String)
  case class Contributor(name: String, contributions: Int)

}
