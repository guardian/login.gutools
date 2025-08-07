package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import config.LoginConfig
import login.BuildInfo
import play.api.Logging
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class Application(
  deps: LoginControllerComponents,
  panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) with Logging {

  def login(returnUrl: String) = AuthAction { implicit request =>
    if (LoginConfig.isValidUrl(config.domain, returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action.async {
    for {
      switches <- deps.switches.allSwitches
    } yield if (switches.isEmpty) {
      val warning = "No switch data loaded"
      log.warn(warning)
      ServiceUnavailable(warning)
    } else {
      log.info("Responding from the healthcheck")
      Ok(Json.parse(BuildInfo.toJson))
    }
  }

  def index() = Action { implicit request => Ok("A small application to login a user via pan-domain-auth and redirect them.")}
}
