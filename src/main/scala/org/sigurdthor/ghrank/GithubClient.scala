package org.sigurdthor.ghrank

import io.circe.Decoder
import io.circe.generic.auto._
import izumi.logstage.api.IzLogger
import izumi.logstage.api.Log.Level.Trace
import izumi.logstage.sink.ConsoleSink
import logstage.{LogBIO, LogstageZIO}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s._
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

    def maybeToken: Option[String]

    lazy val textSink = ConsoleSink.text(colored = true)
    lazy val izLogger: IzLogger = IzLogger(Trace, List(textSink))
    lazy val log: LogBIO[IO] = LogstageZIO.withFiberId(izLogger)

    implicit def entityDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] = jsonOf[Task, A]

    val githubClient: GithubClient.Service[AppEnv] = new Service[AppEnv] {
      override def organisationRepos(org: String): IO[GithubError, Seq[Repository]] =
        performRequest[Repository](s"https://api.github.com/orgs/$org/repos?per_page=100")

      override def repoContributors(owner: Owner, repoName: String): IO[GithubError, Seq[Contributor]] =
        performRequest[Contributor](s"https://api.github.com/repos/${owner.login}/$repoName/contributors")

      def performRequest[T](uri: String)(implicit d: Decoder[T]): IO[GithubError, Seq[T]] = {
        def call(uri: Uri): IO[GithubError, Seq[T]] = {

          val headers = maybeToken match {
            case Some(token) => Headers.of(Header("Authorization", s"token $token"))
            case None => Headers.empty
          }

          client
            .expect[Seq[T]](Request[Task](Method.GET, uri, headers = headers))
            .foldM(_ => /*log.error(s"Request error: ${ex.getMessage}") *>*/ ZIO.succeed(Seq.empty[T]), ZIO.succeed)
        }

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
