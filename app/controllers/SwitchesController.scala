package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher

class SwitchesController(
  deps: LoginControllerComponents, panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) {

  def index = AuthAction.async {
    for {
      allSwitches <- switches.allSwitches
    } yield Ok(views.html.switches.switchValues(allSwitches))
  }
}
