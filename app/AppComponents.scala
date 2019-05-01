import config.{LoginConfig, LoginPublicSettings}
import controllers._
import play.api.ApplicationLoader.Context
import router.Routes
import services.switches.{SwitchAccess, SwitchStatus}

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context) {
  override val config = LoginConfig.forStage(instanceTags.map(_.stage))
  override val switches = new SwitchStatus(config)

  val switchAcess = new SwitchAccess(config)
  val loginPublicSettings = new LoginPublicSettings(config)

  loginPublicSettings.start
  switches.start()

  applicationLifecycle.addStopHook(() => {
    loginPublicSettings.stop
    switches.stop()

    Future.successful(())
  })

  val app = new Application(this)
  val emergency = new Emergency(loginPublicSettings, this)
  val login = new Login(this)
  val switchesController = new SwitchesController(switchAcess, this)

  override lazy val router = new Routes(httpErrorHandler, app, login, emergency, switchesController, assets)
}
