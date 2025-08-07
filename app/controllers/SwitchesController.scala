package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher

import scala.concurrent.ExecutionContext

class SwitchesController(
  deps: LoginControllerComponents, panDomainSettings: PanDomainAuthSettingsRefresher
)(implicit ec: ExecutionContext) extends LoginController(deps, panDomainSettings) {

  def index = AuthAction.async {
    for {
      allSwitches <- switches.allSwitches
    } yield Ok(views.html.switches.switchValues(allSwitches))
  }
}
