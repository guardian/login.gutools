package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import com.gu.pandomainauth.service.{CookieUtils, OAuthException}
import play.api.Logging
import play.api.mvc.{Action, AnyContent}

import java.net.URLEncoder
import scala.concurrent.ExecutionContext

class DesktopLogin(
  deps: LoginControllerComponents,
  panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) with Logging {

  override lazy val authCallbackUrl: String = deps.config.host + "/desktopOauthCallback"

  implicit private val ec: ExecutionContext = deps.executionContext

  def desktopLogin: Action[AnyContent] = Action.async { implicit request =>
    val antiForgeryToken = OAuth.generateAntiForgeryToken()
    OAuth.redirectToOAuthProvider(antiForgeryToken, None)(ec, request, wsClient) map { _.withSession {
      request.session + (ANTI_FORGERY_KEY -> antiForgeryToken)
    }}
  }

  def desktopOauthCallback(): Action[AnyContent] = Action.async { implicit request =>
    val token =
      request.session.get(ANTI_FORGERY_KEY).getOrElse(throw new OAuthException("missing anti forgery token"))

    OAuth.validatedUserIdentity(token)(request, deps.executionContext, wsClient).map { claimedAuth =>
      logger.debug("fresh user login")
      val authedUserData = claimedAuth.copy(authenticatingSystem = "login-desktop", multiFactor = checkMultifactor(claimedAuth))


      if (validateUser(authedUserData)) {
        // TODO updates to get value separately from cookie
        val updatedCookie = generateCookie(authedUserData)
        Redirect(s"gnm://auth/token/${URLEncoder.encode(updatedCookie.value, "UTF-8")}")
          .withSession(session = request.session - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY)
      } else {
        showUnauthedMessage(invalidUserMessage(claimedAuth))
      }
    }
  }
}
