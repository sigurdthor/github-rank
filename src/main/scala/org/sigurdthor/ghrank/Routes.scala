package org.sigurdthor.ghrank

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.sigurdthor.ghrank.GithubClient.factory._
import org.sigurdthor.ghrank.GithubRank.{AppEnv, AppTask}
import org.sigurdthor.ghrank.model.{Contributor, Repository}
import zio.interop.catz._
import zio.stream.ZSink
import zio.{RIO, Task, ZIO}

object Routes {

  object ioz extends Http4sDsl[AppTask]

  import ioz._

  implicit def entityEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  val githubService = HttpRoutes.of[AppTask] {
    case GET -> Root / "org" / orgName / "contributors" => buildResponse[Contributor](retrieveContributors(orgName))
  }.orNotFound

  private def retrieveRepos(orgName: String): ZIO[AppEnv, Throwable, List[Repository]] = for {
    s <- organizationRepos(orgName)
    result <- s.run(ZSink.collectAll[Repository])
  } yield result

  private def retrieveContributors(repoName: String): ZIO[AppEnv, Throwable, Seq[Contributor]] =
    retrieveRepos(repoName)
      .flatMap(it => ZIO.foreach(it)(c => repoContributors(c.name))
        .flatMap(streams => streams.reduceLeft(_.merge(_)).run(ZSink.collectAll[Contributor])))

  private def buildResponse[T](effect: ZIO[AppEnv, Throwable, Seq[T]])(implicit e: Encoder[T]): RIO[AppEnv, Response[AppTask]] = {
    effect.foldM(
      {
        error: Throwable => InternalServerError(s"Failure: ${error.getMessage}")
      },
      entities => Ok(entities.asJson)
    )
  }

}
