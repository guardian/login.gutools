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
    try {
      config.Switches.setEmergencySwitch(On)
      Ok(views.html.switches.switchChange(success(On)))
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage)
        InternalServerError(views.html.switches.switchChange(errorMessage(On)))
    }
  }

  def emergencyOff = EmergencySwitchChangeAccess { req =>
    try {
      config.Switches.setEmergencySwitch(Off)
      Ok(views.html.switches.switchChange(success(On)))
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage)
        InternalServerError(views.html.switches.switchChange(errorMessage(Off)))
    }
  }
}
