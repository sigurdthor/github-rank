package org.sigurdthor.ghrank

import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}
import org.sigurdthor.ghrank.GithubRank.AppEnv
import org.sigurdthor.ghrank.model.GithubError.MalformedUrl
import org.sigurdthor.ghrank.model.{Contributor, GithubError, Owner, Repository}
import zio.interop.catz._
import zio.{IO, Task, ZIO}


trait GithubClient {
  val githubClient: GithubClient.Service[AppEnv]
}

object GithubClient {

  trait Service[R] {
    def organisationRepos(org: String): IO[GithubError, Seq[Repository]]

    def repoContributors(owner: Owner, repoName: String): IO[GithubError, Seq[Contributor]]
  }

  trait Live extends GithubClient {

    def client: Client[Task]

    implicit def entityDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] = jsonOf[Task, A]

    val githubClient: GithubClient.Service[AppEnv] = new Service[AppEnv] {
      override def organisationRepos(org: String): IO[GithubError, Seq[Repository]] =
        performRequest[Repository](s"https://api.github.com/users/$org/repos")

      override def repoContributors(owner: Owner, repoName: String): IO[GithubError, Seq[Contributor]] =
        performRequest[Contributor](s"https://api.github.com/repos/${owner.login}/$repoName/contributors")

      def performRequest[T](uri: String)(implicit d: Decoder[T]): IO[GithubError, Seq[T]] = {
        def call(uri: Uri): IO[GithubError, Seq[T]] = client
          .expect[Seq[T]](uri)
          .foldM(_ => ZIO.succeed(Seq.empty[T]), ZIO.succeed)

        println(s"Perform request $uri")
        Uri
          .fromString(uri)
          .fold(_ => IO.fail(MalformedUrl(uri)), call)
      }
    }
  }

  object factory {
    def organizationRepos(org: String): ZIO[AppEnv, Throwable, Seq[Repository]] =
      ZIO.access[GithubClient](_.githubClient.organisationRepos(org)).flatten

    def repoContributors(owner: Owner, repo: String): ZIO[AppEnv, Throwable, Seq[Contributor]] =
      ZIO.access[GithubClient](_.githubClient.repoContributors(owner, repo)).flatten
  }

}
