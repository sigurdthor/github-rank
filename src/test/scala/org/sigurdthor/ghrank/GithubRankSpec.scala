package org.sigurdthor.ghrank

import io.circe.literal._
import org.http4s.circe._
import org.http4s.{Status, _}
import org.sigurdthor.ghrank.GithubRank.AppTask
import org.sigurdthor.ghrank.utils.RequestHelper._
import org.sigurdthor.ghrank.testenv.TestEnvironment._
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, suite, testM}


object GithubRankSpec extends DefaultRunnableSpec(

  suite("routes suite")(
    testM("contributors request returns list of contributors") {
      withEnv {
        val req = request[AppTask](Method.GET, "/org/sigurdthor/contributors")

        checkRequest(
          Routes.githubService.run(req),
          Status.Ok,
          Some(
            json"""
           [
            {
              "name" : "jack",
              "contributions" : 16
            },
            {
              "name" : "john",
              "contributions" : 10
            }
           ]
                """)
        )
      }
    }
  ))
