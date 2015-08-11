package controllers

import java.net.URL

import controllers.Login._
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {

  lazy val pandaDomainOption = play.api.Play.configuration.getString("pandomain.domain")

  /**
   * returnUrl is a valid domain and host of returnUrl ends with a whitelisted domain
   * @param returnUrl the url to return to
   * @return
   */
  def isValidUrl(returnUrl: String): Boolean = {

    pandaDomainOption match {
      case Some(pandaDomain) => {

        try {
          val url = new URL(returnUrl)
          url.getHost.endsWith(pandaDomain) && url.getProtocol == "https" // valid url, matches panda domain and is secure
        } catch {
          case e: Exception => false // invalid url
        }
      }
      case None => false
    }
  }

  def login(returnUrl: String) = AuthAction { request =>
    if (isValidUrl(returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }

  }

  def healthcheck() = {
    Ok("Ok")
  }

}
