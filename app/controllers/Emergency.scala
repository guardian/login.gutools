package controllers

import java.util.Date

import actions.EmergencySwitchIsOnAction
import com.gu.pandomainauth.PublicSettings
import com.gu.pandomainauth.model.{CookieParseException, CookieSignatureInvalidException}
import com.gu.pandomainauth.service.CookieUtils
import config.LoginPublicSettings
import play.api.Logger
import play.api.mvc.{Action, Controller}


object Emergency extends Controller with PanDomainAuthActions {

  //TODO - import library?
  val cookieLifetime: Long = 1000 * 60 * 60 * 24 // 1 day

  def reissueDisabled = Action {
    Ok(views.html.emergency.reissueDisabled())
  }

  def reissue = EmergencySwitchIsOnAction { req =>
    (for {
      publicKey <- LoginPublicSettings.publicKey
      assymCookie <- req.cookies.find(_.name == PublicSettings.assymCookieName)
    } yield {
      try {
        val authenticatedUser = CookieUtils.parseCookieData(assymCookie.value, publicKey)
        if (validateUser(authenticatedUser)) {
          val expires = new Date().getTime + cookieLifetime
          val newAuthUser = authenticatedUser.copy(expires = expires)
          val authCookies = generateCookies(newAuthUser)
          Ok(views.html.emergency.reissueSuccess())
            .withCookies(authCookies: _*)
        } else {
          unauthorised("Only Guardian email addresses with two-factor auth are supported.")
        }
      }
      catch {
        case e: CookieSignatureInvalidException =>
          unauthorised("Invalid existing session, could not log you in.")
        case e: CookieParseException =>
          unauthorised("Could not refresh existing session due to a corrupted cookie.")
      }
    }).getOrElse {
      unauthorised("No existing login session found, unable to log you in.")
    }
  }

  private def unauthorised(message: String) = {
    Logger.warn(message)
    Unauthorized(views.html.emergency.reissueFailure(message))
  }
}
