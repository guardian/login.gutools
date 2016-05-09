package controllers

import config.{Off, On}
import controllers.Login._

object Switches {

  def index = AuthAction { req =>
    Ok(views.html.switches(config.Switches.allSwitches))
  }

  //TODO whitelist who can do this & failure cases
  def emergencyOn = AuthAction { req =>
    try {
      config.Switches.setEmergencySwitch(On)
      Ok("Emergency switch updated to ON.")
    } catch {
      case e: Exception => InternalServerError(s"Failed to update emergency switch to ON: ${e.getMessage}")
    }
  }

  //TODO whitelist who can do this & failure cases
  def emergencyOff = AuthAction { req =>
    try {
      config.Switches.setEmergencySwitch(Off)
      Ok("Emergency switch updated to OFF.")
    } catch {
      case e: Exception => InternalServerError(s"Failed to update emergency switch to OFF: ${e.getMessage}")
    }
  }
}
