package controllers

import controllers.Login._
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {

  lazy val whitelist = play.api.Play.configuration.getStringList("redirect.whitelist")

  def isGuToolsDomain(returnUrl: String): Boolean = {
    whitelist.get.contains(returnUrl)
  }

  def login(returnUrl: String) = AuthAction { request =>
    if (isGuToolsDomain(returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Not a whitelisted gutools.co.uk domain")
    }

  }

}
