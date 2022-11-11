package controllers

import login.BuildInfo
import config.LoginConfig

import java.net.URL

class Application(deps: LoginControllerComponents) extends LoginController(deps) {

  private val localDomain = "local.dev-gutools.co.uk"

  def login(returnUrl: String) = AuthAction { implicit request =>
    if(new URL(returnUrl).getHost.endsWith(localDomain)) {
      readCookie(request).filter(_.domain.contains("code.dev-gutools.co.uk")).foreach { _ =>
        flushCookie(
          Redirect(request.uri)
        )
      }
    }
    if (LoginConfig.isValidUrl(config.domain, returnUrl) || LoginConfig.isValidUrl(localDomain, returnUrl)) {
      Redirect(returnUrl)
    }
    else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action { implicit request =>
    Ok(BuildInfo.gitCommitId)
  }

  def index() = Action { implicit request => Ok("A small application to login a user via pan-domain-auth and redirect them.")}
}
