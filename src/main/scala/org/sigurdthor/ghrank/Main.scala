package org.sigurdthor.ghrank

import cats.effect.Resource
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze._
import org.sigurdthor.ghrank.middleware.ContributorMiddleware
import zio._
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits


object GithubRank extends zio.App {

  type AppEnv = Clock with Console with GithubClient with ContributorMiddleware
  type AppTask[A] = RIO[AppEnv, A]

  def run(args: List[String]) = {
    val server = for {
      client <- buildHttpClient
      _ <- buildHttpServer(client)
    } yield ()
    server.foldM(err => putStrLn(s"execution failed with $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
  }

  import Routes._

  private def buildHttpClient: RIO[ZEnv, Resource[Task, Client[Task]]] =
    ZIO
      .runtime[ZEnv]
      .map { implicit rts =>
        BlazeClientBuilder
          .apply[Task](Implicits.global)
          .resource
      }

  def buildHttpServer(http4sClient: Resource[Task, Client[Task]]) =
    http4sClient.use { httpClient =>
      ZIO.runtime[AppEnv]
        .flatMap { implicit rts =>
          BlazeServerBuilder[AppTask]
            .bindHttp(8080, "localhost")
            .withHttpApp(githubService)
            .serve
            .compile
            .drain
        }.provide(new GithubClient.Live with Console.Live with Clock.Live with ContributorMiddleware.Live {
        override def client: Client[Task] = httpClient
      })
    }
}