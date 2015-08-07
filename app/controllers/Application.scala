package controllers

import com.netaporter.uri.Uri
import controllers.Login._
import play.api._
import play.api.mvc._
import com.netaporter.uri.Uri.parse
import com.netaporter.uri.dsl._
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
