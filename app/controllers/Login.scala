package controllers

import model.Cors
import play.api.mvc._


object Login extends Controller with PanDomainAuthActions {

  def oauthCallback = Action.async { implicit request =>
    processGoogleCallback()
  }

  def status = AuthAction { request =>
    val user = request.user
    Ok(views.html.loginStatus(user.toJson))
  }

  def logout = Action { implicit request =>
    processLogout
  }

  def whoami = APIAuthAction { implicit request =>
    val user = request.user
    Cors(Ok(user.toJson).as("application/json"), Some("GET"))
  }
}
