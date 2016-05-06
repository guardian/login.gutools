package controllers

import config.On
import controllers.Login._

object Switches {

  def index = AuthAction { req =>
    Ok(views.html.switches(config.Switches.allSwitches))
  }

  //TODO whitelist who can do this
  def emergencyOn = AuthAction { req =>
    try {
      config.Switches.set("emergency", On)
      Ok("Emergency switch updated to ON.")
    } catch {
      case e: Exception => InternalServerError(s"Update failed with exception ${e.getMessage}")
    }
  }
}
