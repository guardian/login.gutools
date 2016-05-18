package controllers

import actions.EmergencySwitchChangeAccess
import config.{Off, On, SwitchState}
import controllers.Login._
import play.Logger

object Switches {

  private def errorMessage(state: SwitchState)  = s"Failed to update Emergency switch to ${state.name}. Contact digitalcms.dev@theguardian.com for more help."
  private def success(state: SwitchState)  = s"Emergency switch updated to ${state.name}"

  def index = AuthAction { req =>
    Ok(views.html.switches.switchValues(config.Switches.allSwitches))
  }

  def emergencyOn = EmergencySwitchChangeAccess { req =>
    config.Switches.setEmergencySwitch(On).map { _ =>
      Ok(views.html.switches.switchChange(success(On)))
    }.getOrElse(InternalServerError(views.html.switches.switchChange(errorMessage(On))))
  }

  def emergencyOff = EmergencySwitchChangeAccess { req =>
    config.Switches.setEmergencySwitch(Off).map { _ =>
      Ok(views.html.switches.switchChange(success(Off)))
    }.getOrElse(InternalServerError(views.html.switches.switchChange(errorMessage(Off))))
  }
}
