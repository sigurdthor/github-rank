package org.sigurdthor.ghrank

import org.sigurdthor.ghrank.GithubClient.factory.{organizationRepos, repoContributors}
import org.sigurdthor.ghrank.GithubRank.AppEnv
import org.sigurdthor.ghrank.model.Contributor
import zio.ZIO

object middleware {

  trait ContributorMiddleware {
    val contributorService: ContributorMiddleware.Service[AppEnv]
  }

  object ContributorMiddleware {

    trait Service[R] {
      def retrieveContributors(repoName: String): ZIO[AppEnv, Throwable, Seq[Contributor]]
    }

    trait Live extends ContributorMiddleware {

      val contributorService: middleware.ContributorMiddleware.Service[AppEnv] = (repoName: String) => {
        for {
          repositories <- organizationRepos(repoName)
          contributors <- ZIO.foreachPar(repositories)(r => repoContributors(r.owner, r.name))
        } yield contributors
          .flatten
          .groupBy(_.login)
          .view
          .mapValues(_.map(_.contributions).sum)
          .map(t => Contributor(t._1, t._2))
          .toSeq
          .sortWith(_.contributions > _.contributions)
      }
    }

    object Live extends Live
  }

  object factory {
    def retrieveContributors(repoName: String): ZIO[AppEnv, Throwable, Seq[Contributor]] =
      ZIO.access[ContributorMiddleware](_.contributorService.retrieveContributors(repoName)).flatten
  }
}
