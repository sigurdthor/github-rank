package org.sigurdthor.ghrank

import org.http4s.server.blaze._
import org.sigurdthor.ghrank.GithubClient.ZioSttpBackend
import sttp.client.asynchttpclient.ziostreams.AsyncHttpClientZioStreamsBackend
import zio._
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.interop.catz._


object GithubRank extends zio.App {

  type AppEnv = Clock with Console with GithubClient
  type AppTask[A] = RIO[AppEnv, A]

  def run(args: List[String]) = {
    val sttpBackend:  RIO[AppEnv, ZioSttpBackend] = ZIO.runtime.flatMap((r: Runtime[AppEnv]) => AsyncHttpClientZioStreamsBackend[AppEnv](r))
    server.provide(new GithubClient.Live with Console.Live with Clock.Live {
      override implicit val backend:  RIO[AppEnv, ZioSttpBackend] = sttpBackend
    }).foldM(err => putStrLn(s"execution failed with $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
  }


  import Routes._

  val server = ZIO.runtime[AppEnv]
    .flatMap {
      implicit rts =>
        BlazeServerBuilder[AppTask]
          .bindHttp(8080, "localhost")
          .withHttpApp(githubService)
          .serve
          .compile
          .drain
    }

}