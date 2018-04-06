package controllers

import config.LoginConfig


class Application(deps: LoginControllerComponents) extends LoginController(deps) {
  def login(returnUrl: String) = AuthAction { implicit request =>
    if (LoginConfig.isValidUrl(config.domain, returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action { implicit request =>
    Ok("Ok")
  }
}
