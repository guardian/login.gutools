package controllers

import config.LoginConfig
import play.api.Configuration
import play.api.mvc._


class Login(deps: LoginControllerComponents) extends LoginController(deps) {
  private val defaultAllowHeaders = List("X-Requested-With","Origin","Accept","Content-Type")

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
    cors(Ok(user.toJson).as("application/json"), Some("GET"))
  }

  private def cors(result: Result, allowedMethods: Option[String] = None)(implicit request: RequestHeader): Result = {

    val responseHeaders = (defaultAllowHeaders ++ request.headers.get("Access-Control-Request-Headers").toList) mkString ","

    request.headers.get("Origin") match {
      case None => result
      case Some(requestOrigin) if LoginConfig.isValidUrl(config.domain, requestOrigin)  => {
        val headers = allowedMethods.map("Access-Control-Allow-Methods" -> _).toList ++ List(
          "Access-Control-Allow-Origin" -> requestOrigin,
          "Access-Control-Allow-Headers" -> responseHeaders,
          "Access-Control-Allow-Credentials" -> "true")
        result.withHeaders(headers: _*)
      }
      case Some(requestOrigin) => Unauthorized
    }
  }
}
