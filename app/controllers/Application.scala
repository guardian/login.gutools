package controllers

import login.BuildInfo
import config.LoginConfig
import play.api.libs.json.Json

class Application(deps: LoginControllerComponents) extends LoginController(deps) {

  def login(returnUrl: String) = AuthAction { implicit request =>
    if (LoginConfig.isValidUrl(config.domain, returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action { implicit request =>
    Ok(Json.parse(BuildInfo.toJson))
  }

  def index() = Action { implicit request => Ok("A small application to login a user via pan-domain-auth and redirect them.")}
}
