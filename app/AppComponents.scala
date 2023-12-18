import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, PublicSettings}
import config.{AWS, LoginConfig, Switches}
import controllers._
import play.api.ApplicationLoader.Context
import router.Routes

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context, new AWS()) {
  override def config = LoginConfig.forStage(asgTags.map(_.stage))

  override val switches = new Switches(config, aws.s3Client)

  private lazy val panDomainSettings: PanDomainAuthSettingsRefresher =
    new PanDomainAuthSettingsRefresher(
      domain = config.domain,
      system = "login",
      bucketName = config.pandaAuthBucket,
      settingsFileKey = s"${config.domain}.settings",
      s3Client = aws.s3Client
    )

  private lazy val desktopPanDomainSettings: PanDomainAuthSettingsRefresher = {
    val domain = config.desktopDomain
    new PanDomainAuthSettingsRefresher(
      domain = domain,
      system = "login-desktop",
      bucketName = config.pandaAuthBucket,
      settingsFileKey = s"$domain.settings",
      s3Client = aws.s3Client
    )
  }

  val loginPublicSettings = new PublicSettings(
    settingsFileKey = s"${config.domain}.settings.public",
    bucketName = config.pandaAuthBucket,
    s3Client = aws.s3Client
  )

  loginPublicSettings.start()

  switches.start()

  applicationLifecycle.addStopHook(() => {
    switches.stop()

    Future.successful(())
  })

  private val app = new Application(this, panDomainSettings)
  private val emergency = new Emergency(loginPublicSettings, this, aws.sesClient, panDomainSettings)
  private val login = new Login(this, panDomainSettings)
  private val desktopLogin = new DesktopLogin(this, desktopPanDomainSettings)
  private val switchesController = new SwitchesController(this, panDomainSettings)

  override lazy val router = new Routes(httpErrorHandler, app, desktopLogin, login, emergency, switchesController, assets)
}
