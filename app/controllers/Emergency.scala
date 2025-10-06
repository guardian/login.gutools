package controllers

import com.github.nscala_time.time.Imports._
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, PublicSettings}
import play.api.mvc._
import services.NewCookieIssue
import software.amazon.awssdk.services.ses.SesClient
import utils._

import java.time.Instant.now
import java.time.Duration
import scala.util.Random
import scala.util.control.NonFatal

class Emergency(
   loginPublicSettings: PublicSettings,
   deps: LoginControllerComponents,
   sesClient: SesClient,
   panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) with Loggable {

  private val cookieLifetime = Duration.ofDays(1) // 1.day

  def reissueDisabled: Action[AnyContent] = Action {
    Ok(views.html.emergency.reissueDisabled())
  }

  def reissue: Action[AnyContent] = EmergencySwitchIsOnAction { req =>

    val reissueTopic = "Your login session has not been extended"

    (for {
      assymCookie <- req.cookies.find(_.name == panDomainSettings.settings.cookieSettings.cookieName)
    } yield CookieUtils.parseCookieData(assymCookie.value, loginPublicSettings.verification).fold(
      tokenIntegrityFailure =>
        unauthorised(s"Invalid existing session, could not log you in: $tokenIntegrityFailure", reissueTopic)
      , authenticatedUser =>
        if (validateUser(authenticatedUser)) {
          val authCookie = generateCookie(authenticatedUser.copy(expires = (now().plus(cookieLifetime))))
          Ok(views.html.emergency.reissueSuccess()).withCookies(authCookie)
        } else unauthorised("Only Guardian email addresses with two-factor auth are supported.", reissueTopic)
    )).getOrElse {
      unauthorised("No existing login session found, unable to log you in.", reissueTopic)
    }
  }

  def requestCookieLink: Action[AnyContent] = EmergencySwitchIsOnAction {
    Ok(views.html.emergency.requestNewCookie())
  }

  def sendCookieLink: Action[AnyContent] = EmergencySwitchIsOnAction { req =>

    val tokenIssuedAt = DateTime.now().getMillis

    try {
      val emailPrefix = req.body.asFormUrlEncoded.get("email").head

      val emailAddress = s"$emailPrefix@guardian.co.uk"

      val token = Random.alphanumeric.take(20).mkString

      val cookieIssue = NewCookieIssue(token, emailAddress,
        tokenIssuedAt, false)

      try {
        deps.tokenDBService.createCookieIssue(cookieIssue)

        val ses = new SES(sesClient, config)
        ses.sendCookieEmail(token, emailAddress)

        Ok(views.html.emergency.emailSent())
      }
      catch {
        case NonFatal(e) => InternalServerError(e.toString)
      }
    }
    catch {
      case NonFatal(_) => BadRequest("both first and last names must be submitted")
    }
  }

  def issueNewCookie(userToken: String): Action[AnyContent] = EmergencySwitchIsOnAction {

    def issueNewCookie(newCookieIssue: NewCookieIssue): Result = {

      deps.tokenDBService.expireCookieIssue(newCookieIssue)

      val expires = now().plus(cookieLifetime)
      val names = newCookieIssue.email.split("\\.")
      val firstName = names(0).capitalize
      val lastName = names(1).split("@")(0).capitalize
      val user = User(firstName, lastName, newCookieIssue.email, None)
      val newAuthUser = AuthenticatedUser(user, config.appName, Set(config.appName), expires, multiFactor = true)
      val authCookie = generateCookie(newAuthUser)


      Ok(views.html.emergency.reissueSuccess()).withCookies(authCookie)
    }

    val issueNewCookieTopic = "New cookie has not been created"
    val tenMinutesInMilliSeconds = 600000

    val tokenOpt = deps.tokenDBService.getCookieIssueForUserToken(userToken)

    tokenOpt.map {
      case Left(error) => {
        log.warn(s"Error when reading entry with $userToken from dynamo. A new cookie will not be issued: $error")
        unauthorised("Checking your access token failed. You will not be issued with a new ", issueNewCookieTopic)
      }
      case Right(tokenEntry: NewCookieIssue) => {
        if (!tokenEntry.used) {
          val tokenAgeInMilliseconds = DateTime.now().getMillis - tokenEntry.requested
          if (tokenAgeInMilliseconds > tenMinutesInMilliSeconds) {
            log.warn(s"Attempted to use expired token: ${tokenEntry.id}")
            Unauthorized(views.html.emergency.newCookieFailure("Your link has expired. Could not create a new cookie"))
          }
          else {
            issueNewCookie(tokenEntry)
          }
        } else {
          log.warn(s"Attempted to use a used token: ${tokenEntry.id}")
          Unauthorized(views.html.emergency.newCookieFailure("Your link has already been been used"))
        }

      }
    }.getOrElse(Unauthorized("Token not found"))
  }

  private def unauthorised(message: String, topic: String): Result = {
    log.warn(message)
    Unauthorized(views.html.emergency.reissueFailure(message, topic))
  }
}
