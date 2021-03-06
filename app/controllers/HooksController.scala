package controllers

import domain.Candidate.{Java, Spark}
import domain.JdkDistro.{BellSoft, OpenJDK, Oracle, Zulu, ZuluFX}
import domain.Platform._
import domain.{Candidate, Platform}
import play.api.Logger
import play.api.mvc.{Action, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HooksController extends Controller {

  val PostHook = "post"
  val PreHook = "pre"

  def hook(phase: String, candidateId: String, version: String, platformId: String): Action[AnyContent] =
    Action.async { _ =>
      Future {
        implicit val candidate = Candidate(candidateId)

        val platform = Platform(platformId).getOrElse(Universal)

        Logger.info(s"$phase install hook requested for: $candidateId $version ${platform.name}")

        (phase, candidate, normalise(version), platform, vendor(version)) match {

          //POST: Mac OSX
          case (PostHook, Java, "8", MacOSX, Oracle) =>
            Ok(views.txt.java_post_8_oracle_osx(candidate, dropSuffix(version), MacOSX))
          case (PostHook, Java, _, MacOSX, BellSoft) =>
            Ok(views.txt.default_post_zip(candidate, version, MacOSX))
          case (PostHook, Java, _, MacOSX, Zulu) =>
            Ok(views.txt.default_post_tarball(candidate, version, MacOSX))
          case (PostHook, Java, _, MacOSX, ZuluFX) =>
            Ok(views.txt.default_post_tarball(candidate, version, MacOSX))
          case (PostHook, Java, _, MacOSX, _) =>
            Ok(views.txt.java_post_openjdk_osx(candidate, version, MacOSX))

          //POST: Linux
          case (PostHook, Java, _, Linux, _) =>
            Ok(views.txt.java_post_linux_tarball(candidate, version, Linux))

          //POST: Cygwin
          case (PostHook, Java, _, Windows64Cygwin, Oracle) =>
            Ok(views.txt.java_post_cygwin_msi(candidate, version, Windows64Cygwin))
          case (PostHook, Java, "9", Windows64Cygwin, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64Cygwin))
          case (PostHook, Java, "10", Windows64Cygwin, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64Cygwin))
          case (PostHook, Java, _, Windows64Cygwin, _) =>
            Ok(views.txt.default_post_zip(candidate, version, Windows64Cygwin))

          //POST: Mysys
          case (PostHook, Java, "9", Windows64MinGW, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64MinGW))
          case (PostHook, Java, "10", Windows64MinGW, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64MinGW))
          case (PostHook, Java, _, Windows64MinGW, _) =>
            Ok(views.txt.default_post_zip(candidate, version, Windows64MinGW))

          //POST
          case (PostHook, Java, _, _, _) =>
            NotFound
          case (PostHook, Spark, _, _, _) =>
            Ok(views.txt.default_post_tarball(candidate, version, platform))
          case (PostHook, _, _, _, _) =>
            Ok(views.txt.default_post_zip(candidate, version, platform))

          //PRE
          case (PreHook, Java, _, Windows64MinGW, Oracle) =>
            Ok(views.txt.java_pre_mingw_msi(candidate, version, Windows64MinGW))
          case (PreHook, Java, _, _, Oracle) =>
            Ok(views.txt.java_pre_obcla(candidate, version))
          case (PreHook, _, _, _, _) =>
            Ok(views.txt.default_pre(candidate, version, platform))
        }
      }
    }

  private def normalise(version: String)(implicit c: Candidate): String =
    if (c == Java) version.split('.').head else version

  private def dropSuffix(v: String) = v.split("-").head

  private def vendor(version: String) = version.split("-").lastOption.getOrElse("")
}
