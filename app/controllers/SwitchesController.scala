package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import config.{Off, On, SwitchState}

class SwitchesController(
  deps: LoginControllerComponents, panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) {

  def index = AuthAction { req =>
    Ok(views.html.switches.switchValues(switches.allSwitches))
  }
}
