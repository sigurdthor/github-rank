package org.sigurdthor.ghrank

import java.nio.ByteBuffer

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.sigurdthor.ghrank.GithubRank.AppEnv
import org.sigurdthor.ghrank.model.{Contributor, Repository}
import sttp.client.{SttpBackend, _}
import sttp.model.Uri
import zio.interop.catz._
import zio.stream.{Stream, ZStream}
import zio.{RIO, Task, ZIO}

import scala.concurrent.duration.Duration


trait GithubClient {
  val githubClient: GithubClient.Service[AppEnv]
}

object GithubClient {

  type ZioSttpBackend = SttpBackend[Task, Stream[Throwable, ByteBuffer], Nothing]

  trait Service[R] {
    def organisationRepos(org: String): ZIO[AppEnv, Throwable, ZStream[R, Throwable, Repository]]

    def repoContributors(repoName: String): ZIO[AppEnv, Throwable, ZStream[R, Throwable, Contributor]]
  }

  trait Live extends GithubClient {

    implicit val backend: RIO[AppEnv, ZioSttpBackend]

    implicit def entityDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] = jsonOf[Task, A]

    val githubClient: GithubClient.Service[AppEnv] = new Service[AppEnv] {
      override def organisationRepos(org: String): ZIO[AppEnv, Throwable, ZStream[AppEnv, Throwable, Repository]] =
        performRequest[Repository](uri"https://api.github.com/users/$org/repos")

      override def repoContributors(repoName: String): ZIO[AppEnv, Throwable, ZStream[AppEnv, Throwable, Contributor]] =
        performRequest[Contributor](uri"https://api.github.com/repo/$repoName/contributors")

      private def performRequest[T](uri: Uri)(implicit d: Decoder[T]) = {
        backend.flatMap { implicit b =>
          val responseIO = basicRequest
            .get(uri)
            .response(asStream[Stream[Throwable, ByteBuffer]])
            .readTimeout(Duration.Inf)
            .send()

          responseIO.map { response =>
            response.body match {
              case Right(stream) =>
                stream
                  .flatMap { s =>
                    decode[Seq[T]](new String(s.array(), "UTF-8")) match {
                      case Right(value) => Stream.fromIterable(value)
                      case Left(error) => Stream.fail(new Exception(error))
                    }
                  }
              case Left(error) => Stream.fail(new Exception(error))
            }
          }
        }
      }
    }
  }

  object factory {
    def organizationRepos(org: String): ZIO[AppEnv, Throwable, ZStream[AppEnv, Throwable, Repository]] =
      ZIO.access[GithubClient](_.githubClient.organisationRepos(org))
        .flatten

    def repoContributors(repo: String): ZIO[AppEnv, Throwable, ZStream[AppEnv, Throwable, Contributor]] =
      ZIO.access[GithubClient](_.githubClient.repoContributors(repo))
        .flatten
  }

}
