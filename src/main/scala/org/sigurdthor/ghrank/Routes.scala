package org.sigurdthor.ghrank

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.sigurdthor.ghrank.GithubRank.{AppEnv, AppTask}
import zio.interop.catz._
import org.http4s.implicits._
import org.sigurdthor.ghrank.middleware.ContributorMiddleware
import zio.{RIO, Task, ZIO}
import org.sigurdthor.ghrank.middleware.factory._

object Routes {

  object ioz extends Http4sDsl[AppTask]

  import ioz._

  implicit def entityEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  val contributorService = ZIO.access[ContributorMiddleware](_.contributorService)

  val githubService = HttpRoutes.of[AppTask] {
    case GET -> Root / "org" / orgName / "contributors" => buildResponse(retrieveContributors(orgName))
  }.orNotFound

  private def buildResponse[T](effect: ZIO[AppEnv, Throwable, Seq[T]])(implicit e: Encoder[T]): RIO[AppEnv, Response[AppTask]] = {
    effect.foldM(
      {
        error: Throwable => InternalServerError(s"Failure: ${error.getMessage}")
      },
      entities => Ok(entities.asJson)
    )
  }

}
