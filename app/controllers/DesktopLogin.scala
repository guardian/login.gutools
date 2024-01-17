package controllers

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.{CookieUtils, OAuthException}
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import play.api.Logging
import play.api.mvc.{Action, AnyContent}

import java.net.URLEncoder
import scala.concurrent.ExecutionContext

class DesktopLogin(
  deps: LoginControllerComponents,
  panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) with Logging {

  override lazy val authCallbackUrl: String = deps.config.host + "/desktop/oauthCallback"

  implicit private val ec: ExecutionContext = deps.executionContext

  def desktopLogin: Action[AnyContent] = Action.async { implicit request =>
    val antiForgeryToken = OAuth.generateAntiForgeryToken()
    OAuth.redirectToOAuthProvider(antiForgeryToken, None)(ec, request, wsClient) map { _.withSession {
      request.session + (ANTI_FORGERY_KEY -> antiForgeryToken)
    }}
  }

  def authStatus: Action[AnyContent] = Action { request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(s"GU-Desktop-Panda $token") =>
        PanDomain.authStatus(token,
          publicKey = panDomainSettings.settings.publicKey,
          validateUser = PanDomain.guardianValidation,
          apiGracePeriod = 0L,
          system = panDomainSettings.system,
          cacheValidation = false
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

  def desktopOauthCallback(): Action[AnyContent] = Action.async { implicit request =>
    val token =
      request.session.get(ANTI_FORGERY_KEY).getOrElse(throw new OAuthException("missing anti forgery token"))

    OAuth.validatedUserIdentity(token)(request, deps.executionContext, wsClient).map { claimedAuth =>
      logger.debug("fresh user desktop login")
      val authedUserData = claimedAuth.copy(authenticatingSystem = "login-desktop", multiFactor = checkMultifactor(claimedAuth))


      if (validateUser(authedUserData)) {
        val token = CookieUtils.generateCookieData(authedUserData, panDomainSettings.settings.privateKey)
        Redirect(s"gnm://auth/token/${URLEncoder.encode(token, "UTF-8")}/stage/${deps.config.stage.toLowerCase}")
          .withSession(session = request.session - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY)
      } else {
        showUnauthedMessage(invalidUserMessage(claimedAuth))
      }
    }
  }
}
