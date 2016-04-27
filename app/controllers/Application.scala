package controllers

import config.LoginConfig
import controllers.Login._
import play.api.Play.current
import play.api.mvc._


object Application extends Controller {

  lazy val pandaDomainOption = play.api.Play.configuration.getString("pandomain.domain")

  def login(returnUrl: String) = AuthAction { implicit request =>
    if (LoginConfig.isValidUrl(pandaDomainOption, returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action { implicit request =>
    Ok("Ok")
  }
}
