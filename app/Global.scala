import config.LoginPublicSettings
import play.Logger
import play.api.{GlobalSettings, Application}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    LoginPublicSettings.start
    config.Switches.start()
  }

  override def onStop(app: Application) {
    LoginPublicSettings.stop
    config.Switches.stop()
    Logger.info("Application has shutdown...")
  }
}