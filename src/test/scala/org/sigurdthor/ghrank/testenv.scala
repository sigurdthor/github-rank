package org.sigurdthor.ghrank

import org.sigurdthor.ghrank.GithubClient.Service
import org.sigurdthor.ghrank.GithubRank.{AppEnv, AppTask}
import org.sigurdthor.ghrank.middleware.ContributorMiddleware
import org.sigurdthor.ghrank.model.{Contributor, GithubError, Owner, Repository}
import zio.clock.Clock
import zio.{IO, ZIO}
import zio.console.Console

object testenv {

  object TestEnvironment {

    def withEnv[A](task: AppTask[A]) =
      ZIO.environment[AppEnv].provide(new Test with Console.Live with Clock.Live with ContributorMiddleware.Live) >>> task
  }

  trait Test extends GithubClient {

    val githubClient: GithubClient.Service[AppEnv] = new Service[AppEnv] {
      override def organisationRepos(org: String): IO[GithubError, Seq[Repository]] = IO.succeed(Seq(Repository("test-1", Owner("bill")), Repository("test-2", Owner("Peter"))))

      override def repoContributors(owner: Owner, repoName: String): IO[GithubError, Seq[Contributor]] = IO.succeed(Seq(Contributor("john", 3), Contributor("jack", 8), Contributor("john", 2)))
    }
  }

}
