package controllers

import config.On
import controllers.Login._

object Switches {

  def index = AuthAction { req =>
    Ok(views.html.switches(config.Switches.allSwitches))
  }

  //TODO whitelist who can do this
  def emergencyOn = AuthAction { req =>
    config.Switches.set("emergency", On)
    Ok("We're done") //TODO fix - catch errors
  }
}
