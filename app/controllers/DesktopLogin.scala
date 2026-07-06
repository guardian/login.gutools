package controllers

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.{CookieUtils, OAuthException}
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, RequestHeader}
import utils.DesktopTokenUtils

import java.time.Duration
import scala.concurrent.Future

class DesktopLogin(
  deps: LoginControllerComponents,
  panDomainSettings: PanDomainAuthSettingsRefresher,
  telemetryUrl: String
) extends LoginController(deps, panDomainSettings) with Logging {

  override lazy val authCallbackUrl: String = deps.config.host + "/desktop/oauthCallback"

  def clientSideRedirectToDesktopLogin = Action {
    val desktopLoginUrl = controllers.routes.DesktopLogin.desktopLogin().url
    Ok(views.html.clientSideRedirectToLogin(desktopLoginUrl, telemetryUrl))
  }

  def desktopLogin: Action[AnyContent] = Action.async { implicit request =>
    val sessionId = OAuth.generateSessionId()
    val antiForgeryToken = OAuth.generateAntiForgeryToken()
    OAuth.redirectToOAuthProvider(sessionId, antiForgeryToken)(ec) map { _.withSession {
      request.session + (antiForgeryTokenKey(sessionId) -> antiForgeryToken)
    }}
  }

  def authStatus: Action[AnyContent] = Action { request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(s"GU-Desktop-Panda $token") =>
        PanDomain.authStatus(token,
          verification = panDomainSettings.settings.signingAndVerification,
          validateUser = PanDomain.guardianValidation,
          apiGracePeriod = Duration.ofHours(9),
          system = panDomainSettings.system,
          cacheValidation = false,
          forceExpiry = false
        ) match {
          case Expired(_) => new Status(419)
          case GracePeriod(authedUser) => Ok(s"hello ${authedUser.user.email} - you're in a grace period")
          case Authenticated(authedUser) => Ok(s"hello ${authedUser.user.email}")
          case NotAuthorized(_) => Forbidden
          case InvalidCookie(_) => BadRequest
          case NotAuthenticated => Unauthorized
        }
      case _ => Unauthorized
    }
  }

  private def fetchSessionIdFromState()(implicit request: RequestHeader) =
    request.getQueryString("state") match {
      case Some(s"$sessionId+$_") => Right(sessionId)
      case Some(_) => Left(BadRequest("State parameter returned missing a session ID"))
      case None => Left(BadRequest("No state parameter passed in callback"))
    }

  def desktopOauthCallback(): Action[AnyContent] = Action.async { implicit request =>
    (for {
      sessionId <- fetchSessionIdFromState()
    } yield {
      val antiForgeryTokenKeyFromSession = antiForgeryTokenKey(sessionId)
      val loginOriginKeyFromSession = loginOriginKey(sessionId)
      val token =
        request.session
          .get(antiForgeryTokenKeyFromSession)
          .getOrElse(throw new OAuthException("missing anti forgery token"))

      OAuth.validatedUserIdentity(sessionId, token)(request, deps.executionContext, wsClient).map { claimedAuth =>
        logger.debug("fresh user desktop login")
        val authedUserData = claimedAuth.copy(
          authenticatingSystem = "login-desktop",
          multiFactor = checkMultifactor(claimedAuth)
        )

        if (validateUser(authedUserData)) {
          val token = CookieUtils.generateCookieData(authedUserData, panDomainSettings.settings.signingAndVerification)
          val stageName = deps.config.stage
          val redirectUrl = DesktopTokenUtils.desktopRedirectUrl(token, stageName)
          Redirect(redirectUrl)
            .withSession(session = request.session - antiForgeryTokenKeyFromSession - loginOriginKeyFromSession)
        } else {
          showUnauthedMessage(invalidUserMessage(claimedAuth))
        }
      }
    }) match {
      case Left(failure) => Future.successful(failure)
      case Right(eventualSuccess) => eventualSuccess
    }
  }
}
