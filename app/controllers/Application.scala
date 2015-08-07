package controllers

import controllers.Login._
import play.api._
import play.api.mvc._

object Application extends Controller {

  def login(returnUrl: String) = AuthAction { request =>
    Redirect(returnUrl)
  }

}
