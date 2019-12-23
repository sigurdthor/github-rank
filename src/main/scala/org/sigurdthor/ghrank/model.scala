package org.sigurdthor.ghrank

object model {

  case class Repository(name: String, owner: Owner)
  case class Contributor(name: String, contributions: Int)
  case class Owner(login: String)


  trait GithubError extends Throwable {
    def message: String
  }

  object GithubError {
    case class MalformedUrl(url: String) extends GithubError {
      def message: String = s"Couldn't build url for repository: $url"
    }
  }
}
