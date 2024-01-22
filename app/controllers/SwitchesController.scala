package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import config.{Off, On, SwitchState}

class SwitchesController(
  deps: LoginControllerComponents, panDomainSettings: PanDomainAuthSettingsRefresher
) extends LoginController(deps, panDomainSettings) {

  private def errorMessage(state: SwitchState)  = s"Failed to update Emergency switch to ${state.name}. Contact digitalcms.dev@theguardian.com for more help."
  private def success(state: SwitchState)  = s"Emergency switch updated to ${state.name}"

  def index = AuthAction { req =>
    Ok(views.html.switches.switchValues(switches.allSwitches))
  }

  def emergencyOn = EmergencySwitchChangeAccess { req =>
    switches.setEmergencySwitch(On).map { _ =>
      Ok(views.html.switches.switchChange(success(On)))
    }.getOrElse(InternalServerError(views.html.switches.switchChange(errorMessage(On))))
  }

  def emergencyOff = EmergencySwitchChangeAccess { req =>
    switches.setEmergencySwitch(Off).map { _ =>
      Ok(views.html.switches.switchChange(success(Off)))
    }.getOrElse(InternalServerError(views.html.switches.switchChange(errorMessage(Off))))
  }
}
