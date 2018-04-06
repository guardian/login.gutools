import config.{AWS, LoginConfig, LoginPublicSettings, Switches}
import controllers._
import play.api.ApplicationLoader.Context
import router.Routes

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context) {
  val stage = AWS.readTag("Stage")
  override val config = LoginConfig.forStage(stage)
  override val switches = new Switches(config)

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
  val switchesController = new SwitchesController(this)

  override lazy val router = new Routes(httpErrorHandler, app, login, emergency, switchesController, assets)
}