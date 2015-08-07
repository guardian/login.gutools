package controllers

import controllers.Login._
import play.api._
import play.api.mvc._

object Application extends Controller {

  def isGuToolsDomain(returnUrl: String): Boolean = {
    returnUrl.contains("gutools.co.uk") // Not a strict check at all..
  }

  def login(returnUrl: String) = AuthAction { request =>
    if (isGuToolsDomain(returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Not a gutools.co.uk domain")
    }

  }

}
